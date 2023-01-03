package com.android.vadify.ui.chat.call.videocall

//import com.github.nkzawa.emitter.Emitter
import android.annotation.SuppressLint
import android.app.ActivityManager
import android.app.KeyguardManager
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.media.MediaPlayer
import android.os.Build
import android.os.Bundle
import android.os.CountDownTimer
import android.os.Vibrator
import android.util.DisplayMetrics
import android.util.Log
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import android.widget.GridLayout
import androidx.annotation.RequiresApi
import androidx.lifecycle.Observer
import com.afollestad.assent.Permission
import com.afollestad.assent.runWithPermissions
import com.android.vadify.R
import com.android.vadify.VadifyApplication
import com.android.vadify.data.api.enums.CallStatus
import com.android.vadify.data.api.enums.CallType
import com.android.vadify.data.api.models.CallRequestResponse
import com.android.vadify.databinding.ActivityVideoBinding
import com.android.vadify.service.Constants
import com.android.vadify.service.MessageHandling
import com.android.vadify.service.VideoCallNotificationService
import com.android.vadify.ui.baseclass.BaseBackStack
import com.android.vadify.ui.chat.ChatActivity
import com.android.vadify.ui.chat.adapter.VideoCallAdapter
import com.android.vadify.ui.chat.call.CallActivity
import com.android.vadify.ui.chat.call.viewmodel.CallViewModel
import com.sdi.joyersmajorplatform.common.livedataext.throttleClicks
import com.twilio.video.*
import io.socket.emitter.Emitter
import kotlinx.android.synthetic.main.activity_call.*
import kotlinx.android.synthetic.main.activity_video.*
import kotlinx.android.synthetic.main.activity_video.callConnect
import kotlinx.android.synthetic.main.activity_video.disConnectCall
import kotlinx.android.synthetic.main.activity_video.muteBtn
import kotlinx.android.synthetic.main.activity_video.toolbar
import kotlinx.android.synthetic.main.activity_video.volumeBtn
import kotlinx.coroutines.*
import org.json.JSONException
import org.json.JSONObject
import tvi.webrtc.Camera1Enumerator

class VideoCallActivity : VideoBaseClass<ActivityVideoBinding>() {


    private var remoteParticipantIdentity: String? = null

    private var localParticipant: LocalParticipant? = null
    val listVideo = ArrayList<VideoTrack>()
    val viewModel: CallViewModel by viewModels()

    var mediaPlayer: MediaPlayer? = null
    var viberator: Vibrator? = null

    companion object {
        const val VIDEOCALL = "video"
        var memberIds:java.util.ArrayList<String?>?= java.util.ArrayList()
    }

    override val layoutRes: Int
        get() = R.layout.activity_video


    override var camera1Enumerator: Camera1Enumerator = Camera1Enumerator()
    private lateinit var videoAdapter: VideoCallAdapter

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if ((applicationContext as VadifyApplication).updateVideoCallStatus.value?.callStatus == CallStatus.DISCONNECTED ||
            (applicationContext as VadifyApplication).updateVideoCallStatus.value?.callStatus == CallStatus.NONE) {
            (applicationContext as VadifyApplication).updateVideoCallStatus.value?.callStatus = CallStatus.CONNECTING
            (applicationContext as VadifyApplication).remoteParticipantVideo.clear()
        }
        if (intent.action != null){
            if (intent.action == Constants.ACTION_INCOMING_CALL)
                callConnect.visibility = View.GONE
        }
        memberIds = intent.extras?.getStringArrayList(ChatActivity.MEMBER_IDs)
        showOnLockScreenAndTurnScreenOn()
        var isGroup = intent.extras?.getBoolean(BaseBackStack.GROUP_TYPE)!!
        if (isGroup)
        {
            (applicationContext as VadifyApplication).videoCallType = VadifyApplication.VideoCallType.GroupCall
        }
        else{
            if ((applicationContext as VadifyApplication).videoCallType is VadifyApplication.VideoCallType.GroupCall)
            {
                (applicationContext as VadifyApplication).videoCallType = VadifyApplication.VideoCallType.GroupCall
            }
            else{
                (applicationContext as VadifyApplication).videoCallType = VadifyApplication.VideoCallType.SingleCall
            }
        }

        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        val keyguardManager = getSystemService(Context.KEYGUARD_SERVICE) as KeyguardManager
        if (keyguardManager != null)
            keyguardManager.requestDismissKeyguard(this, null)
        intent.extras?.getInt(Constants.SENDER_OR_RECEIVER)
            ?.let { viewModel.callSenderReceiver = it }
        volumeControlStream = AudioManager.STREAM_VOICE_CALL
        val notify1 = intent?.extras?.getInt(Constants.INCOMING_CALL_NOTIFICATION_ID_1)
        val notificationManager =
            getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (notify1 != null) {
            notificationManager.cancel(notify1)
        }
        try {
            audioManager = (getSystemService(Context.AUDIO_SERVICE) as AudioManager).also {
                it.setStreamMute(AudioManager.STREAM_NOTIFICATION, false)
                it.setStreamMute(AudioManager.STREAM_ALARM, false)
                it.setStreamMute(AudioManager.STREAM_MUSIC, false)
                it.setStreamMute(AudioManager.STREAM_RING, false)
                it.setStreamMute(AudioManager.STREAM_SYSTEM, false)

            }
        } catch (e: SecurityException) {
        }


