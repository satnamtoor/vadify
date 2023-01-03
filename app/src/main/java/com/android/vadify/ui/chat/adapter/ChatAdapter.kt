package com.android.vadify.ui.chat.adapter

import android.util.Log
import android.view.View
import android.widget.SeekBar
import androidx.appcompat.widget.AppCompatImageView
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.DiffUtil
import com.android.vadify.R
import com.android.vadify.VadifyApplication
import com.android.vadify.data.api.enums.DownloadUploadStatus
import com.android.vadify.data.api.enums.MessageType
import com.android.vadify.data.db.DataConverter
import com.android.vadify.data.db.chat.Chat
import com.android.vadify.data.repository.ChatRepository
import com.android.vadify.data.repository.FileUploadRepositry
import com.android.vadify.databinding.ItemChatBinding
import com.android.vadify.ui.baseclass.BaseBackStack
import com.android.vadify.ui.chat.ChatActivity
import com.android.vadify.ui.chat.viewmodel.ChatViewModel
import com.android.vadify.ui.util.imageUrl
import com.android.vadify.ui.util.imageUrlWithoutPlaceHolder
import com.android.vadify.ui.util.setImageDrawable
import com.android.vadify.viewmodels.EncryptionViewModel
import com.google.android.material.textview.MaterialTextView
import com.google.gson.Gson
import com.sdi.joyersmajorplatform.common.livedataext.throttle
import com.sdi.joyersmajorplatform.common.livedataext.throttleClicks
import io.reactivex.Observable
import io.reactivex.ObservableEmitter


