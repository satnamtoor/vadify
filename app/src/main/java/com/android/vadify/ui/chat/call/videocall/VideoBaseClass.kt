package com.android.vadify.ui.chat.call.videocall

import android.Manifest
import android.content.pm.PackageManager
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.os.Build
import android.os.CountDownTimer
import androidx.databinding.ViewDataBinding
import com.android.vadify.R
import com.android.vadify.VadifyApplication
import com.android.vadify.data.api.models.CallRequestResponse
import com.android.vadify.service.Constants
import com.android.vadify.ui.baseclass.DataBindingActivity
import com.android.vadify.ui.chat.ChatActivity.Companion.ROOM_ID
import com.android.vadify.ui.chat.call.CallActivity
import com.android.vadify.ui.chat.call.videocall.VideoCallActivity.Companion.memberIds
import com.android.vadify.ui.chat.call.viewmodel.CallViewModel
import com.android.vadify.utils.checkSelfPermissionCompat
import com.sdi.joyersmajorplatform.common.progressDialog
import com.twilio.video.CameraCapturer
import com.twilio.video.LocalAudioTrack
import com.twilio.video.LocalVideoTrack
import com.twilio.video.Room
import kotlinx.android.synthetic.main.activity_video.*
import tvi.webrtc.Camera1Enumerator
import tvi.webrtc.VideoSink


abstract class VideoBaseClass<ActivityBinding : ViewDataBinding> :
    DataBindingActivity<ActivityBinding>() {

    var room: Room? = null
    var audioManager: AudioManager? = null
    var localAudioTrack: LocalAudioTrack? = null
    var localVideoTrack: LocalVideoTrack? = null
    var cameraCapturer: CameraCapturer? = null
    var localVideoView: VideoSink? = null
    var timer: CountDownTimer? = null
    val TIMER = 20000L
    val DELAY = 1000L
    // var  isGroup = false

    private var frontCameraId: String? = null
    private var previousMicrophoneMute = false
    private var previousAudioMode = 0
    private var backCameraId: String? = null
    abstract var camera1Enumerator: Camera1Enumerator


//    fun createLocalTracks() {
//        localAudioTrack = LocalAudioTrack.create(this, true)
//        getFrontCameraId()?.let { cameraCapturer = CameraCapturer(this, it) }
//        cameraCapturer?.let { localVideoTrack = LocalVideoTrack.create(this, true, it) }
//        primary_video_view.mirror = true
//        localVideoTrack?.addSink(primary_video_view)
//        localVideoView = primary_video_view
////
//    }

    fun createLocalTracks() {
        localAudioTrack = LocalAudioTrack.create(this, true)
        getFrontCameraId()?.let { cameraCapturer = CameraCapturer(this, it) }
        cameraCapturer?.let { localVideoTrack = LocalVideoTrack.create(this, true, it) }
        (applicationContext as VadifyApplication).localVideoTrack = localVideoTrack
    }

    fun updataLocalTrack(it: LocalVideoTrack) {
        localVideoTrack = it
        thumbnail_video_view.mirror = true
        localVideoTrack?.addSink(thumbnail_video_view)
        localVideoView = thumbnail_video_view
    }



    fun videoCallMethod(viewModel: CallViewModel) {
        if (checkSelfPermissionCompat(Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
            && checkSelfPermissionCompat(Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            if ((applicationContext as VadifyApplication).videoCallType is VadifyApplication.VideoCallType.GroupCall){
                val memberid = memberIds?.map { it!!.split(",")[1] } as ArrayList<String>

                intent.extras?.getString(ROOM_ID)?.let {
                    bindNetworkState(
                        viewModel.groupCallRequestApi(memberid, VideoCallActivity.VIDEOCALL, it)
                    )
                }}
            else
                intent.extras?.getString(CallActivity.ANOTHER_USERID)?.let {
                    bindNetworkState(
                        viewModel.callSingleRequestApi(VideoCallActivity.VIDEOCALL, it)
                    )
                }
        }
    }



    fun videoReceiverCallMethod(viewModel: CallViewModel) {
        intent.extras?.getParcelable<CallRequestResponse.Data.Call>(Constants.INCOMING_CALL_INVITE)
            ?.let {
                bindNetworkState(
                    viewModel.callTokenRequest(it._id),
                    progressDialog(R.string.please_wait)
                )
            }

    }

    /* fun videoCallRoomConnect(token: String, roomName: String) {
         enableAudioFocus(true)
         volumeControlStream = enableVolumeControl(true)
         val connectOptionsBuilder = ConnectOptions.Builder(token).roomName(roomName)
         localAudioTrack?.let { connectOptionsBuilder.audioTracks(listOf(it)) }
         localVideoTrack?.let { connectOptionsBuilder.videoTracks(listOf(it)) }
         room = Video.connect(this, connectOptionsBuilder.build(), roomListener)
     }*/

    fun enableAudioFocus(focus: Boolean) {
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

    fun getFrontCameraId(): String? {
        if (frontCameraId == null) {
            for (deviceName in camera1Enumerator.deviceNames) {
                if (camera1Enumerator.isFrontFacing(deviceName)) {
                    frontCameraId = deviceName
                }
            }
        }
        return frontCameraId
    }


    fun enableVolumeControl(volumeControl: Boolean): Int {
        return if (volumeControl) {
            AudioManager.STREAM_VOICE_CALL
        } else {
            volumeControlStream
        }
    }

    fun getBackCameraId(): String? {
        if (backCameraId == null) {
            for (deviceName in camera1Enumerator.deviceNames) {
                if (camera1Enumerator.isBackFacing(deviceName)) {
                    backCameraId = deviceName
                }
            }
        }
        return backCameraId
    }

    fun requestAudioFocus() {
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

