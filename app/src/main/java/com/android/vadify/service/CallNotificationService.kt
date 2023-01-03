package com.android.vadify.service

import android.annotation.SuppressLint
import android.annotation.TargetApi
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.os.CountDownTimer
import android.os.IBinder
import android.util.Log
import android.widget.RemoteViews
import androidx.core.app.NotificationCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ProcessLifecycleOwner
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.android.vadify.R
import com.android.vadify.VadifyApplication
import com.android.vadify.data.api.enums.CallStatus
import com.android.vadify.data.api.enums.CallType
import com.android.vadify.data.api.models.CallRequestResponse
import com.android.vadify.data.api.models.CallStatusRequest
import com.android.vadify.data.repository.UserRepository
import com.android.vadify.data.service.PreferenceService
import com.android.vadify.service.MessageHandling.Companion.mediaPlayer
import com.android.vadify.service.MessageHandling.Companion.viberator
import com.android.vadify.ui.chat.ChatActivity
import com.android.vadify.ui.chat.call.CallActivity
import com.google.gson.Gson
import com.twilio.video.RemoteParticipant
import com.twilio.video.Room
import com.twilio.video.TwilioException
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.rxkotlin.subscribeBy
import io.reactivex.schedulers.Schedulers
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.*
import java.io.IOException
import java.net.URL
import java.util.ArrayList
import javax.inject.Inject

class CallNotificationService : BaseCall() {


    @Inject
    lateinit var preferenceService: PreferenceService

    @Inject
    lateinit var userRepository: UserRepository


    override var roomListener: Room.Listener = roomListener()


    var notificationId = 0
    var isIncomingCall = false

    var checkStatus = CallStatus.CONNECTING


    val ACCEPT = "accept"
    val DECLINED = "declined"
    val END = "end"

    override fun onCreate() {
        super.onCreate()
        createLocalTracks()
        callTimerCountMethod()
    }

    override fun onBind(p0: Intent?): IBinder? {
        return null
    }

    var callLogId: String = ""


    override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {
        val action = intent.action
        if (action != null) {
            val callInvite: CallRequestResponse.Data.Call? =
                intent.getParcelableExtra(Constants.INCOMING_CALL_INVITE)
            notificationId = intent.getIntExtra(Constants.INCOMING_CALL_NOTIFICATION_ID, 0)
            isIncomingCall = intent.getBooleanExtra(Constants.IS_INCOMING_CALL, false)
            callLogId = intent.getStringExtra(Constants.CALL_LOGER_ID) ?: ""
            when (action) {
                Constants.ACTION_INCOMING_CALL -> {
                    isIncomingCall = true
                    handleIncomingCall(callInvite, notificationId)
                }
                Constants.ACTION_ACCEPT -> callInvite?._id?.let { id ->
                    timer?.cancel()
                    receivingAudioCallMethod(
                        id,
                        ACCEPT,
                        callInvite.members
                    )
                }
                Constants.ACTION_REJECT -> {
                    cancelRingtone()
                    if (callInvite?.type == "Group")
                        endForeground()
                    if (callInvite?.type == "Single")
                        receivingAudioCallMethod(callLogId, END, callInvite?.members)
                }  // endForeground()
                Constants.ACTION_CANCEL_CALL -> handleCancelledCall(intent)
                Constants.SENDER_CALL -> {
                    intent.getStringExtra(Constants.ROOM_NAME)?.let { roomName ->
                        callLogId = roomName
                        intent.getStringExtra(Constants.TOKEN)?.let { token ->
                            startForeground(
                                notificationId,
                                senderNotification(
                                    roomName,
                                    notificationId,
                                    NotificationManager.IMPORTANCE_LOW
                                )
                            )
                            videoCallRoomConnect(token, roomName)
                        }
                    }
                }
            }
        } else
        /*    if (intent.extras!= null)
                if (intent.extras?.getBoolean("fromNotiy",false)!!)*/
            showNotificationWithBitmap(intent)
        return START_NOT_STICKY
    }

