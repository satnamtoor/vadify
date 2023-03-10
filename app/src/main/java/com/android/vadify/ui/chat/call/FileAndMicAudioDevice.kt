package com.android.vadify.ui.chat.call

import android.content.Context
import android.media.AudioManager
import android.media.AudioRecord
import android.media.AudioTrack
import android.media.MediaRecorder
import android.os.Build
import android.os.Handler
import android.os.HandlerThread
import android.os.Process
import android.util.Log
import androidx.annotation.RequiresApi
import com.twilio.video.AudioDevice
import com.twilio.video.AudioDeviceContext
import com.twilio.video.AudioFormat
import tvi.webrtc.ThreadUtils
import java.io.DataInputStream
import java.io.IOException
import java.io.InputStream
import java.nio.ByteBuffer

class FileAndMicAudioDevice(private val context: Context) : AudioDevice {
    private var keepAliveRendererRunnable = true

    // Average number of callbacks per second.
    private val BUFFERS_PER_SECOND = 1000 / CALLBACK_BUFFER_SIZE_MS
    private var fileWriteByteBuffer: ByteBuffer? = null
    private var writeBufferSize = 0
    private var inputStream: InputStream? = null
    private var dataInputStream: DataInputStream? = null
    private var audioRecord: AudioRecord? = null
    private var micWriteBuffer: ByteBuffer? = null
    private var readByteBuffer: ByteBuffer? = null
    private var audioTrack: AudioTrack? = null

    // Handlers and Threads
    private var capturerHandler: Handler? = null
    private var capturerThread: HandlerThread? = null
    private var rendererHandler: Handler? = null
    private var rendererThread: HandlerThread? = null
    private var renderingAudioDeviceContext: AudioDeviceContext? = null
    private var capturingAudioDeviceContext: AudioDeviceContext? = null

    // By default music capturer is enabled
    var isMusicPlaying = false
        private set

    /*
     * This Runnable reads a music file and provides the audio frames to the AudioDevice API via
     * AudioDevice.audioDeviceWriteCaptureData(..) until there is no more data to be read, the
     * capturer input switches to the microphone, or the call ends.
     */

    private val fileCapturerRunnable = object : Runnable {
        override fun run() {
            Process.setThreadPriority(Process.THREAD_PRIORITY_URGENT_AUDIO)
            var bytesRead = 0
            try {
                fileWriteByteBuffer?.let { fileWriteByteBuffer ->
                    dataInputStream?.let { dataInputStream ->
                        if (dataInputStream.read(fileWriteByteBuffer.array(), 0, writeBufferSize)
                                .also { bytesRead = it } > -1
                        ) {
                            if (bytesRead == fileWriteByteBuffer.capacity()) {
                                capturingAudioDeviceContext?.let {
                                    AudioDevice.audioDeviceWriteCaptureData(it, fileWriteByteBuffer)
                                }
                            } else {
                                processRemaining(
                                    fileWriteByteBuffer,
                                    fileWriteByteBuffer.capacity()
                                )
                                capturingAudioDeviceContext?.let {
                                    AudioDevice.audioDeviceWriteCaptureData(it, fileWriteByteBuffer)
                                }
                            }
                        }
                    }

                }
            } catch (e: IOException) {
                e.printStackTrace()
            }
            capturerHandler?.postDelayed(this, CALLBACK_BUFFER_SIZE_MS.toLong())
        }
    }

    /*
     * This Runnable reads data from the microphone and provides the audio frames to the AudioDevice
     * API via AudioDevice.audioDeviceWriteCaptureData(..) until the capturer input switches to
     * microphone or the call ends.
     */
    private val microphoneCapturerRunnable = Runnable {
        Process.setThreadPriority(Process.THREAD_PRIORITY_URGENT_AUDIO)
        if (audioRecord?.state != AudioRecord.STATE_UNINITIALIZED) {
            audioRecord?.startRecording()
            micWriteBuffer?.let { micWriteBuffer ->
                while (true) {
                    val bytesRead = audioRecord?.read(micWriteBuffer, micWriteBuffer.capacity())
                    if (bytesRead == micWriteBuffer.capacity()) {
                        capturingAudioDeviceContext?.let {
                            AudioDevice.audioDeviceWriteCaptureData(it, micWriteBuffer)
                        }
                    } else {
                        val errorMessage = "AudioRecord.read failed: $bytesRead"
                        Log.e(TAG, errorMessage)
                        if (bytesRead == AudioRecord.ERROR_INVALID_OPERATION) {
                            stopRecording()
                            Log.e(TAG, errorMessage)
                        }
                        break
                    }
                }
            }
        }
    }

