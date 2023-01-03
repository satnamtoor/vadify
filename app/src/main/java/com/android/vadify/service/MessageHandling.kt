package com.android.vadify.service

import android.app.Notification
import android.app.Notification.DEFAULT_SOUND
import android.app.Notification.DEFAULT_VIBRATE
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.os.Build
import android.os.Vibrator
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.Person
import androidx.core.graphics.drawable.IconCompat
import com.android.vadify.R
import com.android.vadify.VadifyApplication
import com.android.vadify.data.api.models.MessageResponseList
import com.android.vadify.data.db.chat.Chat
import com.android.vadify.data.db.chat.ChatListCache
import com.android.vadify.data.service.PreferenceService
import com.android.vadify.ui.SplashActivity
import com.android.vadify.ui.baseclass.BaseBackStack
import com.android.vadify.ui.chat.ChatActivity
import com.android.vadify.viewmodels.EncryptionViewModel
import com.google.gson.Gson
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch


class MessageHandling(
    private val context: Context,
    private val preferenceService: PreferenceService
) {
    companion object {
        var viberator: Vibrator? = null
        var mediaPlayer: MediaPlayer? = null
        fun initRingtone(context: Context) {


            viberator =
                (context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator).also {
                    it.vibrate(
                        500
                    )
                }
            //if(viewModel.isSoundEnable()){
            mediaPlayer = MediaPlayer.create(context, R.raw.music)
            mediaPlayer?.start()
            //  }
        }
    }

    fun callNotification(data: MutableMap<String, String>, chatListCache: ChatListCache) {
        //Log.d("login-id",preferenceService.getString(R.string.pkey_user_Id)!! )
        val intent = when {

            preferenceService.getString(R.string.pkey_user_Id).equals("") -> {
                Intent(
                    context,
                    SplashActivity::class.java
                ).also { it.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP) }
            }
            else -> {


                Gson().fromJson(
                    data[VadifyMessagingService.DATA],
                    MessageResponseList.Data.DataX::class.java
                )?.let { userdata ->
                    CoroutineScope(Dispatchers.IO).launch {
                        userdata.members.forEach {
                            it.message = EncryptionViewModel.encryptString(it.message)
                        }
                        chatListCache.insert(Chat.Mapper.from(userdata))
                    }

                    Intent(context, ChatActivity::class.java).also {
                        it.putExtra(BaseBackStack.ANOTHER_USER_ID, userdata.from?._id)
                      //  it.putExtra(BaseBackStack.ANOTHER_USER_NAME, userdata.from.name)
                       // it.putExtra(BaseBackStack.ANOTHER_USER_URL, userdata.from.profileImage)
                        it.putExtra(BaseBackStack.ROOM_ID, userdata.roomId)
                        it.putExtra(BaseBackStack.MESSAGE_ID, userdata._id)
                        it.putExtra(BaseBackStack.LANGUAGE_SWITCH, true)
                        it.putExtra(BaseBackStack.IMAGE_PATH, "")
                        it.putExtra(BaseBackStack.GOTO_SPEECH_TO_TEXT, false)
                        it.putExtra(BaseBackStack.MOTHER_LANGUAGE, userdata.from?.language)
                        //it.putExtra(BaseBackStack.PHONE_NUMBER, userdata.from.number)

                        it.putExtra(BaseBackStack.ANOTHER_USER_URL, userdata.from?.profileImage)

                        if (userdata.chatType == "Group") {
                            it.putExtra(BaseBackStack.TYPE, true)
                            it.putExtra(BaseBackStack.PHONE_NUMBER, "")
                            it.putExtra(BaseBackStack.ANOTHER_USER_NAME, data[VadifyMessagingService.TITLE]!!)


                        } else {
                            it.putExtra(BaseBackStack.PHONE_NUMBER, userdata.from?.number)
                            it.putExtra(BaseBackStack.ANOTHER_USER_NAME,userdata.from?.name)
                            it.putExtra(BaseBackStack.TYPE, false)
                        }

                    }
                }
            }
        }

        val isShowNotification = preferenceService.getBoolean(R.string.pkey_messageNoficaction)
        if (isShowNotification) {
            Gson().fromJson(
                data[VadifyMessagingService.DATA],
                MessageResponseList.Data.DataX::class.java
            ).let {
                /*Log.d(
                    "callNotification: ",
                    " title" +
                            + " message" + info!!)*/
                val info =
                    if (data[VadifyMessagingService.BODY].isNullOrBlank()) it.type+" received" else data[VadifyMessagingService.BODY]

                val length = it.from?.number!!.length
                val uniqueId = it.from?.number!!.substring(length - 5)
                val groupName  = if (it.chatType.equals("Group")) {
                    it.from.name +"@"+ data[VadifyMessagingService.TITLE]!!
                }
                else{
                    data[VadifyMessagingService.TITLE]

                }

                sendNotification(
                    groupName!!, info!!, it.from?.number,
                    PendingIntent.getActivity(
                        context,
                        uniqueId.toInt(),
                        intent,
                        PendingIntent.FLAG_UPDATE_CURRENT
                    )
                )
            }


        }
    }


    fun sendNotification(
        title: String,
        message: String,
        from: String,
        pendingIntent: PendingIntent
    ) {
        //Log.d( "sendNotification: ",messageBody)
        val channelId = System.currentTimeMillis().toString()
        val defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
        val viberation = longArrayOf(0, 250, 100, 250)
        val isSoundEnable = preferenceService.getBoolean(R.string.pkey_inAppSound)
        val isViberationEnable = preferenceService.getBoolean(R.string.pkey_inAppVibrate)
        val message = MessageResponse(message, System.currentTimeMillis(), from)

        VadifyApplication.addMessage(from, message)

        val user: Person =
            Person.Builder().setIcon(IconCompat.createWithResource(context, R.drawable.logo))
                .setName(title).build()
        val messagingStyle: NotificationCompat.MessagingStyle =
            NotificationCompat.MessagingStyle(user)
        for (chatMessage in VadifyApplication.messageList.get(from)!!) {
            val notificationMessage =
                NotificationCompat.MessagingStyle.Message(
                    chatMessage.message,
                    chatMessage.timestamp,
                    user
                )
            messagingStyle.addMessage(notificationMessage)
        }

        val notificationBuilder = NotificationCompat.Builder(context, channelId).also {
            it.setSmallIcon(R.drawable.logo)
            it.setStyle(messagingStyle)
            it.setContentTitle(title)
            it.setAutoCancel(true)

            it.setContentIntent(pendingIntent)
            it.setDefaults(DEFAULT_SOUND or DEFAULT_VIBRATE)
            it.setPriority(NotificationCompat.PRIORITY_HIGH)
            //   it.setCategory(NotificationCompat.CATEGORY_CALL)
            // it.setFullScreenIntent(pendingIntent, true)

            if (isSoundEnable) it.setSound(defaultSoundUri)
            if (isViberationEnable) it.setVibrate(viberation)


        }
        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Channel human readable title",
                NotificationManager.IMPORTANCE_HIGH
            )
            channel.lockscreenVisibility = Notification.VISIBILITY_PUBLIC
            channel.setShowBadge(true)
            channel.enableVibration(isViberationEnable)
            channel.vibrationPattern = if (isViberationEnable) viberation else null
            channel.setSound(if (isSoundEnable) defaultSoundUri else null, null)
            notificationManager.createNotificationChannel(channel)
        }
        //val uniqueId = Instant.now().epochSecond.toInt()
        val length = from.length
        val uniqueId = from.substring(length - 5)
        // startForeground(notificationId, notification)

        notificationManager.notify(uniqueId.toInt(), notificationBuilder.build())
    }


    fun callNotification(
        notificationId: Int,
        callingType: String,
        title: String,
        intent: Intent,
        context: VadifyMessagingService
    ) {
        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_CANCEL_CURRENT
        )
        //Log.d( "sendNotification: ",messageBody)
        val channelId = System.currentTimeMillis().toString()
        val isSoundEnable = preferenceService.getBoolean(R.string.pkey_inAppSound)
        val isViberationEnable = preferenceService.getBoolean(R.string.pkey_inAppVibrate)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(intent)
        } else {
            context.startService(intent)
        }