class ChatAdapter(

    private val chatViewModel: ChatViewModel,
    private val filePath: String,
    private val isGroup: String,
    private val isAudioPlayOrNot: (Boolean) -> Unit,private val  chatActivity:ChatActivity
) : ChatAudioPlayer(diffCallback, filePath) {


    //var mMediaPlayer: MediaPlayer?= null

    private var callClickEmitter: ObservableEmitter<String>? = null
    val callClick: Observable<String> = Observable.create<String> {
        callClickEmitter = it
    }.throttle()

    private var messageClickEmitter: ObservableEmitter<String>? = null
    val messageClick: Observable<String> = Observable.create<String> {
        messageClickEmitter = it
    }.throttle()

    private var longPressEmitter: ObservableEmitter<Chat>? = null
    val longPress: Observable<Chat> = Observable.create<Chat> {
        longPressEmitter = it
    }.throttle()

    private var clickEmitter: ObservableEmitter<Chat>? = null
    val clickPress: Observable<Chat> = Observable.create<Chat> {
        clickEmitter = it
    }.throttle()

    override fun isAudioPlayerStartOrStop(isStart: Boolean) {
        isAudioPlayOrNot.invoke(isStart)
    }

    /**
     * The [LayoutRes] for the RecyclerView item
     * This is used to inflate the view.
     */
    override val defaultLayoutRes: Int
        get() = R.layout.item_chat


    override fun bind(
        bind: ItemChatBinding,
        itemType: Chat?,
        position: Int
    ) {
        itemType?.let { chatData ->
           Log.d("bindDts: ", Gson().toJson(chatData))
            chatViewModel.updateDate(chatData.createdAt)
            VadifyApplication.progress.observe(chatActivity, Observer {
                Log.d( "doWork:-- ",Gson().toJson(it))
                if(it.containsKey(itemType.localOrignalPath))
                    bind.progressFileUpload.progress = it[itemType.localOrignalPath]!!
            })

            VadifyApplication.progressDownload.observe(chatActivity, Observer {
                if(it.containsKey(itemType.url))
                    bind.progressFileUploadReceiver.progress = it[itemType.url]!!
            })
            bind.parentContainerView.setOnLongClickListener {
                longPressEmitter?.onNext(itemType)
                true
            }

            bind.parentContainerView.setOnClickListener {
                clickEmitter?.onNext(itemType)
                true
            }

            chatData.members.firstOrNull()?.let {
                if (it.read) {
                    bind.timer1.setImageResource(R.drawable.ic_readmark)
                } else if (it.delivered) {
                    bind.timer1.setImageResource(R.drawable.ic_delivered)
                } else {
                    bind.timer1.setImageResource(R.drawable.ic_sent)
                }
            }


            bind.position = position
            bind.item = chatData
            bind.userId = chatViewModel.getUserId()
            // bind.isToggleOn = chatViewModel.isLanguageSwitch.value

            bind.receiverContactMessage.throttleClicks().subscribe {
                messageClickEmitter?.onNext(chatData.contact_number)
            }

            bind.receiverContactCall.throttleClicks().subscribe {
                callClickEmitter?.onNext(chatData.contact_number)
            }

            bind.contactCall.throttleClicks().subscribe {
                callClickEmitter?.onNext(chatData.contact_number)
            }

            bind.contactMessage.throttleClicks().subscribe {
                messageClickEmitter?.onNext(chatData.contact_number)
            }

            bind.retryMusic.setOnClickListener {
                retry(chatData)
            }

            bind.receiverRetry.setOnClickListener {
                retry(chatData)
            }


            if (isGroup != "Single" && !isGroup.isEmpty()) {
                //intent.putExtra(BaseBackStack.ANOTHER_USER_URL, data.members[0].profileImage)

                bind.txtReceiverName.visibility = View.VISIBLE
                bind.txtReceiverName.setText(chatData.from_name.toString())
            } else {
               // intent.putExtra(BaseBackStack.ANOTHER_USER_URL, data.profileImage)
                bind.txtReceiverName.visibility = View.GONE
            }


            if (!chatData.members.isNullOrEmpty())
            {
            val myMessage = chatData.members.filter { it.userId == chatViewModel.preferenceService.getString(R.string.pkey_user_Id) }
            // val testMsg =  chatData.members.map { EncryptionViewModel.decryptString(it.message) }

                if (chatData.from_id.equals(chatViewModel.preferenceService.getString(R.string.pkey_user_Id))) {
                    if (myMessage.get(0).message.isNullOrEmpty())
                        bind.messageSender.visibility = View.GONE
                    else {
                        bind.messageSender.visibility = View.VISIBLE
                        bind.messageSender.text = EncryptionViewModel.decryptString(myMessage[0].message)
                    }
                }
                else {
                    if (myMessage[0].message.isNullOrEmpty())
                        bind.materialTextView.visibility = View.GONE
                    else {
                        bind.materialTextView.visibility = View.VISIBLE
                        bind.materialTextView.text = EncryptionViewModel.decryptString(myMessage[0].message)
                    }
                }

            }
            else{
                bind.rlImageSend.visibility = View.GONE
                bind.rlImageRec.visibility = View.GONE
                bind.messageSender.visibility = View.GONE
                bind.materialTextView.visibility = View.GONE
            }
            //Log.d( "json-messages", Gson().toJson(testMsg))
           // val  msg = myMessage.get(0).message



            bind.receiverPauseMusic.setOnClickListener { senderReceiverPauseButton() }
            bind.receiverPlayMusic.setOnClickListener {
                senderReceiverSidePlayButton(
                    chatData,
                    bind.receiverSeekBar,
                    bind.receiverPauseMusic,
                    bind.receiverPlayMusic,
                    bind.receiverTimer
                )
            }
            bind.pauseMusic.setOnClickListener { senderReceiverPauseButton() }
            bind.playMusic.setOnClickListener {
                senderReceiverSidePlayButton(
                    chatData,
                    bind.seekBar,
                    bind.pauseMusic,
                    bind.playMusic,
                    bind.timer
                )
            }

            if (chatData.replyToMessageId.isNotBlank()) {

                val repliedChat = chatViewModel.chatListCache.getMessage(chatData.replyToMessageId)
                Log.d( "reply-data: ",Gson().toJson(repliedChat))

                if (repliedChat == null) {
                    return
                }

                if (repliedChat.type.equals("text", true)) {
                    print("hello")
                }

                val myMessage = repliedChat.members.filter { it.userId == chatViewModel.preferenceService.getString(R.string.pkey_user_Id) }
                if (repliedChat.from_id.equals(chatViewModel.preferenceService.getString(R.string.pkey_user_Id))) {
                    bind.replyToMessageSender.text =EncryptionViewModel.decryptString(myMessage[0].message)
                    Log.d( "reply-datam: ",Gson().toJson(myMessage[0].message))
                }
                else{
                bind.replyToMessageReceiver.text = EncryptionViewModel.decryptString(myMessage[0].message)
                    Log.d( "reply-datam: ",Gson().toJson(myMessage[0].message))
                }



                bind.replyContainerViewSender.visibility = View.VISIBLE
                bind.replyContainerViewReceiver.visibility = View.VISIBLE

                bind.replyToMessageSender.text = ""
                bind.replyToMessageReceiver.text = ""

                bind.replyImageSender.visibility = View.GONE
                bind.replyImageSender.setImageDrawable(null)

                bind.replyImageReceiver.visibility = View.GONE
                bind.replyImageReceiver.setImageDrawable(null)

                when {
                    repliedChat.type.equals(MessageType.IMAGE.value, true) -> {
                        bind.replyImageSender.visibility = View.VISIBLE
                        bind.replyToMessageSender.text = "Image"
                        bind.replyImageSender.imageUrlWithoutPlaceHolder(repliedChat.url)

                        bind.replyImageReceiver.visibility = View.VISIBLE
                        bind.replyToMessageReceiver.text = "Image"
                        bind.replyImageReceiver.imageUrlWithoutPlaceHolder(repliedChat.url)
                    }

                    repliedChat.type.equals(MessageType.VIDEO.value, true) -> {
                        bind.replyImageSender.visibility = View.VISIBLE
                        bind.replyToMessageSender.text = "Video"
                        bind.replyImageSender.imageUrlWithoutPlaceHolder(repliedChat.url)

                        bind.replyImageReceiver.visibility = View.VISIBLE
                        bind.replyToMessageReceiver.text = "Video"
                        bind.replyImageReceiver.imageUrlWithoutPlaceHolder(repliedChat.url)
                    }

                    repliedChat.type.equals(MessageType.CONTACT.value, true) -> {
                        bind.replyImageSender.visibility = View.VISIBLE
                        bind.replyToMessageSender.text = "Contact"
                        bind.replyImageSender.setImageDrawable(R.drawable.ic_chat_contact)

                        bind.replyImageReceiver.visibility = View.VISIBLE
                        bind.replyToMessageReceiver.text = "Contact"
                        bind.replyImageReceiver.setImageDrawable(R.drawable.ic_chat_contact)
                    }

                    repliedChat.type.equals(MessageType.DOCUMENT.value, true) -> {
                        bind.replyImageSender.visibility = View.VISIBLE
                        bind.replyToMessageSender.text = "Document"
                        bind.replyImageSender.setImageDrawable(R.drawable.icon_file_doc)

                        bind.replyImageReceiver.visibility = View.VISIBLE
                        bind.replyToMessageReceiver.text = "Document"
                        bind.replyImageReceiver.setImageDrawable(R.drawable.icon_file_doc)
                    }

                    repliedChat.type.equals(MessageType.LOCATION.value, true) -> {
                        bind.replyImageSender.visibility = View.VISIBLE
                        bind.replyToMessageSender.text = "Location"
                        bind.replyImageSender.setImageDrawable(R.drawable.ic_map_address)

                        bind.replyImageReceiver.visibility = View.VISIBLE
                        bind.replyToMessageReceiver.text = "Location"
                        bind.replyImageReceiver.setImageDrawable(R.drawable.ic_map_address)
                    }

                    repliedChat.type.equals(MessageType.AUDIO.value, true) -> {
                        bind.replyImageSender.visibility = View.VISIBLE
                        bind.replyToMessageSender.text = "Audio"
                        bind.replyImageSender.setImageDrawable(R.drawable.ic_audio_play_icon)

                        bind.replyImageReceiver.visibility = View.VISIBLE
                        bind.replyToMessageReceiver.text = "Audio"
                        bind.replyImageReceiver.setImageDrawable(R.drawable.ic_audio_play_icon)
                    }

                    repliedChat.type.equals(MessageType.TEXT.value, true) -> {
                        //if (repliedChat.from_id.equals(chatViewModel.preferenceService.getString(R.string.pkey_user_Id))) {
                            bind.replyToMessageSender.text =EncryptionViewModel.decryptString(myMessage[0].message)

                            bind.replyImageSender.visibility = View.GONE
                     //   }


                            bind.replyToMessageReceiver.text = EncryptionViewModel.decryptString(myMessage[0].message)
                            bind.replyImageReceiver.visibility = View.GONE
                        }





                }

                if (repliedChat.from_id == chatViewModel.getUserId()) {
                    bind.replyMessageSender.text = "You"
                    bind.replyMessageReceiver.text = "You"
                } else {
                    bind.replyMessageSender.text = repliedChat.from_name
                    bind.replyMessageReceiver.text = repliedChat.from_name
                }
            } else {
                bind.replyContainerViewSender.visibility = View.GONE
                bind.replyContainerViewReceiver.visibility = View.GONE
            }
        }
    }

    private fun senderReceiverPauseButton() {
      //  isAudioPlayOrNot.invoke(false)
        mMediaPlayer?.pause()
        audioFileView?.updatePreviousView(View.GONE, View.VISIBLE)
    }

    private fun retry(chatData: Chat) {
        when {
            chatData.downloadStatus == DownloadUploadStatus.FAILED.value && (chatData.url.isBlank() || !chatData.url.contains(
                "http"
            )) -> {
                chatViewModel.uploadFileOnBackground(
                    chatData.localUrl,
                    chatData.message,
                    chatData.type,
                    FileUploadRepositry.AUDIO_FILE,
                    chatData.localOrignalPath
                )
            }
            else -> {
                stopAudio()
                chatViewModel.downloadFileOnBackground(chatData.url, chatData.type)
            }
        }
    }


    private fun senderReceiverSidePlayButton(
        chatData: Chat,
        seekBar: SeekBar,
        pauseMusic: AppCompatImageView,
        playMusic: AppCompatImageView,
        timer: MaterialTextView
    ) {
        when {
            audioFileView?.isCurrentMusicPlay(chatData) == true -> {
                isAudioPlayOrNot.invoke(true)
                mMediaPlayer?.start()
                audioFileView?.updatePreviousView(View.VISIBLE, View.GONE)
            }
            chatData.downloadStatus == DownloadUploadStatus.FAILED.value && (chatData.url.isBlank() || !chatData.url.contains(
                "http"
            )) -> {
                chatViewModel.uploadFileOnBackground(
                    chatData.localUrl,
                    chatData.message,
                    chatData.type,
                    FileUploadRepositry.AUDIO_FILE,
                    chatData.localOrignalPath
                )
            }
            checkLocalPathExist(chatData).isNotEmpty() -> {
                isAudioPlayOrNot.invoke(true)
                seekBar.callLocalDirectory(chatData, pauseMusic, playMusic, timer)
            }
            else -> {
                stopAudio()
                chatViewModel.downloadFileOnBackground(chatData.url, chatData.type)
            }
        }
    }


    companion object {
        private val diffCallback = object : DiffUtil.ItemCallback<Chat>() {
            override fun areItemsTheSame(
                oldItem: Chat,
                newItem: Chat
            ): Boolean = oldItem == newItem

            override fun areContentsTheSame(
                oldItem: Chat,
                newItem: Chat
            ): Boolean = oldItem.createdAt == newItem.createdAt
                    && oldItem.downloadStatus == newItem.downloadStatus

        }
    }

    override fun map(binding: ItemChatBinding): Chat? {

        return binding.item
    }


//   private suspend  fun translateApiCall1(text: String, languageCode:String):String  = coroutineScope {
//       var  orignalText  = text
//       async {
//         orignalText =  try {
//               TranslateOptions.getDefaultInstance().service?.let {
//                   it.translate(text, Translate.TranslateOption.targetLanguage(languageCode), Translate.TranslateOption.model("nmt"))?.let {
//                       orignalText = it.translatedText
//                   }
//               }
//             orignalText
//
//           } catch (e: Exception) {
//             orignalText
//           }
//       }.await()
//   }


//     fun googleTranslateApi(text: String, languageCode:String): String {
//       var orignalText= text
//       try{
//           TranslateOptions.getDefaultInstance().service?.let {
//               it.translate(text, Translate.TranslateOption.targetLanguage(languageCode), Translate.TranslateOption.model("nmt"))?.let {
//                   orignalText = it.translatedText
//               }
//           }
//       }catch (e:Exception){}
//        return orignalText
//    }

}