        initVideoCall()
        initSocket()

        subscribe(callConnect.throttleClicks()) {
            runWithPermissions(Permission.CAMERA, Permission.RECORD_AUDIO) {
                callConnect.visibility=View.GONE
                val intent = Intent(applicationContext, VideoCallNotificationService::class.java)
                intent.action = Constants.ACTION_ACCEPT
                intent.putExtra(Constants.IS_INCOMING_CALL, true)
                callIncomingData(intent)
            }
        }
        subscribe(disConnectCall.throttleClicks()) {
            when (viewModel.callSenderReceiver) {
                CallType.RECEIVER.value -> {
                    intent.extras?.getParcelable<CallRequestResponse.Data.Call>(Constants.INCOMING_CALL_INVITE)
                        ?.let {
                            viewModel.receivingAudioCallMethod(it._id, "declined")
                            bindNetworkState(viewModel.callNetworkStatus) {
                                callFinishMethod()
                            }
                        }
                }
                CallType.SENDER.value -> {

                    if ((applicationContext as VadifyApplication).videoCallType is VadifyApplication.VideoCallType.SingleCall)
                    {
                        viewModel.callRequestResponse.value?.roomName?.let {
                            viewModel.receivingAudioCallMethod(it, "end")
                            bindNetworkState(viewModel.callNetworkStatus) {
                                callFinishMethod()
                            }
                        }
                    }
                    if ((applicationContext as VadifyApplication).videoCallType is VadifyApplication.VideoCallType.GroupCall)
                        callFinishMethod()
                }
                else -> {
                    callFinishMethod()

                }
            }

        }


        subscribe(volumeBtn.throttleClicks()) {
            cameraSwitch()
        }

        subscribe(muteBtn.throttleClicks()) {
            muteUnmuteMethod()
        }

        toolbar.setNavigationOnClickListener {
            onBackPressed()
        }

        (applicationContext as VadifyApplication).updateVideoCallStatus.observe(this, Observer {
            it?.let {
                when (it.callStatus) {
                    CallStatus.CONNECTED -> {
                        audioManager = (getSystemService(Context.AUDIO_SERVICE) as AudioManager).also {it1->
                            it1.isSpeakerphoneOn = true
                        }
                        thumbnail_video_view.visibility = View.VISIBLE
                        callConnect.visibility=View.GONE
                        it.room?.let { onConnected(it) }

                    }
                    CallStatus.PARTICIPATE_CONNECTED -> {
                        callConnect.visibility=View.GONE
                        thumbnail_video_view.visibility = View.VISIBLE
                        it.remoteParticipant?.let {
                            //cancelRingtone()
                            onParticipantConnected(it)
                            //   name_dp_container.visibility = View.GONE

                        }
                    }
                    CallStatus.PARTICIPATE_DISCONNECTED -> {
                        if ((applicationContext as VadifyApplication).videoCallType is VadifyApplication.VideoCallType.SingleCall) {
                            callFinishMethod()
                        }else{

                        }
                    }
                    CallStatus.DISCONNECTED -> callFinishMethod()
                    CallStatus.FAILED -> callFinishMethod()
                    else -> {
                    }
                }
            }
        })
        findViewById<GridLayout>(R.id.videoContainer).setOnClickListener{
            showHideView()
        }