    private fun createNotification(
        callInvite: CallRequestResponse.Data.Call?, notificationId: Int, channelImportance: Int
    ): Notification {
        val intent = Intent(this, CallActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this,
            notificationId,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT
        )

        /*
         * Pass the notification id and call sid to use as an identifier to cancel the
         * notification later
         */
        val extras = Bundle()
        // extras.putString(Constants.CALL_SID_KEY, callInvite.getCallSid());
        val rejectIntent = Intent(applicationContext, CallNotificationService::class.java)
        rejectIntent.action = Constants.ACTION_REJECT
        rejectIntent.putExtra(Constants.CALL_LOGER_ID, callInvite?._id)
        rejectIntent.putExtra(Constants.INCOMING_CALL_INVITE, callInvite)
        rejectIntent.putExtra(Constants.INCOMING_CALL_NOTIFICATION_ID, notificationId)
        val piRejectIntent = PendingIntent.getService(
            applicationContext,
            0,
            rejectIntent,
            PendingIntent.FLAG_UPDATE_CURRENT
        )

        val acceptIntent = Intent(applicationContext, CallNotificationService::class.java)
        acceptIntent.action = Constants.ACTION_ACCEPT
        acceptIntent.putExtra(Constants.INCOMING_CALL_INVITE, callInvite)
        acceptIntent.putExtra(Constants.SENDER_OR_RECEIVER, CallType.RECEIVER_VISIT_AGIAN)
        acceptIntent.putExtra(Constants.INCOMING_CALL_NOTIFICATION_ID, notificationId)
        val piAcceptIntent = PendingIntent.getService(
            applicationContext,
            0,
            acceptIntent,
            PendingIntent.FLAG_UPDATE_CURRENT
        )

        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            buildNotification(
                callInvite?.from?.name + " is calling.",
                pendingIntent,
                extras,
                createChannel(channelImportance),
                piRejectIntent,
                piAcceptIntent
            )
        } else {
            NotificationCompat.Builder(this)
                .setSmallIcon(R.drawable.ic_call_end_white_24dp)
                .setContentTitle(getString(R.string.app_name))
                .setContentText(callInvite?.from?.name.toString() + " is calling.")
                .setAutoCancel(true)
                .setOngoing(true)
                .setExtras(extras)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setGroup("test_app_notification")
                .addAction(
                    android.R.drawable.ic_menu_delete,
                    getString(R.string.decline),
                    piRejectIntent
                )
                .addAction(
                    android.R.drawable.ic_menu_call,
                    getString(R.string.answer),
                    piAcceptIntent
                )
                .setColor(Color.rgb(214, 10, 37)).build()
        }
    }

    /**
     * Build a notification.
     *
     * @param text          the text of the notification
     * @param pendingIntent the body, pending intent for the notification
     * @param extras        extras passed with the notification
     * @return the builder
     */
    @TargetApi(Build.VERSION_CODES.O)
    private fun buildNotification(
        text: String,
        pendingIntent: PendingIntent,
        extras: Bundle,
        channelId: String,
        piRejectIntent: PendingIntent,
        piAcceptIntent: PendingIntent
    ): Notification {
        val builder =
            Notification.Builder(applicationContext, channelId)
                .setSmallIcon(R.drawable.ic_call_end_white_24dp)
                .setContentTitle(getString(R.string.app_name))
                .setContentText(text)
                .setCategory(Notification.CATEGORY_CALL)
                .setExtras(extras)
                .setAutoCancel(true)
                .addAction(
                    android.R.drawable.ic_menu_delete,
                    getString(R.string.decline),
                    piRejectIntent
                )
                .addAction(
                    android.R.drawable.ic_menu_call,
                    getString(R.string.answer),
                    piAcceptIntent
                )
        //.setFullScreenIntent(pendingIntent, true)
        return builder.build()
    }

    @TargetApi(Build.VERSION_CODES.O)
    private fun createChannel(channelImportance: Int): String {
        var callInviteChannel = NotificationChannel(
            Constants.VOICE_CHANNEL_HIGH_IMPORTANCE,
            "Primary Voice Channel", NotificationManager.IMPORTANCE_HIGH
        )
        var channelId =
            Constants.VOICE_CHANNEL_HIGH_IMPORTANCE
        if (channelImportance == NotificationManager.IMPORTANCE_LOW) {
            callInviteChannel = NotificationChannel(
                Constants.VOICE_CHANNEL_LOW_IMPORTANCE,
                "Primary Voice Channel", NotificationManager.IMPORTANCE_LOW
            )
            channelId = Constants.VOICE_CHANNEL_LOW_IMPORTANCE
        }
        callInviteChannel.lightColor = Color.GREEN
        callInviteChannel.lockscreenVisibility = Notification.VISIBILITY_PRIVATE
        val notificationManager =
            getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.createNotificationChannel(callInviteChannel)
        return channelId
    }


    private fun handleCancelledCall(intent: Intent) {
        endForeground()
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent)
    }


    override fun onDestroy() {
        endForeground()
    }

    private fun handleIncomingCall(
        callInvite: CallRequestResponse.Data.Call?,
        notificationId: Int
    ) {
//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
//            setCallInProgressNotification(callInvite, notificationId)
//        }
       // Log.d( "call-invitess: ", Gson().toJson(callInvite))

        setCallInProgressNotification(callInvite, notificationId)
        // sendCallInviteToActivity(callInvite, notificationId);
    }

    private fun endForeground() {
        updateStatus(CallStatus.NONE, null)
        room?.state?.let {
            if (it != Room.State.DISCONNECTED) {
                room?.disconnect()

            }
        }

        if ((applicationContext as VadifyApplication).remoteParticipantAudio.isNullOrEmpty()) {
            localAudioTrack?.let {
                it.release()
                localAudioTrack = null
            }

            (applicationContext as VadifyApplication).remoteParticipantAudio.clear()
            (applicationContext as VadifyApplication).updateVideoCallStatus(
                CallStatus.NONE,
                null,
                null
            )
            (applicationContext as VadifyApplication).videoCallType =
                VadifyApplication.VideoCallType.None
            stopForeground(true)
            this.stopSelf()
        }
    }


    @TargetApi(Build.VERSION_CODES.O)
    private fun setCallInProgressNotification(
        callInvite: CallRequestResponse.Data.Call?,
        notificationId: Int
    ) {
        startForeground(
            notificationId,
            createNotification(callInvite, notificationId, NotificationManager.IMPORTANCE_LOW)
        )
//        if (isAppVisible) {
//            startForeground(notificationId, createNotification(callInvite, notificationId, NotificationManager.IMPORTANCE_HIGH));
//        } else {
//            Log.i(TAG, "setCallInProgressNotification - app is NOT visible.");
//            startForeground(notificationId, createNotification(callInvite, notificationId, NotificationManager.IMPORTANCE_HIGH));
//        }
    }

    /*
     * Send the CallInvite to the VoiceActivity. Start the activity if it is not running already.
     */
    private fun sendCallInviteToActivity(
        id : String,
        notificationId: Int,
        callInvite: List<CallRequestResponse.Data.Call.Member>?
    ) {
        Log.d( "call-invitess: ", "callingggg2")
        // if (Build.VERSION.SDK_INT >= 29 && !isAppVisible) { return }
        val memberIds = callInvite?.map { it.name + "," + it.userId }
        val intent = Intent(this, CallActivity::class.java)
        intent.putStringArrayListExtra(ChatActivity.MEMBER_IDs, memberIds as ArrayList<String>)
        intent.action = Constants.ACTION_INCOMING_CALL
        intent.putExtra(Constants.INCOMING_CALL_NOTIFICATION_ID, notificationId)
        intent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
        intent.putExtra(Constants.SENDER_OR_RECEIVER, CallType.RECEIVER_VISIT_AGIAN.value)
        intent.putExtra(Constants.CALL_ID,id)
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

        this.startActivity(intent)
    }

    private val isAppVisible: Boolean
        get() = ProcessLifecycleOwner
            .get()
            .lifecycle
            .currentState
            .isAtLeast(Lifecycle.State.STARTED)

    companion object {
        private val TAG = CallNotificationService::class.java.simpleName
    }


    @SuppressLint("SetTextI18n")
    private fun roomListener(): Room.Listener {
        return object : Room.Listener {
            override fun onConnected(room: Room) {
                updateStatus(CallStatus.CONNECTED, room)

            }

            override fun onConnectFailure(room: Room, e: TwilioException) {
                Log.e("messagea are", "" + e?.message.toString())
                updateStatus(CallStatus.FAILED, room)
                endForeground()
            }

            override fun onDisconnected(room1: Room, e: TwilioException?) {
                updateStatus(CallStatus.DISCONNECTED, room)
                //  endForeground()
                Log.e("messagea are", "" + e?.message.toString())
            }

            override fun onParticipantConnected(room: Room, remoteParticipant: RemoteParticipant) {
                updateStatus(CallStatus.PARTICIPATE_CONNECTED, room, remoteParticipant)
                timer?.cancel()
            }

            override fun onParticipantDisconnected(
                room: Room,
                remoteParticipant: RemoteParticipant
            ) {
                updateStatus(CallStatus.PARTICIPATE_DISCONNECTED, room, remoteParticipant)
                // endForeground()
            }

            override fun onRecordingStarted(room: Room) {
                /*
                 * Indicates when media shared to a Room is being recorded. Note that
                 * recording is only available in our Group Rooms developer preview.
                 */
            }

            override fun onRecordingStopped(room: Room) {
                /*
                 * Indicates when media shared to a Room is no longer being recorded. Note that
                 * recording is only available in our Group Rooms developer preview.
                 */
            }

            override fun onReconnecting(room: Room, exception: TwilioException) {

            }

            override fun onReconnected(room: Room) {

            }
        }
    }


    private fun senderNotification(
        callLogerId: String,
        notificationId: Int,
        channelImportance: Int
    ): Notification {
        val intent = Intent(this, CallActivity::class.java)
        intent.putExtra(Constants.SENDER_OR_RECEIVER, CallType.RECEIVER_VISIT_AGIAN.value)
        val pendingIntent = PendingIntent.getActivity(
            this,
            notificationId,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT
        )
        val extras = Bundle()
        val rejectIntent = Intent(applicationContext, CallNotificationService::class.java)
        rejectIntent.action = Constants.ACTION_REJECT
        rejectIntent.putExtra(Constants.CALL_LOGER_ID, callLogerId)
        rejectIntent.putExtra(Constants.INCOMING_CALL_NOTIFICATION_ID, notificationId)
        val piRejectIntent = PendingIntent.getService(
            applicationContext,
            0,
            rejectIntent,
            PendingIntent.FLAG_UPDATE_CURRENT
        )
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            buildSendNotification(
                "Audio call...",
                pendingIntent,
                extras,
                createChannel(channelImportance),
                piRejectIntent
            )
        } else {
            NotificationCompat.Builder(this)
                .setSmallIcon(R.drawable.ic_call_end_white_24dp)
                .setContentTitle(getString(R.string.app_name))
                .setContentText("Audio call...")
                .setAutoCancel(true)
                .setOngoing(true)
                .setExtras(extras)
                .setGroup("test_app_notification")
                .addAction(
                    android.R.drawable.ic_menu_delete,
                    getString(R.string.decline),
                    piRejectIntent
                )
                .setColor(Color.rgb(214, 10, 37)).build()
        }
    }


    @TargetApi(Build.VERSION_CODES.O)
    private fun buildSendNotification(
        text: String,
        pendingIntent: PendingIntent,
        extras: Bundle,
        channelId: String,
        piRejectIntent: PendingIntent
    ): Notification {
        val builder =
            Notification.Builder(applicationContext, channelId)
                .setSmallIcon(R.drawable.ic_call_end_white_24dp)
                .setContentTitle(getString(R.string.app_name))
                .setContentText(text)
                .setCategory(Notification.CATEGORY_CALL)
                .setExtras(extras)
                .setAutoCancel(true)
                .addAction(
                    android.R.drawable.ic_menu_delete,
                    getString(R.string.decline),
                    piRejectIntent
                )
                .setFullScreenIntent(pendingIntent, true)
        return builder.build()
    }


