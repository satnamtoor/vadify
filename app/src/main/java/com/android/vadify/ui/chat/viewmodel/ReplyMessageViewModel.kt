package com.android.vadify.ui.chat.viewmodel

import android.util.Log
import androidx.lifecycle.MutableLiveData
import com.android.vadify.R
import com.android.vadify.data.api.enums.MessageAction
import com.android.vadify.data.db.chat.Chat
import com.android.vadify.data.service.PreferenceService
import com.android.vadify.ui.chat.popup.ReplyMessagePopUp
import com.google.cloud.translate.Translate
import com.google.cloud.translate.TranslateOptions
import com.sdi.joyersmajorplatform.common.livedataext.mutableLiveData
import com.sdi.joyersmajorplatform.uiview.NetworkState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject


class ReplyMessageViewModel @Inject constructor(val preferenceService: PreferenceService) :
    TextToSpeechModel() {


    var createdDate = mutableLiveData("")

    var isTranslateSucessFull = false

    var traslateTextState: MutableLiveData<NetworkState> = MutableLiveData()

    var messageList = arrayListOf<ReplyMessagePopUp.MessageData>().also {
        it.add(ReplyMessagePopUp.MessageData(MessageAction.REPLY.value, R.drawable.ic_reply))
         it.add(ReplyMessagePopUp.MessageData(MessageAction.FORWARD.value, R.drawable.ic_forward))
        it.add(ReplyMessagePopUp.MessageData(MessageAction.TRANSLATE.value,R.drawable.ic_translate))
        it.add(ReplyMessagePopUp.MessageData(MessageAction.LISTEN.value, R.drawable.ic_listen))
          it.add(ReplyMessagePopUp.MessageData(MessageAction.COPY.value, R.drawable.ic_copy))
         it.add(ReplyMessagePopUp.MessageData(MessageAction.DELETE.value, R.drawable.ic_trash))
    }


    var shareList = arrayListOf<ReplyMessagePopUp.MessageData>().also {
        it.add(
            ReplyMessagePopUp.MessageData(
                MessageAction.PHOTO_VIDEO.value,
                R.drawable.ic_image_upload_single_chat
            )
        )
        it.add(ReplyMessagePopUp.MessageData(MessageAction.Document.value, R.drawable.ic_doc))
        it.add(ReplyMessagePopUp.MessageData(MessageAction.Location.value, R.drawable.ic_loc))
        it.add(
            ReplyMessagePopUp.MessageData(
                MessageAction.Contact.value,
                R.drawable.ic_contact_btn
            )
        )
    }


    fun updateData(message: String, createdDate: String) {
        messageText.value = message
        this.createdDate.value = createdDate
    }

    fun translateLanguageToAnother(languageCode: String) {

      //  Log.d( "translateLanguageToAnother: ", languageCode)
        traslateTextState.postValue(NetworkState.loading)
        CoroutineScope(Dispatchers.IO).launch {
            try {
                TranslateOptions.getDefaultInstance().service?.let {
                    it.translate(
                        messageText.value,
                        Translate.TranslateOption.targetLanguage(languageCode.substringBefore(
                            "-")),
                        Translate.TranslateOption.model("nmt")
                    )?.let {
                        messageText.postValue(it.translatedText)
                        isTranslateSucessFull = true
                        traslateTextState.postValue(NetworkState.success)
                    }
                }
            } catch (e: Exception) {
                traslateTextState.postValue(NetworkState.error(R.string.tranalate_text_faliure))
            }
        }
    }
}