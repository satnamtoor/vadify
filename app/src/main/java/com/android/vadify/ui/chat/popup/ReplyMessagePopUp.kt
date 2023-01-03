package com.android.vadify.ui.chat.popup

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.media.AudioManager
import android.media.MediaPlayer
import android.os.Build
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.text.method.ScrollingMovementMethod
import android.util.Log
import android.view.View
import android.view.WindowManager
import android.widget.Toast
import androidx.fragment.app.viewModels
import com.android.vadify.R
import com.android.vadify.data.api.enums.MessageAction
import com.android.vadify.data.api.enums.MessageType
import com.android.vadify.data.db.chat.Chat
import com.android.vadify.databinding.PopupReplyLayoutBinding
import com.android.vadify.ui.baseclass.BaseDialogDaggerListFragment
import com.android.vadify.ui.chat.adapter.ReplyItemAdapter
import com.android.vadify.ui.chat.viewmodel.ReplyMessageViewModel
import com.android.vadify.viewmodels.EncryptionViewModel
import com.google.gson.Gson
import com.sdi.joyersmajorplatform.common.livedataext.throttleClicks
import com.sdi.joyersmajorplatform.uiview.NetworkState
import kotlinx.android.synthetic.main.popup_reply_layout.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import java.io.IOException
import java.util.*

enum class ReplyOptionSelected {
    FORWARD, DELETE, REPLY
}

class ReplyMessagePopUp(
    var text: String,
    var createdData: String,
    var languageCode: String,
    var chat: Chat,
    var selection: (ReplyOptionSelected, Chat) -> Unit
) : BaseDialogDaggerListFragment<PopupReplyLayoutBinding>() {

    var textToSpeech: TextToSpeech? = null

    override val layoutRes: Int
        get() = R.layout.popup_reply_layout

    private var mMediaPlayer: MediaPlayer? = null

    val viewModel: ReplyMessageViewModel by viewModels { viewModelFactory }

    override fun onStart() {
        super.onStart()

        dialog?.window?.setLayout(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.WRAP_CONTENT
        )
        dialog?.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        dialog?.setCancelable(false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        materialTextView.movementMethod = ScrollingMovementMethod();
        val manager = requireContext().getSystemService(Context.AUDIO_SERVICE) as AudioManager
        manager.mode = AudioManager.MODE_NORMAL
        //  manager.setStreamVolume(AudioManager.STREAM_MUSIC, 12, 0)

        if (chat.type == MessageType.DOCUMENT.value ||
            chat.type == MessageType.LOCATION.value ||
            chat.type == MessageType.CONTACT.value ||
            chat.type == MessageType.AUDIO.value
            || chat.type == MessageType.IMAGE.value || chat.type == MessageType.VIDEO.value
        ) {
            viewModel.messageList.remove(
                MessageData(
                    MessageAction.LISTEN.value,
                    R.drawable.ic_listen
                )
            )
            viewModel.messageList.remove(MessageData(MessageAction.COPY.value, R.drawable.ic_copy))
            viewModel.messageList.remove(
                MessageData(
                    MessageAction.TRANSLATE.value,
                    R.drawable.ic_translate
                )
            )
        }

        initAdapter(ReplyItemAdapter(), replyLayout, viewModel.messageList) {
            when (it.message) {
                MessageAction.LISTEN.value -> {
                    viewModel.localNetworkState.postValue(NetworkState.loading)
                    viewModel.messageText.value?.let { text ->
                        CoroutineScope(Dispatchers.IO).launch {
                            val contactList = async { viewModel.getLanguageCodeFromText(text) }
                            getLanguageCode(contactList.await(), text)
                        }
                    }
                }

                MessageAction.COPY.value -> {
                    copyMessageToClipboard()
                }

                MessageAction.TRANSLATE.value -> {
                    if (!viewModel.isTranslateSucessFull)
                        viewModel.translateLanguageToAnother(languageCode)
                    else showMessage(getString(R.string.message_translate_label))
                }

                MessageAction.FORWARD.value -> {
                    chat.forwarded = true
                    selection.invoke(ReplyOptionSelected.FORWARD, chat)
                    dialog?.dismiss()
                }

                MessageAction.DELETE.value -> {
                    selection.invoke(ReplyOptionSelected.DELETE, chat)
                    dialog?.dismiss()
                }

                MessageAction.REPLY.value -> {
                    selection.invoke(ReplyOptionSelected.REPLY, chat)
                    dialog?.dismiss()
                }
            }
        }

        viewModel.updateData(text, createdData)
        subscribe(closeBtn.throttleClicks()) {
            dialog?.dismiss()
        }

        bindNetworkState(viewModel.traslateTextState, loadingIndicator = progress_view, onError = {
            showMessage(requireContext().resources.getString(R.string.tranalate_text_faliure))
        })

        bindNetworkState(viewModel.localNetworkState, loadingIndicator = progress_view)

        bindNetworkState(viewModel.networkState, loadingIndicator = progress_view, onSuccess = {
            playAudio(viewModel.audioFile)
        }, onError = {
            showMessage(requireContext().resources.getString(R.string.speechRecoganizationProcess))
        })
    }

    private fun copyMessageToClipboard() {

        Log.d( "copyMessageToClipboard: ",text)
       // Log.d( "copyMessageToClipboard1: ",Gson().toJson(EncryptionViewModel.decryptString(chat.members[1].message)))


        val clipboardManager =
            activityContext.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clipData = ClipData.newPlainText("message", text)
        clipboardManager.setPrimaryClip(clipData)
        Toast.makeText(activityContext, "Message copied to clipboard", Toast.LENGTH_SHORT).show()
        dialog?.dismiss()
    }

    private fun playAudio(base64EncodedString: String) {
        try {
            stopAudio()
            val url = "data:audio/mp3;base64,$base64EncodedString"
            mMediaPlayer = MediaPlayer()
            mMediaPlayer?.setDataSource(url)
            mMediaPlayer?.prepare()
            mMediaPlayer?.start()
            //  speakSuccess(mMessage)
        } catch (IoEx: IOException) {
            showMessage(requireContext().resources.getString(R.string.speechRecoganizationProcess))
            // speakFail(mMessage, IoEx)
        }
    }

    private fun stopAudio() {
        if (mMediaPlayer != null && mMediaPlayer?.isPlaying == true) {
            mMediaPlayer?.stop()
            mMediaPlayer?.reset()
        }
        textToSpeech?.stop()
    }

    override fun onDestroy() {
        super.onDestroy()
        stopAudio()
    }

    override fun onBindView(binding: PopupReplyLayoutBinding) {
        binding.vm = viewModel
    }

    data class MessageData(var message: String, var drawable: Int)


    override fun onPause() {
        super.onPause()
        stopAudio()
    }

    private fun textToSpeech(language: String, text: String) {
        textToSpeech = TextToSpeech(requireContext(), TextToSpeech.OnInitListener { status ->
            viewModel.localNetworkState.postValue(NetworkState.success)
            when (status) {
                TextToSpeech.SUCCESS -> {
                    if (status != TextToSpeech.ERROR) {
                        textToSpeech?.language = Locale(language)
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                            textToSpeech?.speak(text, TextToSpeech.QUEUE_FLUSH, null, null);
                        } else {
                            textToSpeech?.speak(text, TextToSpeech.QUEUE_FLUSH, null);
                        }
                    }
                }
                else -> viewModel.googleTextToSpeech(text)
            }
        })
    }

    private fun getLanguageCode(language: String, text: String) {
        CoroutineScope(Dispatchers.Main).launch {
            textToSpeech(language, text)
        }
    }
}
