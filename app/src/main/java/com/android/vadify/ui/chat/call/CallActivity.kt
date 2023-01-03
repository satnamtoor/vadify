package com.android.vadify.ui.chat.call

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
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import android.widget.GridLayout
import android.widget.TextView
import androidx.annotation.RequiresApi
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.lifecycle.Observer
import com.afollestad.assent.Permission
import com.afollestad.assent.runWithPermissions
import com.android.vadify.R
import com.android.vadify.VadifyApplication
import com.android.vadify.data.api.enums.CallStatus
import com.android.vadify.data.api.enums.CallType
import com.android.vadify.data.api.models.CallRequestResponse
import com.android.vadify.databinding.ActivityCallBinding
import com.android.vadify.service.CallNotificationService
import com.android.vadify.service.Constants
import com.android.vadify.service.MessageHandling
import com.android.vadify.service.MessageHandling.Companion.initRingtone
import com.android.vadify.ui.baseclass.BaseBackStack
import com.android.vadify.ui.chat.ChatActivity
import com.android.vadify.ui.chat.call.viewmodel.CallViewModel
import com.google.gson.Gson
import com.sdi.joyersmajorplatform.common.livedataext.throttleClicks
import com.twilio.video.*
import io.socket.emitter.Emitter
import kotlinx.android.synthetic.main.activity_call.*
import kotlinx.android.synthetic.main.activity_call.callConnect
import kotlinx.android.synthetic.main.activity_call.disConnectCall
import kotlinx.android.synthetic.main.activity_call.muteBtn
import kotlinx.android.synthetic.main.activity_call.toolbar
import kotlinx.android.synthetic.main.activity_video.*
import kotlinx.coroutines.*
import tvi.webrtc.Camera1Enumerator

class CallActivity : AudioBaseClass<ActivityCallBinding>() {


    private var remoteParticipantIdentity: String? = null

    private var localParticipant: LocalParticipant? = null
    val listVideo = ArrayList<VideoTrack>()
    val viewModel: CallViewModel by viewModels()

    var mediaPlayer: MediaPlayer? = null
    var viberator: Vibrator? = null
    lateinit var animGone : Animation
    lateinit var animVisible : Animation
    companion object {
        const val VIDEOCALL = "video"
        var memberIds:java.util.ArrayList<String?>?= java.util.ArrayList()
        const val AUDIO = "audio"
        const val ANOTHER_USERID = "ANOTHER_USERID"
        const val ANOTHER_USER_NAME = "ANOTHER_USER_NAME"
        const val ANOTHER_USER_IMAGE = "ANOTHER_USER_IMAGE"
        const val LOCAL_AUDIO_TRACK_NAME = "mic"


    }

    override val layoutRes: Int
        get() = R.layout.activity_call


