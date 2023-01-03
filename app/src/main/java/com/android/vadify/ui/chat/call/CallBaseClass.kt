package com.android.vadify.ui.chat.call

import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.os.Build
import android.os.CountDownTimer
import androidx.databinding.ViewDataBinding
import com.afollestad.assent.Permission
import com.afollestad.assent.runWithPermissions
import com.android.vadify.R
import com.android.vadify.data.api.models.CallRequestResponse
import com.android.vadify.service.Constants
import com.android.vadify.ui.baseclass.DataBindingActivity
import com.android.vadify.ui.chat.call.viewmodel.CallViewModel
import com.sdi.joyersmajorplatform.common.progressDialog
import com.twilio.video.ConnectOptions
import com.twilio.video.LocalAudioTrack
import com.twilio.video.Room


abstract class CallBaseClass<ActivityBinding : ViewDataBinding> :
    DataBindingActivity<ActivityBinding>() {

    var room: Room? = null
    var audioManager: AudioManager? = null
    private var previousMicrophoneMute = false
    private var previousAudioMode = 0
    var localAudioTrack: LocalAudioTrack? = null

    // abstract var roomListener: Room.Listener
    var timer: CountDownTimer? = null
    val TIMER = 20000L
    val DELAY = 1000L


    fun connectToRoom(roomName: String, accessToken: String) {
        configureAudio(true)
        val connectOptionsBuilder = ConnectOptions.Builder(accessToken).roomName(roomName)
        /*
         * Add local audio track to connect options to share with participants.
         */
        if (localAudioTrack != null) {
            connectOptionsBuilder.audioTracks(listOf(localAudioTrack))
        }
        // room = Video.connect(this, connectOptionsBuilder.build(),roomListener)
        //setDisconnectAction()
    }


    fun receivingAudioCallMethod(viewModel: CallViewModel) {
        runWithPermissions(Permission.RECORD_AUDIO) {
            createAudioTrack()
            intent.extras?.getParcelable<CallRequestResponse.Data.Call>(Constants.INCOMING_CALL_INVITE)
                ?.let {
                    bindNetworkState(
                        viewModel.callTokenRequest(it._id),
                        progressDialog(R.string.please_wait)
                    )
                }
        }
    }


    private fun createAudioTrack() {
        // Share your microphone
        localAudioTrack = LocalAudioTrack.create(this, true, CallActivity.LOCAL_AUDIO_TRACK_NAME)

    }


    fun configureAudio(enable: Boolean) {
        audioManager?.let { audioManager ->
            if (enable) {
                previousAudioMode = audioManager.mode
                requestAudioFocus()
                audioManager.mode = AudioManager.MODE_IN_COMMUNICATION
                previousMicrophoneMute = audioManager.isMicrophoneMute
                audioManager.isMicrophoneMute = false
            } else {
                audioManager.mode = previousAudioMode
                audioManager.abandonAudioFocus(null)
                audioManager.isMicrophoneMute = previousMicrophoneMute
            }
        }
    }

    private fun requestAudioFocus() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val playbackAttributes = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_VOICE_COMMUNICATION)
                .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                .build()
            val focusRequest = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT)
                .setAudioAttributes(playbackAttributes)
                .setAcceptsDelayedFocusGain(true)
                .setOnAudioFocusChangeListener { i: Int -> }
                .build()
            audioManager?.requestAudioFocus(focusRequest)
        } else {
            audioManager?.requestAudioFocus(
                null,
                AudioManager.STREAM_VOICE_CALL,
                AudioManager.AUDIOFOCUS_GAIN_TRANSIENT
            )
        }
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


}

