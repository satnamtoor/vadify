package com.android.vadify.service

import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.os.Build
import android.os.CountDownTimer
import com.android.vadify.VadifyApplication
import com.twilio.video.*
import dagger.android.DaggerService
import tvi.webrtc.Camera1Enumerator
import java.util.*


abstract class VideoBaseClassService : DaggerService() {

    var room: Room? = null
    var audioManager: AudioManager? = null
    var localAudioTrack: LocalAudioTrack? = null
    var localVideoTrack: LocalVideoTrack? = null
    var timer: CountDownTimer? = null
    val TIMER = 20000L
    val DELAY = 1000L

    private var previousAudioMode = 0
    abstract var roomListener: Room.Listener

    var cameraCapturer: CameraCapturer? = null
    private var frontCameraId: String? = null
    var camera1Enumerator: Camera1Enumerator = Camera1Enumerator()


    fun createLocalTracks() {
        localAudioTrack = LocalAudioTrack.create(this, true)
        getFrontCameraId()?.let { cameraCapturer = CameraCapturer(this, it) }
        cameraCapturer?.let { localVideoTrack = LocalVideoTrack.create(this, true, it,"camera") }
        (applicationContext as VadifyApplication).localVideoTrack = localVideoTrack
        (applicationContext as VadifyApplication).camera1Enumerator = camera1Enumerator
        (applicationContext as VadifyApplication).cameraCapturer = cameraCapturer
    }

    private fun getFrontCameraId(): String? {
        if (frontCameraId == null) {
            for (deviceName in camera1Enumerator.deviceNames) {
                if (camera1Enumerator.isFrontFacing(deviceName)) {
                    frontCameraId = deviceName
                }
            }
        }
        return frontCameraId
    }


    fun videoCallRoomConnect(token: String, roomName: String) {
        enableAudioFocus(true)
        val connectOptionsBuilder = ConnectOptions.Builder(token).roomName(roomName)
        localAudioTrack?.let { connectOptionsBuilder.audioTracks(listOf(it)) }
        (applicationContext as VadifyApplication).localVideoTrack?.let { connectOptionsBuilder.videoTracks(Collections.singletonList(it)) }
        room = Video.connect(this, connectOptionsBuilder.build(), roomListener)
    }

    private fun enableAudioFocus(focus: Boolean) {
        audioManager?.let { audioManager ->
            if (focus) {
                previousAudioMode = audioManager.mode
                // Request audio focus before making any device switch.
                requestAudioFocus()
                audioManager.mode = AudioManager.MODE_IN_COMMUNICATION
            } else {
                audioManager.mode = previousAudioMode
                audioManager.abandonAudioFocus(null)
            }
        }
    }


    private fun requestAudioFocus() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val playbackAttributes =
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_VOICE_COMMUNICATION)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                    .build()
            val focusRequest =
                AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT)
                    .setAudioAttributes(playbackAttributes)
                    .setAcceptsDelayedFocusGain(true)
                    .setOnAudioFocusChangeListener { i: Int -> }
                    .build()
            audioManager?.requestAudioFocus(focusRequest)
        } else {
            audioManager?.requestAudioFocus(
                null, AudioManager.STREAM_VOICE_CALL, AudioManager.AUDIOFOCUS_GAIN_TRANSIENT
            )
        }
    }
}