//    @SuppressLint("CheckResult")
//     fun receivingAudioCallMethod(_id: String) {
//        startForeground(notificationId, senderNotification(notificationId,NotificationManager.IMPORTANCE_LOW))
//        sendCallInviteToActivity(notificationId)
//        userRepository.callTokenSingle().subscribeOn(Schedulers.io()).
//            observeOn(AndroidSchedulers.mainThread())
//            .subscribeBy(onSuccess = { if (it.isSuccessful) {
//                it.body()?.let {
//                    videoCallRoomConnect(it.data,_id)
//                } } else { endForeground() } },
//                onError = { endForeground() }
//            )
//    }

    @SuppressLint("CheckResult")
    private fun receivingAudioCallMethod(
        _id: String,
        status: String,
        callInvite: List<CallRequestResponse.Data.Call.Member>?
    ) {
        startForeground(
            notificationId,
            senderNotification(_id, notificationId, NotificationManager.IMPORTANCE_LOW)
        )
        if (status.equals(ACCEPT, ignoreCase = true)) sendCallInviteToActivity(
            _id,
            notificationId,
            callInvite
        )
        userRepository.callTokenUpdateSingle(_id, CallStatusRequest(status))
            .subscribeOn(Schedulers.io()).observeOn(AndroidSchedulers.mainThread()).subscribeBy(
                onSuccess = {
                    if (it.isSuccessful) {
                        when (status) {
                            ACCEPT -> {
                                it.body()?.let { videoCallRoomConnect(it.data as String, _id) }
                            }
                            else -> {
                                (applicationContext as VadifyApplication).remoteParticipantAudio.clear()
                                endForeground()
                            }
                        }
                    } else {
                        (applicationContext as VadifyApplication).remoteParticipantAudio.clear()
                        endForeground()
                    }
                },
                onError = {
                    (applicationContext as VadifyApplication).remoteParticipantAudio.clear()
                    endForeground()
                })
    }


    private fun updateStatus(
        callStatus: CallStatus,
        room: Room?,
        remoteParticipant: RemoteParticipant? = null
    ) {
        this.checkStatus = callStatus
        (applicationContext as VadifyApplication).updateVideoCallStatus(
            callStatus,
            room,
            remoteParticipant
        )
        //  preferenceService.putString(R.string.pkey_call_status,status)
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
                    if ((isIncomingCall && callStatus == CallStatus.CONNECTED) || callStatus == CallStatus.PARTICIPATE_CONNECTED) {
                        Log.e("check", "message")
                    } else {
                        updatestatus(callLogId, END)
                    }
                }
            }
            timer?.start()
        }
    }

    @SuppressLint("CheckResult")
    fun updatestatus(_id: String, status: String) {
        userRepository.callTokenUpdateSingle(_id, CallStatusRequest(status))
            .subscribeOn(Schedulers.io()).observeOn(AndroidSchedulers.mainThread()).subscribeBy(
                onSuccess = {
                    updateStatus(CallStatus.FAILED, null)
                    endForeground()
                },
                onError = {
                    updateStatus(CallStatus.FAILED, null)
                    endForeground()
                })
    }

    private fun notifyForGround(intent: Intent, dpBitmap: Bitmap?) {

        Log.d( "call-invitess: ", "callingggg")
        val callInvite: CallRequestResponse.Data.Call? =
            intent.getParcelableExtra(Constants.INCOMING_CALL_INVITE)
        val callerName = callInvite?.from?.name
        preferenceService.putString(R.string.pkey_receiver_name_call, callerName)
        val piIntent = Intent(this, CallActivity::class.java)
        piIntent.putExtras(intent)
        val pendingIntent =
            PendingIntent.getActivity(this, 0, piIntent, PendingIntent.FLAG_CANCEL_CURRENT)
        val intent1 = Intent(this, CallNotificationService::class.java)
        if (callInvite?.type == "Group")
            (applicationContext as VadifyApplication).videoCallType =
                VadifyApplication.VideoCallType.GroupCall
        else
            (applicationContext as VadifyApplication).videoCallType =
                VadifyApplication.VideoCallType.SingleCall
        intent1.putExtras(intent)
        intent1.action = Constants.ACTION_ACCEPT
        intent1.putExtra(Constants.IS_INCOMING_CALL, true)

        intent.extras?.getParcelable<CallRequestResponse.Data.Call>(Constants.INCOMING_CALL_INVITE)
            ?.let { userdata ->
                intent1.putExtra(
                    Constants.INCOMING_CALL_NOTIFICATION_ID,
                    intent.extras?.getInt(Constants.INCOMING_CALL_NOTIFICATION_ID)
                )
                intent1.putExtra(Constants.INCOMING_CALL_INVITE, userdata)



            }

        val acceptPendingIntent =
            PendingIntent.getService(this, 10, intent1, PendingIntent.FLAG_CANCEL_CURRENT)

        val intent2 = Intent(this, CallNotificationService::class.java)
        intent2.putExtras(intent)
        intent2.action = Constants.ACTION_REJECT
        intent2.putExtra(Constants.IS_INCOMING_CALL, true)

        intent.extras?.getParcelable<CallRequestResponse.Data.Call>(Constants.INCOMING_CALL_INVITE)
            ?.let { userdata ->
                intent2.putExtra(
                    Constants.INCOMING_CALL_NOTIFICATION_ID,
                    intent.extras?.getInt(Constants.INCOMING_CALL_NOTIFICATION_ID)
                )
                intent2.putExtra(Constants.INCOMING_CALL_INVITE, userdata)
                intent2.putExtra(Constants.CALL_LOGER_ID, userdata._id)
            }
        val rejectPendingIntent =
            PendingIntent.getService(this, 20, intent2, PendingIntent.FLAG_CANCEL_CURRENT)
        val customView = RemoteViews(this.packageName, R.layout.custom_call_notification)
        customView.setTextViewText(R.id.callType, "Incoming audio call")
        customView.setTextViewText(R.id.name, callerName)
        customView.setOnClickPendingIntent(R.id.btnAnswer, acceptPendingIntent)
        customView.setOnClickPendingIntent(R.id.btnDecline, rejectPendingIntent)
        if (dpBitmap != null)
            customView.setImageViewBitmap(R.id.notificationDp, dpBitmap)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager =
                getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            val notificationChannel = NotificationChannel(
                "Incomingcall",
                "Incomingcall",
                NotificationManager.IMPORTANCE_HIGH
            )
            notificationChannel.setSound(null, null)
            notificationChannel.lockscreenVisibility = Notification.VISIBILITY_PUBLIC
            notificationChannel.setShowBadge(true)
            notificationManager.createNotificationChannel(notificationChannel)
            val notification = NotificationCompat.Builder(this, "Incomingcall")
            notification.setContentTitle(callerName)
            notification.setTicker("Call_STATUS")
            notification.setContentText("Incoming call")
            notification.setSmallIcon(R.drawable.logo)
            notification.setDefaults(Notification.DEFAULT_LIGHTS or Notification.DEFAULT_SOUND)
            notification.setCategory(NotificationCompat.CATEGORY_CALL)
            notification.setVibrate(null)
            notification.setOngoing(true)
            notification.setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            notification.setFullScreenIntent(pendingIntent, true)
            notification.priority = NotificationCompat.PRIORITY_HIGH
            notification.setStyle(NotificationCompat.DecoratedCustomViewStyle())
            notification.setCustomContentView(customView)
            notification.setCustomBigContentView(customView)
            startForeground(1124, notification.build())
        } else {
            val notification = NotificationCompat.Builder(this)
            notification.setContentTitle(callerName)
            notification.setTicker("call_STATUS")
            notification.setContentText("Incoming call")
            notification.setSmallIcon(R.drawable.logo)
            notification.setDefaults(Notification.DEFAULT_LIGHTS or Notification.DEFAULT_SOUND)
            notification.setVibrate(null)
            notification.setFullScreenIntent(pendingIntent, true)
            notification.setOngoing(true)
            notification.setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            notification.setCategory(NotificationCompat.CATEGORY_CALL)
            notification.priority = NotificationCompat.PRIORITY_HIGH
            startForeground(1124, notification.build())
        }
    }

    private fun showNotificationWithBitmap(intent: Intent) {
        val callInvite: CallRequestResponse.Data.Call? =
            intent.getParcelableExtra(Constants.INCOMING_CALL_INVITE)
        val callerDp = callInvite?.from?.profileImage
        CoroutineScope(Job() + Dispatchers.IO).launch {
            try {
                val url = URL(callerDp)
                val bitMap = BitmapFactory.decodeStream(url.openConnection().getInputStream())
                val dpBitmap = Bitmap.createScaledBitmap(bitMap, 100, 100, true)
                notifyForGround(intent, dpBitmap)
            } catch (e: IOException) {
                notifyForGround(intent, null)
            }
        }
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
}