        addAllvideo()
    }


    private fun initSocket() {
        try {
            viewModel.getSocketInstance()?.let {
                it.on(ChatActivity.CALL_LOG_STATUS, callLogStatus)
                it.connect()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }


    private val callLogStatus: Emitter.Listener = Emitter.Listener { args ->
        callFinishMethod()

        // checkOnListStatus(args)
    }


    private fun checkOnListStatus(args: Array<out Any?>) {
        CoroutineScope(Dispatchers.Main).launch {
            try {
                val data: JSONObject = args[0] as JSONObject
                Log.e("data are", "" + data)
            } catch (e: JSONException) {
            }
        }
    }

    private fun callFinishMethod() {
        audioManager?.mode = AudioManager.MODE_NORMAL
        stopService(Intent(this, VideoCallNotificationService::class.java))
        cancelRingtone()
        finish()
    }


    private fun initVideoCall() {
        when (viewModel.callSenderReceiver) {
            CallType.RECEIVER.value -> {
                if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.O) {
                    initRingtone()
                }
                intent.extras?.getParcelable<CallRequestResponse.Data.Call>(Constants.INCOMING_CALL_INVITE)
                    ?.let {
                        viewModel.updateReceiverInformation(it.from.name, it.from.profileImage)

                    }
                //viewModel.updateCall(CallType.RECEIVER)
                //    videoReceiverCallMethod(viewModel)
            }
            CallType.SENDER.value ->
                runWithPermissions(Permission.CAMERA, Permission.RECORD_AUDIO) {
                    callConnect.visibility=View.GONE
                    if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.O) {
                        initRingtone()
                    } else {
                        MessageHandling.initRingtone(VideoCallActivity@ this)
                    }
                    viewModel.updateSenderInformation(
                        intent.extras?.getString(CallActivity.ANOTHER_USER_NAME) ?: "Unknown Name",
                        intent.extras?.getString(CallActivity.ANOTHER_USER_IMAGE) ?: ""
                    )
                    viewModel.updateCall(CallType.SENDER)
                    videoCallMethod(viewModel)
                }
            else -> {
                // intent.extras?.getParcelable<CallRequestResponse.Data.Call>(Constants.INCOMING_CALL_INVITE)
                //Log.d( "updateUi: ", ""+intent.extras?.getString(ANOTHER_USER_NAME))
                viewModel.updateProfileData()
            }
        }


        viewModel.receiverRequestResponse.observe(this, Observer {
            it.roomName?.let { roomName ->
                it.token?.let { token ->
                    volumeControlStream = enableVolumeControl(true)
                    Intent(
                        applicationContext,
                        VideoCallNotificationService::class.java
                    ).also { it ->
                        it.action = Constants.SENDER_CALL
                        it.putExtra(
                            Constants.INCOMING_CALL_NOTIFICATION_ID,
                            System.currentTimeMillis().toInt()
                        )
                        it.putExtra(Constants.ROOM_NAME, roomName)
                        it.putExtra(Constants.TOKEN, token)
                        startService(it)
                    }
                }
            }
        })

        viewModel.callRequestResponse.observe(this, androidx.lifecycle.Observer {
            it.roomName?.let { roomName ->
                it.token?.let { token ->
                    volumeControlStream = enableVolumeControl(true)
                    Intent(
                        applicationContext,
                        VideoCallNotificationService::class.java
                    ).also { it ->
                        it.action = Constants.SENDER_CALL
                        it.putExtra(
                            Constants.INCOMING_CALL_NOTIFICATION_ID,
                            System.currentTimeMillis().toInt()
                        )
                        it.putExtra(Constants.ROOM_NAME, roomName)
                        it.putExtra(Constants.TOKEN, token)
                        startService(it)
                    }
                }
            }
        })
    }


    private fun onConnected(room: Room) {
        //audioManager?.let { it.isSpeakerphoneOn = true }
        if (viewModel.callSenderReceiver == CallType.RECEIVER.value) {
            //  cancelRingtone()
            viewModel.updateCallStatus(CallStatus.CONNECTED)
            viewModel.updateCall(CallType.CONNECTED)
        }
        (applicationContext as VadifyApplication).localVideoTrack?.let { updataLocalTrack(it) }
        (applicationContext as VadifyApplication).camera1Enumerator?.let { camera1Enumerator = it }
        (applicationContext as VadifyApplication).cameraCapturer?.let { cameraCapturer = it }
        localParticipant = room.localParticipant
        for (remoteParticipant in room.remoteParticipants) {
            addRemoteParticipant(remoteParticipant)
        }
    }

    private fun onParticipantConnected(remoteParticipant: RemoteParticipant) {
        // cancelRingtone()

        // if( (applicationContext as VadifyApplication).videoCallType is VadifyApplication.VideoCallType.SingleCall) {
        viewModel.updateCallStatus(CallStatus.CONNECTED)
        viewModel.updateCall(CallType.CONNECTED)
        addRemoteParticipant(remoteParticipant)
        // }
    }

    private fun cameraSwitch() {
        cameraCapturer?.let {
            val cameraId =
                if (it.cameraId == getFrontCameraId()) getBackCameraId() else getFrontCameraId()!!
            cameraId?.let { cameraCapturer?.switchCamera(it) }
            if (thumbnail_video_view.visibility == View.VISIBLE) {
                thumbnail_video_view.mirror = cameraId == getBackCameraId()
            } else {
                //  primary_video_view.mirror = cameraId == getBackCameraId()
            }
        }
    }


    /*
     * Called when participant leaves the room
     */
    @SuppressLint("SetTextI18n")
    private fun removeParticipant(remoteParticipant: RemoteParticipant) {
        if (remoteParticipant.identity != remoteParticipantIdentity) return
        /*
         * Remove participant renderer
         */if (remoteParticipant.remoteVideoTracks.size > 0) {
            val remoteVideoTrackPublication = remoteParticipant.remoteVideoTracks[0]
            /*
             * Remove video only if subscribed to participant track.
             */if (remoteVideoTrackPublication.isTrackSubscribed) {
                removeParticipantVideo(remoteVideoTrackPublication.remoteVideoTrack!!)
            }
        }
        moveLocalVideoToPrimaryView()
    }


    private fun moveLocalVideoToPrimaryView() {
//        if (thumbnail_video_view.visibility == View.VISIBLE) {
//            localVideoTrack?.removeSink(thumbnail_video_view)
//            thumbnail_video_view.setVisibility(View.GONE)
//            localVideoTrack?.removeSink(primary_video_view)
//            localVideoView = primary_video_view
//            primary_video_view.mirror = cameraCapturer?.cameraId == getFrontCameraId()
//        }
    }


    /*
     * Called when remote participant joins the room
     */
    @SuppressLint("SetTextI18n")
    private fun addRemoteParticipant(remoteParticipant: RemoteParticipant) {
        if (thumbnail_video_view.visibility == View.GONE) {
            showMessage("Rendering multiple participants not supported in this app")
            return
        }
        remoteParticipantIdentity = remoteParticipant.identity
        Log.d("identity: ", remoteParticipantIdentity!!)
        //statusTextView.setText("RemoteParticipant $remoteParticipantIdentity joined")
        /*
         * Add remote participant renderer
         */if (remoteParticipant.remoteVideoTracks.size > 0) {
            Log.d("addRemoteParticipant: ", "" + remoteParticipant.remoteVideoTracks.size)
            val remoteVideoTrackPublication = remoteParticipant.remoteVideoTracks[0]
            /*
             * Only render video tracks that are subscribed to
             */
//            if (remoteVideoTrackPublication.isTrackSubscribed) {
//                remoteVideoTrackPublication.remoteVideoTrack?.let { addRemoteParticipantVideo(it) }
//            }
        }
        /*
         * Start listening for participant media events
         */
        remoteParticipant.setListener(mediaListener())
    }


    @SuppressLint("SetTextI18n")
    private fun mediaListener(): RemoteParticipant.Listener? {
        return object : RemoteParticipant.Listener {
            override fun onAudioTrackPublished(
                remoteParticipant: RemoteParticipant,
                remoteAudioTrackPublication: RemoteAudioTrackPublication
            ) {
                //status_textview.setText("onAudioTrackPublished")
            }

            override fun onAudioTrackUnpublished(
                remoteParticipant: RemoteParticipant,
                remoteAudioTrackPublication: RemoteAudioTrackPublication
            ) {
                //  status_textview.setText("onAudioTrackPublished")
            }

            override fun onVideoTrackPublished(
                remoteParticipant: RemoteParticipant,
                remoteVideoTrackPublication: RemoteVideoTrackPublication
            ) {
                // status_textview.setText("onVideoTrackPublished")
            }

            override fun onVideoTrackUnpublished(
                remoteParticipant: RemoteParticipant,
                remoteVideoTrackPublication: RemoteVideoTrackPublication
            ) {
                // status_textview.setText("onVideoTrackUnpublished")
            }

            override fun onDataTrackPublished(
                remoteParticipant: RemoteParticipant,
                remoteDataTrackPublication: RemoteDataTrackPublication
            ) {
                // statusTextView.setText("onDataTrackPublished")
            }

            override fun onDataTrackUnpublished(
                remoteParticipant: RemoteParticipant,
                remoteDataTrackPublication: RemoteDataTrackPublication
            ) {
                //status_textview.setText("onDataTrackUnpublished")
            }

            override fun onAudioTrackSubscribed(
                remoteParticipant: RemoteParticipant,
                remoteAudioTrackPublication: RemoteAudioTrackPublication,
                remoteAudioTrack: RemoteAudioTrack
            ) {
                //status_textview.setText("onAudioTrackSubscribed")
            }

            override fun onAudioTrackUnsubscribed(
                remoteParticipant: RemoteParticipant,
                remoteAudioTrackPublication: RemoteAudioTrackPublication,
                remoteAudioTrack: RemoteAudioTrack
            ) {
                //status_textview.setText("onAudioTrackUnsubscribed")
            }

            override fun onAudioTrackSubscriptionFailed(
                remoteParticipant: RemoteParticipant,
                remoteAudioTrackPublication: RemoteAudioTrackPublication,
                twilioException: TwilioException
            ) {
                // status_textview.setText("onAudioTrackSubscriptionFailed")
            }

            override fun onVideoTrackSubscribed(
                remoteParticipant: RemoteParticipant,
                remoteVideoTrackPublication: RemoteVideoTrackPublication,
                remoteVideoTrack: RemoteVideoTrack
            ) {
                //   status_textview.setText("onVideoTrackSubscribed")
                // addRemoteParticipantVideo(remoteVideoTrack)
                (applicationContext as VadifyApplication).remoteParticipantVideo.add(remoteVideoTrack)
                addAllvideo()
            }

            override fun onVideoTrackUnsubscribed(
                remoteParticipant: RemoteParticipant,
                remoteVideoTrackPublication: RemoteVideoTrackPublication,
                remoteVideoTrack: RemoteVideoTrack
            ) {
                // status_textview.setText("onVideoTrackUnsubscribed")
                (applicationContext as VadifyApplication).remoteParticipantVideo.remove(remoteVideoTrack)
                if((applicationContext as VadifyApplication).remoteParticipantVideo.size==0)
                    callFinishMethod()
                else
                    addAllvideo()
                // removeParticipantVideo(remoteVideoTrack)
            }

            override fun onVideoTrackSubscriptionFailed(
                remoteParticipant: RemoteParticipant,
                remoteVideoTrackPublication: RemoteVideoTrackPublication,
                twilioException: TwilioException
            ) {
                //status_textview.setText("onVideoTrackSubscriptionFailed")
                showMessage("Failed to subscribe to %s video track")

            }

            override fun onDataTrackSubscribed(
                remoteParticipant: RemoteParticipant,
                remoteDataTrackPublication: RemoteDataTrackPublication,
                remoteDataTrack: RemoteDataTrack
            ) {
                //status_textview.setText("onDataTrackSubscribed")
            }

            override fun onDataTrackUnsubscribed(
                remoteParticipant: RemoteParticipant,
                remoteDataTrackPublication: RemoteDataTrackPublication,
                remoteDataTrack: RemoteDataTrack
            ) {
                // status_textview.setText("onDataTrackUnsubscribed")
            }

            override fun onDataTrackSubscriptionFailed(
                remoteParticipant: RemoteParticipant,
                remoteDataTrackPublication: RemoteDataTrackPublication,
                twilioException: TwilioException
            ) {
                //status_textview.setText("onDataTrackSubscriptionFailed")
            }

            override fun onAudioTrackEnabled(
                remoteParticipant: RemoteParticipant,
                remoteAudioTrackPublication: RemoteAudioTrackPublication
            ) {
            }


            override fun onAudioTrackDisabled(
                remoteParticipant: RemoteParticipant,
                remoteAudioTrackPublication: RemoteAudioTrackPublication
            ) {
            }

            override fun onVideoTrackEnabled(
                remoteParticipant: RemoteParticipant,
                remoteVideoTrackPublication: RemoteVideoTrackPublication
            ) {
            }

            override fun onVideoTrackDisabled(
                remoteParticipant: RemoteParticipant,
                remoteVideoTrackPublication: RemoteVideoTrackPublication
            ) {
            }
        }
    }

    private fun removeParticipantVideo(videoTrack: VideoTrack) {
        //    videoTrack.removeSink(primary_video_view)
        // videoAdapter.removeParticipant(videoTrack)
    }


    /*
     * Set primary view as renderer for participant video track
     */
    fun addAllvideo()
    {
        if (!(applicationContext as VadifyApplication).remoteParticipantVideo.isNullOrEmpty())
        {
            (applicationContext as VadifyApplication).localVideoTrack?.let { updataLocalTrack(it) }
            showHideView()
            videoContainer.removeAllViews()
            name_dp_container.visibility = View.GONE
            (applicationContext as VadifyApplication).remoteParticipantVideo.forEach()
            {
                addRemoteParticipantVideo(it)
            }
        }

    }

    private fun addRemoteParticipantVideo(videoTrack: RemoteVideoTrack) {

        var column =2
        var row =2
        cancelRingtone()
        name_dp_container.visibility = View.GONE
        val param = GridLayout.LayoutParams()
        param.height = GridLayout.LayoutParams.WRAP_CONTENT

        if ((applicationContext as VadifyApplication).videoCallType is VadifyApplication.VideoCallType.SingleCall ||
            (applicationContext as VadifyApplication).remoteParticipantVideo.size==1  )

        {
            column = 1
            row = 1
            param.height = GridLayout.LayoutParams.MATCH_PARENT

        }

        videoContainer.columnCount = column;
        videoContainer.rowCount = row + 1;
        val videoView = VideoView(this@VideoCallActivity)
        videoTrack.addSink(videoView)
        param.width = getScreenSIze()[0] / column
        param.rightMargin = 10
        param.leftMargin = 10
        param.topMargin = 10
        param.setGravity(Gravity.CENTER);
        videoView.layoutParams = param
        videoContainer.addView(videoView)
        /* videoView.setOnClickListener{
             showHideView()
         }*/
        showHideView()
    }

