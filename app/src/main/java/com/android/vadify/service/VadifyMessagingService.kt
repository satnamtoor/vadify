package com.android.vadify.service

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.util.Log
import com.android.vadify.R
import com.android.vadify.data.api.enums.CallType
import com.android.vadify.data.api.models.CallRequestResponse
import com.android.vadify.data.api.models.MessageResponseList
import com.android.vadify.data.db.chat.ChatListCache
import com.android.vadify.data.service.PreferenceService
import com.android.vadify.ui.SplashActivity
import com.android.vadify.ui.chat.ChatActivity
import com.android.vadify.ui.chat.call.CallActivity
import com.android.vadify.ui.chat.call.videocall.VideoCallActivity
import com.android.vadify.ui.chat.call.viewmodel.CallViewModel
import com.android.vadify.utils.RxBus
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.google.gson.Gson
import dagger.android.AndroidInjection
import org.json.JSONObject
import org.threeten.bp.Instant
import java.util.ArrayList
import javax.inject.Inject

class VadifyMessagingService : FirebaseMessagingService() {

    @Inject
    lateinit var preferenceService: PreferenceService

    @Inject
    lateinit var chatListCache: ChatListCache

    @Inject
    lateinit var badgeService: BadgeService

    @Inject
    lateinit var callViewModel: CallViewModel


    override fun onCreate() {
        super.onCreate()
        AndroidInjection.inject(this)
    }

    /**
     * Called when message is received.
     *
     * @param remoteMessage Object representing the message received from Firebase Cloud Messaging.
     */
    // [START receive_message]
    override fun onMessageReceived(remoteMessage: RemoteMessage) {
         //Log.e("message--noti", Gson().toJson(remoteMessage))
        try {
            RxBus.publish("New Message")
            remoteMessage.data[DATA]?.let {
                when {
                    JSONObject(it).has(MODE) -> {
                        val notificationId = System.currentTimeMillis().toInt()
                        handleInvite(remoteMessage.data, notificationId)
                    }
                    else -> {
                        Gson().fromJson(
                            remoteMessage.data[DATA],
                            MessageResponseList.Data.DataX::class.java
                        ).let {
                            val senderNum = Gson().toJson(it.from?.number).replace("\"", "")
                            val condition = preferenceService.getPhoneNumber(
                                this.resources.getString(R.string.pkey_phone_number),
                                ""
                            )
                            //   Log.d("onMessageReceived: ", "$senderNum  ::  $condition")
                            if (!condition.equals(senderNum)) {
                                MessageHandling(
                                    applicationContext,
                                    preferenceService
                                ).callNotification(
                                    remoteMessage.data,
                                    chatListCache
                                )
                                //  badgeService.refresh()
                            }
                        }
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun onNewToken(token: String) {
        Log.d(TAG, "Refreshed token: $token")
        preferenceService.putString(R.string.pkey_device_token, token)
    }

    companion object {
        private const val TAG = "MyFirebaseMsgService"
        const val BODY = "body"
        const val TITLE = "title"
        const val FROM = "from"
        const val DATA = "data"


        const val SEND_FILE = "Send file"
        const val MODE = "mode"
        const val KEY = "pkey_phone_number"
        const val UserKey = "pkey_user_Id"

    }


    private fun handleInvite(data: MutableMap<String, String>, notificationId: Int) {
        Gson().fromJson(data[DATA], CallRequestResponse.Data.Call::class.java)?.let { userdata ->



            if (JSONObject(data[DATA]).getJSONObject("roomId").has("name"))
                userdata.from.name = JSONObject(data[DATA]).getJSONObject("roomId").getString("name")
            // Log.d("handleInvite ", "" + Gson().toJson(data))
            when {
                preferenceService.getFireBaseString(UserKey).isNullOrBlank() -> {
                    Intent(applicationContext, SplashActivity::class.java).also {
                        it.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
                    }
                }


                else -> when {
                    userdata.status != "initiated" -> {
                        val notificationManager =
                            getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                        notificationManager.cancelAll()
                        stopService(Intent(this, VideoCallNotificationService::class.java))
                        stopService(Intent(this, CallNotificationService::class.java))
                        if (android.os.Build.VERSION.SDK_INT > android.os.Build.VERSION_CODES.O) {
                            if (MessageHandling.mediaPlayer != null) {
                                if (MessageHandling.mediaPlayer!!.isPlaying) {
                                    MessageHandling.mediaPlayer?.stop()
                                    MessageHandling.viberator?.cancel()
                                }
                            }
                        }
                        val intent = Intent(
                            applicationContext,
                            SplashActivity::class.java
                        ).also { it.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP) }
                        val pendingIntent = PendingIntent.getActivity(
                            applicationContext,
                            0,
                            intent,
                            PendingIntent.FLAG_CANCEL_CURRENT
                        )
                        MessageHandling(
                            applicationContext,
                            preferenceService
                        ).missedCallNotify(userdata.from.name,userdata.from.number,userdata.mode,pendingIntent)
                    }
                    //data[VadifyMessagingService.TITLE]!!, info!!, data[VadifyMessagingService.FROM]!!

                    userdata.mode.equals("audio", ignoreCase = true) -> {

                        if (android.os.Build.VERSION.SDK_INT > android.os.Build.VERSION_CODES.O) {
                            val uniqueId = Instant.now().epochSecond.toInt()
                            MessageHandling(
                                applicationContext,
                                preferenceService
                            ).callNotification(
                                uniqueId,
                                "audio", data[TITLE]!!, navigateToActivity(
                                    uniqueId,
                                    Intent(
                                        applicationContext,
                                        CallNotificationService::class.java
                                    ), notificationId, userdata
                                ),this                            )
                        } else {
                            navigateToActivity(
                                0,
                                Intent(
                                    applicationContext,
                                    CallActivity::class.java
                                ), notificationId, userdata
                            )
                        }
                    }

                    else ->{
                        if (android.os.Build.VERSION.SDK_INT > android.os.Build.VERSION_CODES.O) {
                            val uniqueId = Instant.now().epochSecond.toInt()
                            MessageHandling(
                                applicationContext,
                                preferenceService
                            ).callNotification(
                                uniqueId,
                                "video", data[TITLE]!!, navigateToActivity(
                                    uniqueId,
                                    Intent(
                                        applicationContext,
                                        VideoCallNotificationService::class.java
                                    ), notificationId, userdata
                                ),this
                            )
                        } else {
                            navigateToActivity(
                                0,
                                Intent(
                                    applicationContext,
                                    VideoCallActivity::class.java
                                ), notificationId, userdata
                            )
                        }
                    }
                }
            }
        }
    }

    private fun navigateToActivity(
        notificationId1: Int,
        intent: Intent,
        notificationId: Int,
        userdata: CallRequestResponse.Data.Call
    ): Intent {
        intent.putExtra(Constants.INCOMING_CALL_NOTIFICATION_ID_1, notificationId1)
        intent.putExtra(Constants.INCOMING_CALL_NOTIFICATION_ID, notificationId)
        intent.putExtra(Constants.INCOMING_CALL_INVITE, userdata)
        val memberIds=userdata.members.map { it.name+","+it.userId }
        intent.putStringArrayListExtra(ChatActivity.MEMBER_IDs,memberIds as ArrayList<String>)
        intent.putExtra(Constants.IS_INCOMING_CALL, true)
        intent.putExtra(Constants.SENDER_OR_RECEIVER, CallType.RECEIVER_VISIT_AGIAN.value)
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        if (android.os.Build.VERSION.SDK_INT <= android.os.Build.VERSION_CODES.O) {
            startActivity(intent)
        }
        return intent
    }


}