    /*
     * This Runnable reads audio data from the callee perspective via AudioDevice.audioDeviceReadRenderData(...)
     * and plays out the audio data using AudioTrack.write().
     */
    private val speakerRendererRunnable = label@ Runnable {
        Process.setThreadPriority(Process.THREAD_PRIORITY_URGENT_AUDIO)
        try {
            audioTrack?.play()
        } catch (e: IllegalStateException) {
            Log.e(TAG, "AudioTrack.play failed: " + e.message)
            releaseAudioResources()
        }
        while (keepAliveRendererRunnable) {
            // Get 10ms of PCM data from the SDK. Audio data is written into the ByteBuffer provided.
            var bytesWritten = 0
            readByteBuffer?.let { readByteBuffer ->
                renderingAudioDeviceContext?.let {
                    AudioDevice.audioDeviceReadRenderData(it, readByteBuffer)
                }
                audioTrack?.let { audioTrack ->
                    bytesWritten = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                        writeOnLollipop(audioTrack, readByteBuffer, readByteBuffer.capacity())
                    } else {
                        writePreLollipop(audioTrack, readByteBuffer, readByteBuffer.capacity())
                    }
                }
            }

            if (bytesWritten != readByteBuffer?.capacity()) {
                Log.e(TAG, "AudioTrack.write failed: $bytesWritten")
                if (bytesWritten == AudioTrack.ERROR_INVALID_OPERATION) {
                    keepAliveRendererRunnable = false
                    break
                }
            }
            // The byte buffer must be rewinded since byteBuffer.position() is increased at each
            // call to AudioTrack.write(). If we don't do this, will fail the next  AudioTrack.write().
            readByteBuffer?.rewind()
        }
    }

    /*
     * This method enables a capturer switch between a file and the microphone.
     * @param playMusic
     */
    fun switchInput(playMusic: Boolean) {
        isMusicPlaying = playMusic
        if (playMusic) {
            initializeStreams()
            capturerHandler?.removeCallbacks(microphoneCapturerRunnable)
            stopRecording()
            capturerHandler?.post(fileCapturerRunnable)
        } else {
            capturerHandler?.removeCallbacks(fileCapturerRunnable)
            capturerHandler?.post(microphoneCapturerRunnable)
        }
    }

    /*
     * Return the AudioFormat used the capturer. This custom device uses 44.1kHz sample rate and
     * STEREO channel configuration both for microphone and the music file.
     */
    override fun getCapturerFormat(): AudioFormat? {
        return AudioFormat(
            AudioFormat.AUDIO_SAMPLE_RATE_44100,
            AudioFormat.AUDIO_SAMPLE_STEREO
        )
    }

    /*
     * Init the capturer using the AudioFormat return by getCapturerFormat().
     */
    override fun onInitCapturer(): Boolean {
        val bytesPerFrame = 2 * (BITS_PER_SAMPLE / 8)
        val framesPerBuffer = capturerFormat!!.sampleRate / BUFFERS_PER_SECOND
        // Calculate the minimum buffer size required for the successful creation of
        // an AudioRecord object, in byte units.
        val channelConfig = channelCountToConfiguration(capturerFormat!!.channelCount)
        val minBufferSize = AudioRecord.getMinBufferSize(
            capturerFormat!!.sampleRate,
            channelConfig, android.media.AudioFormat.ENCODING_PCM_16BIT
        )
        micWriteBuffer = ByteBuffer.allocateDirect(bytesPerFrame * framesPerBuffer)
        val tempMicWriteBuffer = micWriteBuffer
        tempMicWriteBuffer?.capacity()?.let {
            val bufferSizeInBytes = Math.max(BUFFER_SIZE_FACTOR * minBufferSize, it)
            audioRecord = AudioRecord(
                MediaRecorder.AudioSource.MIC,
                capturerFormat!!.sampleRate,
                android.media.AudioFormat.CHANNEL_OUT_STEREO,
                android.media.AudioFormat.ENCODING_PCM_16BIT,
                bufferSizeInBytes
            )
        }
        fileWriteByteBuffer = ByteBuffer.allocateDirect(bytesPerFrame * framesPerBuffer)
        val testFileWriteByteBuffer = fileWriteByteBuffer
        testFileWriteByteBuffer?.capacity()?.let {
            writeBufferSize = it
        }
        // Initialize the streams.
        initializeStreams()
        return true
    }

    override fun onStartCapturing(audioDeviceContext: AudioDeviceContext): Boolean {
        // Initialize the AudioDeviceContext
        capturingAudioDeviceContext = audioDeviceContext
        // Create the capturer thread and start
        capturerThread = HandlerThread("CapturerThread")
        capturerThread?.start()
        // Create the capturer handler that processes the capturer Runnables.
        capturerThread?.looper?.let {
            capturerHandler = Handler(it)
        }

        isMusicPlaying = true
        capturerHandler?.post(fileCapturerRunnable)
        switchInput(false)
        return true
    }

    override fun onStopCapturing(): Boolean {
        if (isMusicPlaying) {
            isMusicPlaying = false
            closeStreams()
        } else {
            stopRecording()
        }
        /*
         * When onStopCapturing is called, the AudioDevice API expects that at the completion
         * of the callback the capturer has completely stopped. As a result, quit the capturer
         * thread and explicitly wait for the thread to complete.
         */
        capturerThread?.quit()
        capturerThread?.let {
            if (!ThreadUtils.joinUninterruptibly(it, THREAD_JOIN_TIMEOUT_MS)) {
                Log.e(TAG, "Join of capturerThread timed out")
                return false
            }
        }
        return true
    }

    /*
     * Return the AudioFormat used the renderer. This custom device uses 44.1kHz sample rate and
     * STEREO channel configuration for audio track.
     */
    override fun getRendererFormat(): AudioFormat? {
        return AudioFormat(
            AudioFormat.AUDIO_SAMPLE_RATE_44100,
            AudioFormat.AUDIO_SAMPLE_STEREO
        )
    }

    override fun onInitRenderer(): Boolean {
        val bytesPerFrame = rendererFormat!!.channelCount * (BITS_PER_SAMPLE / 8)
        readByteBuffer =
            ByteBuffer.allocateDirect(bytesPerFrame * (rendererFormat!!.sampleRate / BUFFERS_PER_SECOND))
        val channelConfig = channelCountToConfiguration(rendererFormat!!.channelCount)
        val minBufferSize = AudioRecord.getMinBufferSize(
            rendererFormat!!.sampleRate,
            channelConfig,
            android.media.AudioFormat.ENCODING_PCM_16BIT
        )
        audioTrack = AudioTrack(
            AudioManager.STREAM_VOICE_CALL, rendererFormat!!.sampleRate, channelConfig,
            android.media.AudioFormat.ENCODING_PCM_16BIT, minBufferSize, AudioTrack.MODE_STREAM
        )
        keepAliveRendererRunnable = true
        return true
    }

    override fun onStartRendering(audioDeviceContext: AudioDeviceContext): Boolean {
        renderingAudioDeviceContext = audioDeviceContext
        // Create the renderer thread and start
        rendererThread = HandlerThread("RendererThread")
        rendererThread?.start()
        // Create the capturer handler that processes the renderer Runnables.
        rendererThread?.looper?.let {
            rendererHandler = Handler(it)
        }
        rendererHandler?.post(speakerRendererRunnable)
        return true
    }

    override fun onStopRendering(): Boolean {
        stopAudioTrack()
        // Quit the rendererThread's looper to stop processing any further messages.
        rendererThread?.quit()
        /*
         * When onStopRendering is called, the AudioDevice API expects that at the completion
         * of the callback the renderer has completely stopped. As a result, quit the renderer
         * thread and explicitly wait for the thread to complete.
         */
        if (!ThreadUtils.joinUninterruptibly(rendererThread, THREAD_JOIN_TIMEOUT_MS)) {
            Log.e(TAG, "Join of rendererThread timed out")
            return false
        }
        return true
    }

    // Capturer helper methods
    private fun initializeStreams() {
        inputStream = context.resources.openRawResource(
            context.resources.getIdentifier(
                "music",
                "raw",
                context.packageName
            )
        )
        dataInputStream = DataInputStream(inputStream)
        try {
            val bytes = dataInputStream?.skipBytes(WAV_FILE_HEADER_SIZE)
            Log.d(TAG, "Number of bytes skipped : $bytes")
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    private fun closeStreams() {
        Log.d(
            TAG,
            "Remove any pending posts of fileCapturerRunnable that are in the message queue "
        )
        capturerHandler?.removeCallbacks(fileCapturerRunnable)
        try {
            dataInputStream?.close()
            inputStream?.close()
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    private fun stopRecording() {
        Log.d(
            TAG,
            "Remove any pending posts of microphoneCapturerRunnable that are in the message queue "
        )
        capturerHandler?.removeCallbacks(microphoneCapturerRunnable)
        try {
            audioRecord?.stop()
        } catch (e: IllegalStateException) {
            Log.e(TAG, "AudioRecord.stop failed: " + e.message)
        }
    }

    private fun channelCountToConfiguration(channels: Int): Int {
        return if (channels == 1) android.media.AudioFormat.CHANNEL_IN_MONO else android.media.AudioFormat.CHANNEL_IN_STEREO
    }

    private fun processRemaining(bb: ByteBuffer?, chunkSize: Int) {
        bb!!.position(bb.limit()) // move at the end
        bb.limit(chunkSize) // get ready to pad with longs
        while (bb.position() < chunkSize) {
            bb.putLong(0)
        }
        bb.limit(chunkSize)
        bb.flip()
    }

    // Renderer helper methods
    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    private fun writeOnLollipop(
        audioTrack: AudioTrack,
        byteBuffer: ByteBuffer,
        sizeInBytes: Int
    ): Int {
        return audioTrack.write(byteBuffer, sizeInBytes, AudioTrack.WRITE_BLOCKING)
    }

    private fun writePreLollipop(
        audioTrack: AudioTrack,
        byteBuffer: ByteBuffer,
        sizeInBytes: Int
    ): Int {
        return audioTrack.write(byteBuffer.array(), byteBuffer.arrayOffset(), sizeInBytes)
    }

    fun stopAudioTrack() {
        keepAliveRendererRunnable = false
        Log.d(
            TAG,
            "Remove any pending posts of speakerRendererRunnable that are in the message queue "
        )
        rendererHandler?.removeCallbacks(speakerRendererRunnable)
        try {
            audioTrack?.stop()
        } catch (e: IllegalStateException) {
            Log.e(TAG, "AudioTrack.stop failed: " + e.message)
        }
        releaseAudioResources()
    }

    private fun releaseAudioResources() {
        audioTrack?.apply {
            flush()
            release()
        }
    }

    companion object {
        private val TAG = FileAndMicAudioDevice::class.java.simpleName

        // TIMEOUT for rendererThread and capturerThread to wait for successful call to join()
        private const val THREAD_JOIN_TIMEOUT_MS: Long = 2000

        // We want to get as close to 10 msec buffers as possible because this is what the media engine prefers.
        private const val CALLBACK_BUFFER_SIZE_MS = 10

        // Default audio data format is PCM 16 bit per sample. Guaranteed to be supported by all devices.
        private const val BITS_PER_SAMPLE = 16

        // Ask for a buffer size of BUFFER_SIZE_FACTOR * (minimum required buffer size). The extra space
        // is allocated to guard against glitches under high load.
        private const val BUFFER_SIZE_FACTOR = 2
        private const val WAV_FILE_HEADER_SIZE = 44
    }
}