//        for(i in 0..1){
//            val ll = LinearLayout(this)
//            val layoutParams: LinearLayout.LayoutParams = LinearLayout.LayoutParams(getScreenSIze()/2,200)
//            layoutParams.setMargins(10, 10, 10, 0)
//            val videoView = VideoView(this)
//            videoView.layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
//            videoView.layoutParams
//            listVideo.add(videoTrack)
//            videoTrack.addSink(videoView)
//            ll.addView(videoView, layoutParams)
//            videoContainer.addView(ll)
//        }


//        val gridLayoutManager = GridLayoutManager(this, 4)
//        video_container.layoutManager = gridLayoutManager
//        videoAdapter = VideoCallAdapter(listVideo)
//        video_container.adapter = videoAdapter

    private fun getScreenSIze(): IntArray {
        val displaymetrics = DisplayMetrics()
        windowManager.defaultDisplay.getMetrics(displaymetrics)
        val h: Int = displaymetrics.heightPixels
        val w: Int = displaymetrics.widthPixels
        return intArrayOf(w, h)
    }

    private fun moveLocalVideoToThumbnailView() {
        if (thumbnail_video_view.visibility == View.GONE) {
            thumbnail_video_view.visibility = View.VISIBLE
            localVideoTrack?.let {
                //    it.removeSink(primary_video_view)
                it.addSink(thumbnail_video_view)
            }
            localVideoView = thumbnail_video_view
            thumbnail_video_view.mirror = cameraCapturer?.cameraId == getFrontCameraId()
        }
    }

    override fun onBindView(binding: ActivityVideoBinding) {
        binding.vm = viewModel
    }


    private fun muteUnmuteMethod() {
        audioManager?.also {
            it.isMicrophoneMute = !it.isMicrophoneMute
            viewModel.updateDrawable(!it.isMicrophoneMute)
        }
    }


    private fun initRingtone() {
        viberator =
            (getSystemService(Context.VIBRATOR_SERVICE) as Vibrator).also { it.vibrate(500) }
        //if(viewModel.isSoundEnable()){
        mediaPlayer = MediaPlayer.create(this, R.raw.music)
        mediaPlayer?.start()
        // }
    }

    private fun cancelRingtone() {
        if (android.os.Build.VERSION.SDK_INT > android.os.Build.VERSION_CODES.O) {
            MessageHandling.mediaPlayer?.stop()
            MessageHandling.viberator?.cancel()
        } else {
            mediaPlayer?.stop()
            viberator?.cancel()

        }
    }


    private fun callIncomingData(intent1: Intent) {
        intent.extras?.getParcelable<CallRequestResponse.Data.Call>(Constants.INCOMING_CALL_INVITE)
            ?.let { userdata ->
                intent1.putExtra(
                    Constants.INCOMING_CALL_NOTIFICATION_ID,
                    getIntent().extras?.getInt(Constants.INCOMING_CALL_NOTIFICATION_ID)
                )
                intent1.putExtra(Constants.INCOMING_CALL_INVITE, userdata)
                startService(intent1)
                viewModel.needToCallMethod = true
            }
    }

    override fun onBackPressed() {
        cancelRingtone()
        val intent = Intent(applicationContext, VideoCallNotificationService::class.java)
        intent.action = Constants.ACTION_INCOMING_CALL
        if (!isMyServiceRunning(VideoCallNotificationService::class.java)) callIncomingData(intent)
        finish()
    }

    private fun isMyServiceRunning(serviceClass: Class<*>): Boolean {
        val manager: ActivityManager = getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        for (service in manager.getRunningServices(Int.MAX_VALUE)) {
            if (serviceClass.name == service.service.className) {
                return true
            }
        }
        return false
    }

    override fun onDestroy() {
        super.onDestroy()
        disconnectMethod()
    }


    private fun disconnectMethod() {
        viewModel.mSocket?.let {
            it.disconnect()
            it.off(ChatActivity.CALL_LOG_STATUS, callLogStatus)
        }
    }


    private fun callTimerCountMethod() {
        if (timer == null) {
            timer = object : CountDownTimer(TIMER, DELAY) {
                override fun onTick(millisUntilFinished: Long) {
                    Log.e("counter", "" + millisUntilFinished)
                }

                override fun onFinish() {
                    timer = null
                    val callStatus =
                        (applicationContext as VadifyApplication).updateVideoCallStatus.value?.callStatus
                    if (callStatus == CallStatus.CONNECTED || callStatus == CallStatus.PARTICIPATE_CONNECTED) {
                    } else {

                    }
                }
            }
            timer?.start()
        }
    }

    private fun showHideView(){
        video_controller.also {
            if (it.visibility==View.VISIBLE){
                val anim = AnimationUtils.loadAnimation(applicationContext, R.anim.slide_down)
                it.startAnimation(anim)
                it.visibility =  View.GONE
            }
            else {
                val anim = AnimationUtils.loadAnimation(applicationContext, R.anim.slide_up)
                it.startAnimation(anim)
                anim.setAnimationListener(object : Animation.AnimationListener{
                    override fun onAnimationStart(animation: Animation?) {

                    }

                    override fun onAnimationEnd(animation: Animation?) {
                        it.visibility =  View.VISIBLE
                        CoroutineScope(Dispatchers.IO).launch {
                            delay(2000)
                            CoroutineScope(Dispatchers.Main).launch {
                                showHideView()
                            }
                        }
                    }

                    override fun onAnimationRepeat(animation: Animation?) {
                    }

                })
            }
        }
    }
    private fun showOnLockScreenAndTurnScreenOn() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
            val keyguardManager =
                getSystemService(Context.KEYGUARD_SERVICE) as KeyguardManager
            keyguardManager?.requestDismissKeyguard(this, null)
        } else {

            window.addFlags(
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED
                        or WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
                        or WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD
            )
        }
    }
}

//9780684024