//        val notificationBuilder = NotificationCompat.Builder(context, channelId).also {
//            it.setSmallIcon(R.drawable.logo)
//            it.setContentTitle(title)
//            it.setAutoCancel(true)
//            it.setContentText("is $callingType Calling...")
//            it.setCategory(NotificationCompat.CATEGORY_ALARM)
//            it.priority = NotificationCompat.PRIORITY_HIGH
//
//            it.setFullScreenIntent(pendingIntent, true)
//
//        }
//        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
//            val channel = NotificationChannel(
//                channelId,
//                "Channel human readable title",
//                NotificationManager.IMPORTANCE_HIGH
//            )
//            channel.lockscreenVisibility = Notification.VISIBILITY_PUBLIC
//            channel.setShowBadge(true)
//            notificationManager.createNotificationChannel(channel)
//        }
//        notificationManager.notify(notificationId, notificationBuilder.build())
        if (isSoundEnable)
            initRingtone()
    }

    fun initRingtone() {
        viberator =
            (context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator).also { it.vibrate(500) }
        mediaPlayer = MediaPlayer.create(context, R.raw.music)
        mediaPlayer?.start()
    }

    fun missedCallNotify(
        caller: String,
        from: String,
        callType: String,
        pendingIntent: PendingIntent
    ) {

        val channelId = System.currentTimeMillis().toString()
        val defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
        val viberation = longArrayOf(0, 250, 100, 250)
        val isSoundEnable = preferenceService.getBoolean(R.string.pkey_inAppSound)
        val isViberationEnable = preferenceService.getBoolean(R.string.pkey_inAppVibrate)

        val notificationBuilder = NotificationCompat.Builder(context, channelId).also {
            it.setSmallIcon(R.drawable.logo)
            it.setContentText("$caller's $callType missed")
            it.setContentTitle(caller)
            it.setAutoCancel(true)
            it.setContentIntent(pendingIntent)
            it.setDefaults(DEFAULT_SOUND or DEFAULT_VIBRATE)
            it.priority = NotificationCompat.PRIORITY_HIGH
            if (isSoundEnable) it.setSound(defaultSoundUri)
            if (isViberationEnable) it.setVibrate(viberation)
        }
        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Channel human readable title",
                NotificationManager.IMPORTANCE_HIGH
            )
            channel.lockscreenVisibility = Notification.VISIBILITY_PUBLIC
            channel.setShowBadge(true)
            channel.enableVibration(isViberationEnable)
            channel.vibrationPattern = if (isViberationEnable) viberation else null
            channel.setSound(if (isSoundEnable) defaultSoundUri else null, null)
            notificationManager.createNotificationChannel(channel)
        }
        val length = from.length
        val uniqueId = from.substring(length - 5)
        notificationManager.notify(uniqueId.toInt(), notificationBuilder.build())
    }

}