    override var camera1Enumerator: Camera1Enumerator = Camera1Enumerator()

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if ((applicationContext as VadifyApplication).updateVideoCallStatus.value?.callStatus == CallStatus.DISCONNECTED ||
            (applicationContext as VadifyApplication).updateVideoCallStatus.value?.callStatus == CallStatus.NONE) {
            (applicationContext as VadifyApplication).updateVideoCallStatus.value?.callStatus = CallStatus.CONNECTING
            (applicationContext as VadifyApplication).remoteParticipantAudio.clear()
        }
        animGone = AnimationUtils.loadAnimation(applicationContext, R.anim.slide_down)
        animVisible = AnimationUtils.loadAnimation(applicationContext, R.anim.slide_up)
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
        if (intent.action != null){
            if (intent.action == Constants.ACTION_INCOMING_CALL)
                callConnect.visibility = View.GONE
        }
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
                val intent = Intent(applicationContext, CallNotificationService::class.java)
                intent.action = Constants.ACTION_ACCEPT
                intent.putExtra(Constants.IS_INCOMING_CALL, true)
                callIncomingData(intent)
            }
        }
        subscribe(disConnectCall.throttleClicks()) {

            when (viewModel.callSenderReceiver) {
                CallType.RECEIVER_VISIT_AGIAN.value -> {
                    intent.extras?.getString(Constants.CALL_ID)
                        ?.let {
                            Log.d( "-idid: ",it)
                            viewModel.receivingAudioCallMethod(it, "declined")
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


        subscribe(muteBtn.throttleClicks()) {
            muteUnmuteMethod()
        }

       /* subscribe(volumeBtn.throttleClicks()) {
            audioManager?.let {
                it.isSpeakerphoneOn = viewModel.isSpeekerMute
                viewModel.updateVolumeDrawable(viewModel.isSpeekerMute)
                viewModel.isSpeekerMute = !viewModel.isSpeekerMute
            }
        }*/

        subscribe(volumeBtncall.throttleClicks())
        {
            audioManager?.let {
                it.isSpeakerphoneOn = viewModel.isSpeekerMute
                viewModel.updateVolumeDrawable(viewModel.isSpeekerMute)
                viewModel.isSpeekerMute = !viewModel.isSpeekerMute
            }
        }

        toolbar.setNavigationOnClickListener {
            onBackPressed()
        }

        (applicationContext as VadifyApplication).updateVideoCallStatus.observe(this, Observer {
            it?.let {
                when (it.callStatus) {
                    CallStatus.CONNECTED -> {
                        audioManager = (getSystemService(AUDIO_SERVICE) as AudioManager).also { it1->
                            it1.isSpeakerphoneOn = true
                        }
                        it.room?.let { onConnected(it) }

                    }
                    CallStatus.PARTICIPATE_CONNECTED -> {
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
        findViewById<GridLayout>(R.id.audioContainer).setOnClickListener{
         //   showHideView()
        }
       // showHideView()
      //  showHideView()
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


    private fun callFinishMethod() {
        audioManager?.mode = AudioManager.MODE_NORMAL
        stopService(Intent(this, CallNotificationService::class.java))
        cancelRingtone()
        finish()
    }


    private fun initVideoCall() {
        when (viewModel.callSenderReceiver) {
            CallType.RECEIVER.value -> {
                intent.extras?.getParcelable<CallRequestResponse.Data.Call>(Constants.INCOMING_CALL_INVITE)
                    ?.let {
                        viewModel.updateReceiverInformation(it.from.name, it.from.profileImage)
                    }
                viewModel.updateCall(CallType.RECEIVER)
                //    videoReceiverCallMethod(viewModel)
            }
            CallType.SENDER.value ->
                runWithPermissions(Permission.CAMERA, Permission.RECORD_AUDIO) {
                    callConnect.visibility=View.GONE
                    if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.O) {
                        initRingtone()
                    } else {
                        initRingtone(CallActivity@this)
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
                        CallNotificationService::class.java
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
                        CallNotificationService::class.java
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
        if (viewModel.callSenderReceiver == CallType.RECEIVER_VISIT_AGIAN.value) {
            cancelRingtone()
            hideViews()
            viewModel.updateCallStatus(CallStatus.CONNECTED)
            callConnect.visibility=View.GONE
            viewModel.updateCall(CallType.RECEIVER)
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
        cancelRingtone()
        hideViews()
        viewModel.updateCallStatus(CallStatus.CONNECTED)
        callConnect.visibility=View.GONE
        viewModel.updateCall(CallType.RECEIVER)
        addRemoteParticipant(remoteParticipant)

    }



    /*
     * Called when participant leaves the room
     */
    @SuppressLint("SetTextI18n")
    private fun removeParticipant(remoteParticipant: RemoteParticipant) {

    }



    /*
     * Called when remote participant joins the room
     */
    @SuppressLint("SetTextI18n")
    private fun addRemoteParticipant(remoteParticipant: RemoteParticipant) {
        remoteParticipantIdentity = remoteParticipant.identity
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
                if ((applicationContext as VadifyApplication).videoCallType is VadifyApplication.VideoCallType.GroupCall) {
                    val connectedUser = memberIds?.first { it!!.contains(remoteParticipant.identity) }
                    cancelRingtone()
                    hideViews()
                    viewModel.updateCallStatus(CallStatus.CONNECTED)
                    (applicationContext as VadifyApplication).remoteParticipantAudio.add(connectedUser?.first().toString())
                    addAllvideo()
                }
            }

            override fun onAudioTrackUnsubscribed(
                remoteParticipant: RemoteParticipant,
                remoteAudioTrackPublication: RemoteAudioTrackPublication,
                remoteAudioTrack: RemoteAudioTrack
            ) {
                if ((applicationContext as VadifyApplication).videoCallType is VadifyApplication.VideoCallType.GroupCall) {
                    val connectedUser = memberIds?.first { it!!.contains(remoteParticipant.identity) }
                    (applicationContext as VadifyApplication).remoteParticipantAudio.remove(connectedUser?.first().toString())
                    if ((applicationContext as VadifyApplication).remoteParticipantAudio.isNullOrEmpty()) {
                        callFinishMethod()
                    }
                    else
                        addAllvideo()
                }

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

            }

            override fun onVideoTrackUnsubscribed(
                remoteParticipant: RemoteParticipant,
                remoteVideoTrackPublication: RemoteVideoTrackPublication,
                remoteVideoTrack: RemoteVideoTrack
            ) {
                // status_textview.setText("onVideoTrackUnsubscribed")
                removeParticipantVideo(remoteVideoTrack)
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
        if (!(applicationContext as VadifyApplication).remoteParticipantAudio.isNullOrEmpty())
        {
            (applicationContext as VadifyApplication).localVideoTrack?.let { updataLocalTrack(it) }
            audioContainer.removeAllViews()
            (applicationContext as VadifyApplication).remoteParticipantAudio.forEach()
            {
                addRemoteParticipantVideo(it)
            }
        }

    }
    private fun hideViews(){
        if ((applicationContext as VadifyApplication).videoCallType is VadifyApplication.VideoCallType.GroupCall) {
            findViewById<ConstraintLayout>(R.id.imageView266).visibility = View.GONE
            findViewById<ConstraintLayout>(R.id.imageView277).visibility = View.GONE
            findViewById<TextView>(R.id.textView50).visibility = View.GONE
            findViewById<TextView>(R.id.textView52).visibility = View.GONE
        }
    }
    private fun addRemoteParticipantVideo(memberName: String) {

        var column =3
        var row =3
        val param = GridLayout.LayoutParams()
        param.height = GridLayout.LayoutParams.WRAP_CONTENT

        if ((applicationContext as VadifyApplication).videoCallType is VadifyApplication.VideoCallType.SingleCall)

        {
            column = 1
            row = 1
            param.height = GridLayout.LayoutParams.MATCH_PARENT

        }

        audioContainer.columnCount = column
        audioContainer.rowCount = row + 1

        val videoView = TextView(this@CallActivity)
        videoView.text = memberName
        videoView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 20F)
        videoView.gravity=Gravity.CENTER
        videoView.setBackgroundResource(R.drawable.blue_circle)
        param.width = getScreenSIze()[0] / column
        param.height = getScreenSIze()[0] / column
        param.rightMargin = 10
        param.leftMargin = 10
        param.topMargin = 10
        param.setGravity(Gravity.CENTER);
        videoView.layoutParams = param
        audioContainer.addView(videoView)
    }


    private fun getScreenSIze(): IntArray {
        val displaymetrics = DisplayMetrics()
        windowManager.defaultDisplay.getMetrics(displaymetrics)
        val h: Int = displaymetrics.heightPixels
        val w: Int = displaymetrics.widthPixels
        return intArrayOf(w, h)
    }


    override fun onBindView(binding: ActivityCallBinding) {
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
        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.O) {
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
        val intent = Intent(applicationContext, CallNotificationService::class.java)
        intent.action = Constants.ACTION_INCOMING_CALL
        if (!isMyServiceRunning(CallNotificationService::class.java)) callIncomingData(intent)
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
        btnContainers.also {
            if (it.visibility== View.VISIBLE){
                it.startAnimation(animVisible)
                animVisible.setAnimationListener(object : Animation.AnimationListener{
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
            else{
                it.startAnimation(animGone)
                it.visibility =  View.VISIBLE
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