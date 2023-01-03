package com.android.vadify.ui.util

import android.content.Context
import android.text.TextUtils
import android.util.Log
import android.util.Patterns
import androidx.preference.PreferenceManager
import com.android.vadify.R
import com.android.vadify.data.api.enums.CallLogStatus
import com.android.vadify.data.api.enums.MessageType
import com.android.vadify.data.db.chat.Chat
import com.android.vadify.data.db.chatThread.ChatThread
import com.android.vadify.utils.date
import com.android.vadify.utils.time
import com.google.cloud.translate.Translate
import com.google.cloud.translate.TranslateOptions
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.runBlocking
import java.io.File


const val AUDIO = "audio"

fun getAlphabetCharacter(name: String?): String? {
    return name?.replace("^\\s*([a-zA-Z]).*\\s+([a-zA-Z])\\S+$".toRegex(), "$1$2")
}

 fun isValidEmail(email: String): Boolean {
    return !TextUtils.isEmpty(email) && Patterns.EMAIL_ADDRESS.matcher(email).matches()
}

fun dateConvert(name: String?): String? {
    var timeCheck = ""
    try {
        timeCheck = time(name) ?: ""
    } catch (e: Exception) {
    }
    return timeCheck
}

fun timeConverter(name: String?): String? {
    var timeCheck = ""
    try {
        timeCheck = date(name) ?: ""
    } catch (e: Exception) {
    }
    return timeCheck
}

fun chatTimeConverter(name: String?): String? {
    var timeCheck = ""
    try {
        timeCheck = date(name) ?: ""
    } catch (e: Exception) {
    }
    return timeCheck
}

fun checkIsDataEmpty(isdateVisible: String?): Boolean {
    return !isdateVisible.isNullOrBlank()
}


fun getFileName(url: String?): String {
    var fileName = ""
    url?.let {
        fileName = File(it).name
    }
    return fileName
}

fun changeFormat(number: Int): String {
    return String.format("%02d:%02d", 0, number)
}


fun blockedUser(number: Int): String {
    return when (number) {
        0 -> "Blocked User Not Available"
        1 -> "Blocked [$number Contact]"
        else -> "Blocked [$number Contacts]"
    }
}

fun lastMessageType(type: String): Int {
    return when (type) {
        MessageType.VIDEO.value -> R.drawable.ic_video_call
        MessageType.AUDIO.value -> R.drawable.ic_audioplay
        MessageType.IMAGE.value -> R.drawable.ic_image_upload_single_chat
        MessageType.CONTACT.value -> R.drawable.ic_contact_btn
        MessageType.LOCATION.value -> R.drawable.ic_loc
        MessageType.TEXT.value -> R.drawable.ic_outgoing
        else -> R.drawable.icon_file_unknown
    }
    // android:text="@{item.lastMessageType == MessageType.TEXT.value? item.lastMessage : item.type }"

}

fun callStatus(type: String?): Int {
    var callStatus = R.drawable.ic_call_gray
    type?.let {
        callStatus = when (type) {
            MessageType.VIDEO.value -> R.drawable.ic_video_call_gray
            else -> R.drawable.ic_call_gray
        }
    }
    return callStatus
}


fun checkCallStatus(callStatus: String?, duration: Int, callFrom: String, id: String,mContext: Context): String? {


    var information = "unknown"
     val defaultSharedPreferences = PreferenceManager.getDefaultSharedPreferences(mContext)
 var   userID = defaultSharedPreferences.getString("pkey_user_Id", "")

    Log.d("call-status: ", callStatus!!)
    Log.d("callfrom-id: ", userID!!)

    if (callStatus != null) {
        information = if (callStatus == CallLogStatus.MISSED.value) {

            return if (callFrom == userID) {
                CallLogStatus.OUTGOING.value +" | " + "0:0"
            } else{
                CallLogStatus.MISSED.value
            }
            } else {
            callStatus + " | " + convertSecondToTime(duration)
        }
    }
    return information

}


fun convertSecondToTime(seconds: Int): String {
    var hours = seconds / 3600
    val minutes = (seconds % 3600) / 60
    return String.format("%02d:%02d", minutes, (seconds % 3600) % 60) + " min"

}

fun callMethod(isLanguageSwitch: Boolean, text: String, languageCode: String): String {
    return runBlocking {
        if (isLanguageSwitch) {
            val jobA =
                CoroutineScope(Dispatchers.IO).async { googleTranslateApi(text, languageCode) }
            jobA.await()
        } else text
    }
}

fun googleTranslateApi(text: String, languageCode: String): String {
    var orignalText = text
    try {
        TranslateOptions.getDefaultInstance().service?.let {
            it.translate(
                text,
                Translate.TranslateOption.targetLanguage(languageCode),
                Translate.TranslateOption.model("nmt")
            )?.let {
                orignalText = it.translatedText
            }
        }
    } catch (e: Exception) {
    }
    return orignalText
}

fun replyMessageSender(chat: Chat, currentUserId: String): String {
    if (chat.members.first().userId == currentUserId) {
        return "You"
    } else {
        return chat.from_name
    }
}