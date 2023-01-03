package com.android.vadify.service

import android.annotation.TargetApi
import android.app.*
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ProcessLifecycleOwner
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.android.vadify.R
import com.android.vadify.data.api.models.CallRequestResponse
import com.android.vadify.ui.chat.call.CallActivity
import com.android.vadify.ui.chat.call.videocall.VideoCallActivity

class IncomingCallNotificationService : Service() {


    override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {
        val action = intent.action
        if (action != null) {
            val callInvite: CallRequestResponse.Data.Call? =
                intent.getParcelableExtra(Constants.INCOMING_CALL_INVITE)
            val notificationId = intent.getIntExtra(Constants.INCOMING_CALL_NOTIFICATION_ID, 0)
            when (action) {
                Constants.ACTION_INCOMING_CALL -> handleIncomingCall(callInvite, notificationId)
                Constants.ACTION_ACCEPT -> accept(callInvite, notificationId)
                Constants.ACTION_REJECT -> reject(callInvite)
                Constants.ACTION_CANCEL_CALL -> handleCancelledCall(intent)
                else -> {
                }
            }
        }
        return START_NOT_STICKY
    }

    override fun onBind(intent: Intent): IBinder? {
        return null
    }

    private fun createNotification(
        callInvite: CallRequestResponse.Data.Call?, notificationId: Int, channelImportance: Int
    ): Notification {
        val intent = Intent(this, VideoCallActivity::class.java)
        intent.action = Constants.ACTION_INCOMING_CALL_NOTIFICATION
        intent.putExtra(Constants.INCOMING_CALL_NOTIFICATION_ID, notificationId)
        intent.putExtra(Constants.INCOMING_CALL_INVITE, callInvite)
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
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

        val rejectIntent = Intent(applicationContext, IncomingCallNotificationService::class.java)
        rejectIntent.action = Constants.ACTION_REJECT
        rejectIntent.putExtra(Constants.INCOMING_CALL_INVITE, callInvite)
        rejectIntent.putExtra(Constants.INCOMING_CALL_NOTIFICATION_ID, notificationId)

        val piRejectIntent = PendingIntent.getService(
            applicationContext,
            0,
            rejectIntent,
            PendingIntent.FLAG_UPDATE_CURRENT
        )
        val acceptIntent = Intent(applicationContext, IncomingCallNotificationService::class.java)
        acceptIntent.action = Constants.ACTION_ACCEPT
        acceptIntent.putExtra(Constants.INCOMING_CALL_INVITE, callInvite)
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
                .setExtras(extras)
                .setOngoing(true)
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
                .setFullScreenIntent(pendingIntent, true)
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

    private fun accept(callInvite: CallRequestResponse.Data.Call?, notificationId: Int) {
        endForeground()
        val activeCallIntent = Intent(this, CallActivity::class.java)
        activeCallIntent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
        activeCallIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        activeCallIntent.putExtra(Constants.INCOMING_CALL_INVITE, callInvite)
        activeCallIntent.putExtra(Constants.INCOMING_CALL_NOTIFICATION_ID, notificationId)
        activeCallIntent.action = Constants.ACTION_ACCEPT
        startActivity(activeCallIntent)
    }

    private fun reject(callInvite: CallRequestResponse.Data.Call?) {
        endForeground()
        //callInvite.reject(getApplicationContext());
    }

    private fun handleCancelledCall(intent: Intent) {
        endForeground()
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent)
    }

    private fun handleIncomingCall(
        callInvite: CallRequestResponse.Data.Call?,
        notificationId: Int
    ) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            setCallInProgressNotification(callInvite, notificationId)
        }
        sendCallInviteToActivity(callInvite, notificationId);
    }

    private fun endForeground() {
        stopForeground(true)
    }


    @TargetApi(Build.VERSION_CODES.O)
    private fun setCallInProgressNotification(
        callInvite: CallRequestResponse.Data.Call?,
        notificationId: Int
    ) {
        startForeground(
            notificationId,
            createNotification(callInvite, notificationId, NotificationManager.IMPORTANCE_HIGH)
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
        callInvite: CallRequestResponse.Data.Call?,
        notificationId: Int
    ) {
        if (Build.VERSION.SDK_INT >= 29 && !isAppVisible) {
            return
        }
        val intent = Intent(this, VideoCallActivity::class.java)
        intent.action = Constants.ACTION_INCOMING_CALL
        intent.putExtra(Constants.INCOMING_CALL_NOTIFICATION_ID, notificationId)
        intent.putExtra(Constants.INCOMING_CALL_INVITE, callInvite)
        intent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
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
        private val TAG = IncomingCallNotificationService::class.java.simpleName
    }

    private fun filterClassMethod(callInvite: CallRequestResponse.Data.Call?): Class<*> {
        return if (callInvite?.mode?.equals(
                "Audio",
                ignoreCase = true
            ) == true
        ) CallActivity::class.java else VideoCallActivity::class.java

    }
}