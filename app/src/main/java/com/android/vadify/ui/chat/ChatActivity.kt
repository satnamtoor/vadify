package com.android.vadify.ui.chat

import android.annotation.SuppressLint
import android.app.Activity
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.media.AudioManager
import android.media.MediaPlayer
import android.media.MediaRecorder
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.CountDownTimer
import android.provider.ContactsContract
import android.speech.SpeechRecognizer
import android.text.TextWatcher
import android.util.Log
import android.view.View
import android.view.WindowManager
import android.view.animation.AnimationUtils
import android.view.inputmethod.InputMethodInfo
import android.view.inputmethod.InputMethodManager
import android.view.inputmethod.InputMethodSubtype
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.LinearLayout
import androidx.appcompat.app.AlertDialog
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.core.widget.doOnTextChanged
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import androidx.lifecycle.observe
import androidx.preference.PreferenceManager
import com.afollestad.assent.Permission
import com.afollestad.assent.isAllGranted
import com.afollestad.assent.runWithPermissions
import com.android.vadify.BuildConfig
import com.android.vadify.R
import com.android.vadify.VadifyApplication
import com.android.vadify.data.api.enums.*
import com.android.vadify.data.api.models.LocalLanguageModel
import com.android.vadify.data.db.chat.Chat
import com.android.vadify.data.db.commands.Commands
import com.android.vadify.data.extension.isGPSEnabled
import com.android.vadify.data.extension.isNetworkEnabled
import com.android.vadify.data.repository.FileUploadRepositry
import com.android.vadify.databinding.ActivityChatBinding
import com.android.vadify.service.Constants
import com.android.vadify.ui.baseclass.BaseBackStack
import com.android.vadify.ui.chat.adapter.ChatAdapter
import com.android.vadify.ui.chat.call.CallActivity
import com.android.vadify.ui.chat.call.videocall.VideoCallActivity
import com.android.vadify.ui.chat.camera.CameraActivity
import com.android.vadify.ui.chat.contact.UserContactInformation
import com.android.vadify.ui.chat.popup.ImagePopUp
import com.android.vadify.ui.chat.popup.ReplyMessagePopUp
import com.android.vadify.ui.chat.popup.ReplyOptionSelected
import com.android.vadify.ui.chat.popup.ShareMessagePopUp
import com.android.vadify.ui.chat.videoplayer.VideoPlayerActivity
import com.android.vadify.ui.chat.viewmodel.ChatViewModel
import com.android.vadify.ui.chat.viewmodel.UploadFileWorker
import com.android.vadify.ui.dashboard.activity.RetakeCommand
import com.android.vadify.ui.dashboard.fragment.chat.ChatFragment
import com.android.vadify.ui.dashboard.fragment.vadifyfriend.VadifyFriendFragment
import com.android.vadify.ui.login.fragment.CommandsFragment
import com.android.vadify.ui.login.fragment.PersonalInformationFragment
import com.android.vadify.ui.login.viewmodel.ProfileFragmentViewModel
import com.android.vadify.ui.util.imageUrlWithoutPlaceHolder
import com.android.vadify.ui.util.setImageDrawable
import com.android.vadify.utils.LocalStorage
import com.android.vadify.utils.LocalStorage.getAudioPath
import com.android.vadify.utils.LocalStorage.getFilePath
import com.android.vadify.utils.RxBus
import com.android.vadify.viewmodels.CommandsViewModel
import com.android.vadify.viewmodels.EncryptionViewModel
import com.android.vadify.widgets.checkFileSize
import com.android.vadify.widgets.currentSeconds
import com.android.vadify.widgets.messagePicker
import com.android.vadify.widgets.onTextChange
import com.google.cloud.translate.Translate
import com.google.cloud.translate.TranslateOptions
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.hbb20.CountryCodePicker
import com.sdi.joyersmajorplatform.common.livedataext.throttleClicks
import com.sdi.joyersmajorplatform.common.progressDialog
import com.sdi.joyersmajorplatform.uiview.NetworkState
import droidninja.filepicker.FilePickerBuilder
import droidninja.filepicker.FilePickerConst
import io.socket.client.Socket
import io.socket.emitter.Emitter
import kotlinx.android.synthetic.main.activity_chat.*
import kotlinx.android.synthetic.main.activity_chat.cardView10
import kotlinx.android.synthetic.main.activity_edit_profile.*
import kotlinx.android.synthetic.main.create_group.*
import kotlinx.android.synthetic.main.fragment_personal_information.*
import kotlinx.coroutines.*
import org.json.JSONObject
import java.io.File
import java.io.IOException
import smartdevelop.ir.eram.showcaseviewlib.config.DismissType

import smartdevelop.ir.eram.showcaseviewlib.GuideView
import smartdevelop.ir.eram.showcaseviewlib.config.Gravity
import smartdevelop.ir.eram.showcaseviewlib.listener.GuideListener


class ChatActivity : SpeechRecoganization() {
 var roomIdChatFrag:String = ""
    val viewModelChat: ChatViewModel by viewModels()
    private var mMediaPlayer: MediaPlayer? = null
    private var mRecorder: MediaRecorder? = null
    private var voiceTimer: CountDownTimer? = null
    var liveData: MutableLiveData<String> = MutableLiveData()
    var commanData: ArrayList<Commands> = ArrayList()
    public val commandViewModel: CommandsViewModel by viewModels()
    var languageCode: String = ""
    private val encryptionViewModel: EncryptionViewModel by viewModels()
    override val layoutRes: Int get() = R.layout.activity_chat
    lateinit var languageCodeArray: List<String>
    lateinit var langCodelist: List<String>
    lateinit var defaultSharedPreferences: SharedPreferences
    private var firstCall = false
    companion object {
        const val INPUT_LANGUAGE = "inputLanguage"
        var isAudioOn = false
        var isMicOn: Boolean = false
        val thread: MutableList<Thread> = ArrayList()
        var isListenCall: Boolean = false
        var isHasValue: Boolean = false
        const val MESSAGE = "MESSAGE"
        const val GROUP_MESSAGE = "GROUP_MESSAGE"
        const val ACKNOWLEDGE_MESSAGE = "ACKNOWLEDGE_MESSAGE"
        const val RECEIVE_MESSAGE = "NEW_MESSAGE"
        const val CALL_LOG_STATUS = "CALL_LOG_STATUS"
        const val ONLINE_STATUS = "ONLINE_STATUS"
        const val JOIN_ONLINE_STATUS = "JOIN_ONLINE_STATUS"
        const val LEAVE_ONLINE_STATUS = "LEAVE_ONLINE_STATUS"
        const val TYPING = "TYPING"
        const val UPDATED_MESSAGE = "UPDATED_MESSAGE"
        const val DISPLAY_TYPING = "DISPLAY_TYPING"
        const val MOTHER_LANGUAGE_SWITCH = "MOTHER_SWITCH"
        const val USER_LANGUAGE_CHANGE = "USER_LANGUAGE"
        const val GOOGLE_KEY = "GOOGLE_API_KEY"
        const val PAGE_COUNT = 1
        const val TOTAL_ITEMS = "30"
        const val TIMER = 60000L
        const val DELAY = 1000L
        const val SILENT_MODE = 4
        const val PROFILE_IMAGE_SIZE = 800
        const val CAMERA_REQUEST_CODE = 1
        const val VIDEO_REQUEST_CODE = 2
        const val SELECT_PHONE_NUMBER = 3
        const val MAP_BASE_URL = "http://maps.google.com/maps?q="
        const val SOCKET_FROM = "from"
        const val SOCKET_TO = "to"
        const val SOCKET_TYPE = "type"
        const val SOCKET_MESSAGE = "message"
        const val SOCKET_MESSAGE_SENDER = "messageSender"
        const val SOCKET_MESSAGE_RECEIVER = "messageReceiver"
        const val SOCKET_MESSAGE_REPLY_TO = "replyToMessageId"
        const val SOCKET_URL = "url"
        const val IS_FORWARD = "forwarded"
        const val ROOM_ID = "roomId"
        const val MEMBER_IDs = "memberIds"
        const val ONLINE = "Online"
        const val OFFLINE = "Offline"
        const val TYPING_PROGRESS = "Typing..."
        const val CAMERA = 0
        const val VIDEO = 1
        const val RecyclerViewCache = 30
    }

    lateinit var adapter: ChatAdapter
    val viewModelProfile: ProfileFragmentViewModel by viewModels()

    //    override val listeningLayout: (Boolean) -> Unit
//        get() =  { viewModel.isSpeechLanguageEnable.value = it }

    @SuppressLint("ResourceType")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        (applicationContext as VadifyApplication).firstTimeRoomId.value = ""
        defaultSharedPreferences =
            PreferenceManager.getDefaultSharedPreferences(ChatActivity@ this)

     //   languageCode = viewModel.checkLanguageCodeForSpeech()
        encryptionViewModel.staticContent()
       // Log.d("language-code--", languageCode)
        Log.d("roommmytdft: ","click11")
if (viewModel.preferenceService.getString(com.android.vadify.R.string.pkey_pop1).isNullOrEmpty())
{
        GuideView.Builder(this)
            .setContentText("Tap on  dropdown and\n choose any native language")
            .setGravity(Gravity.auto) //optional
            .setDismissType(DismissType.anywhere) //optional - default DismissType.targetView
            .setTargetView(binding.countryLanguageSelected)
            .setContentTextSize(14) //optional
            .build()
            .show()


        GuideView.Builder(this)
            .setContentText("Blinking microphone means you can talk in your chosen language, and it will type. Tap it to pause to manually correct or type. Tap again to start talking again")
            .setGravity(Gravity.auto) //optional
            .setDismissType(DismissType.anywhere) //optional - default DismissType.targetView
            .setTargetView(binding.mic)
            .setContentTextSize(14) //optional
            .build()
            .show()

    viewModel.preferenceService.putString(R.string.pkey_pop1,"A")
}


        viewModel.getForwardingMessage().observe(this, androidx.lifecycle.Observer {
            if (it == NetworkState.success) {
                onBackPressed()
            }
        })





        viewModel.replyMessage.observe(this, androidx.lifecycle.Observer {
            if (it == null) {
                closeReplyView()
            } else {
                openReplyView()
            }
        })

        initAlls()

        RxBus.listen(String::class.java).subscribe {
            if (it == "Message Sent") {
                CoroutineScope(Dispatchers.Main).async {
                    if (viewModel.replyMessage.value != null) {
                        viewModel.replyMessage.value = null
                    }
                }
            }
        }

        subscribe(sendView.throttleClicks()) {
             if (!searchText.text.isNullOrBlank()) {
                sendSocketData(searchText.text.toString())
                searchText.setText("")

            }
        }

        subscribe(closeReplyView.throttleClicks()) {
            viewModel.replyMessage.value = null
        }

        subscribe(profile_layout.throttleClicks()) {
            Intent(this, UserContactInformation::class.java).also {
                intent.extras?.getString(BaseBackStack.ANOTHER_USER_ID)?.let { userId ->
                    it.putExtra(BaseBackStack.ANOTHER_USER_ID, userId)
                    it.putExtra(BaseBackStack.ROOM_ID, viewModel.roomId.value)
                    it.putExtra(BaseBackStack.FIRST_TIME_START_CHAT, false)
                    it.putExtra(BaseBackStack.IS_BLOCK, viewModel.isBlockedUserOrNot(this).value)
                    it.putExtra(BaseBackStack.GROUP_TYPE, viewModel.groupType.value)
//                   it.putExtra(BaseBackStack.GROUP_TYPE, "Single")
                    startActivity(it)
                }
            }
        }


        subscribe(searchText.onTextChange()) {
            viewModel.userTyping()
        }

        subscribe(cancelView.throttleClicks()) {
            searchText.setText("")

        }

        subscribe(micView.throttleClicks()) {
            viewModel.callChatOptions()
            if (viewModel.preferenceService.getString(com.android.vadify.R.string.pkey_pop2).isNullOrEmpty()) {
                GuideView.Builder(this)
                    .setContentText("Tap to start speech to text.")

                    .setGravity(Gravity.auto) //optional
                    .setDismissType(DismissType.anywhere) //optional - default DismissType.targetView
                    .setTargetView(binding.cardView12)

                    .setContentTextSize(14) //optional
                    .setGuideListener(GuideListener() {
                        GuideView.Builder(this)
                            .setContentText("Tap to record voice message.")

                            .setGravity(Gravity.auto) //optional
                            .setDismissType(DismissType.anywhere) //optional - default DismissType.targetView
                            .setTargetView(binding.audioRecordingView)
                            .setContentTextSize(14) //optional
                            .build()
                            .show()
                    })
                    .build()
                    .show()

                viewModel.preferenceService.putString(R.string.pkey_pop2,"B")
            }
        }

        subscribe(commandDriven.throttleClicks()) {
            //commandViewModel.get

            if (commanData.isNotEmpty()) {
                callSpeechListener()
            } else {
                //withButtonCentered()


                val skiplanguage =
                    defaultSharedPreferences.getString("pkey_skip_language", "")

                Log.d("skip_params ", skiplanguage!!)
              /*  if (skiplanguage.equals("")) {
                    //withButtonCentered()
                    viewModel.callChatOptions()
                } else {*/
                    viewModel.callChatOptions()
                    viewModel.isSpeechLanguageEnable.value = ChatType.SPEECH_TO_TEXT
                    speechToText(languageCode)
                //}
            }
        }
        subscribe(commandDrivenView.throttleClicks()) {
            commandDriven.callOnClick()
        }



        subscribe(audioRecording.throttleClicks()) {
            audioRecordingView()
        }

        subscribe(audioRecordingView.throttleClicks()) {
            audioRecording.callOnClick()
        }

        subscribe(videoCall.throttleClicks()) {
            callNavigation(VideoCallActivity::class.java)
        }

        subscribe(callBtnT.throttleClicks()) {
            if (viewModel.isBlockedUserOrNot(this).value == false) {
                callNavigation(CallActivity::class.java)
            }

        }


        subscribe(audioRecordingCloseBtn.throttleClicks()) {
            stopVoiceRecord()
            speechToText(languageCode)

        }

        subscribe(audioRecordingSendBtn.throttleClicks()) {
            stopVoiceRecord()


            viewModel.uploadFileOnBackground(
                viewModel.voiceRecordingPath,
                "",
                MessageType.AUDIO.value,
                FileUploadRepositry.AUDIO_FILE
            )
        }

        subscribe(sendListeningData.throttleClicks()) {
            if (!listening_input.text.isNullOrBlank()) {
                viewModel.isStopTyping = false
                sendSocketData(listening_input.text.toString())
                listening_input.setText("")
                viewModel.speechLanguageText.value = ""
            }
        }

        subscribe(listeningMic.throttleClicks()) {
            Log.d( "onCreate-calling: ", languageCode)
            if (isMicOn) {
                mic.setImageResource(com.android.vadify.R.drawable.ic_mic)
                isListenCall = true
                viewModel.isSilentMode = 0
                speech?.cancel()
                speech?.stopListening()
                stopSpeechRecoganization()
                isMicOn = false
            } else {

                mic.setImageResource(com.android.vadify.R.drawable.ic_mic_red)
                isListenCall = false
                speechToText(languageCode)

                isMicOn = true
            }

        }

        subscribe(camera.throttleClicks()) {
            viewModel.callMediaOptions()
        }

//        val i = Intent(Intent.ACTION_PICK, MediaStore.Video.Media.EXTERNAL_CONTENT_URI)
//        startActivityForResult(i, 1)

        subscribe(videoRecording.throttleClicks()) {
            viewModel.callMediaOptions()
            startActivityForResult(getIntent(true), VIDEO_REQUEST_CODE)
        }

        subscribe(imagePicker.throttleClicks()) {
            viewModel.callMediaOptions()
            startActivityForResult(getIntent(false), CAMERA_REQUEST_CODE)
        }
        Log.d("onSpeechError: ", "there is an error" + "   " + Thread.currentThread().name)

        subscribe(listenerCancel.throttleClicks()) {

            listening_input.setText("")
            viewModel.isSpeechLanguageEnable.value = ChatType.NORMAL
            stopSpeechRecoganization()
   if (viewModel.preferenceService.getString(com.android.vadify.R.string.pkey_pop3).isNullOrEmpty()) {

       GuideView.Builder(this)
           .setContentText("Tap to share\n pictures, videos and more.")
           .setGravity(Gravity.auto) //optional
           .setDismissType(DismissType.anywhere) //optional - default DismissType.targetView
           .setTargetView(binding.plusBtn)
           .setContentTextSize(14) //optional
          .setGuideListener(GuideListener() {
               GuideView.Builder(this)
                   .setContentText("Tap to start speech to text\n and also to record voice message.")
                   .setGravity(Gravity.auto) //optional
                   .setDismissType(DismissType.anywhere) //optional - default DismissType.targetView
                   .setTargetView(binding.micView)
                   .setContentTextSize(14)
                   .build()
                   .show()
           })
           .build()
           .show()
       viewModel.preferenceService.putString(R.string.pkey_pop3,"C")
   }
        }

        subscribe(plusBtn.throttleClicks()) {
            viewModel.isMediaFile.value = false
            viewModel.chatOption.value = false
            stopSpeechRecoganization()
            ShareMessagePopUp {

                when (it) {
                    MessageAction.Document.value -> documentPicker()
                    MessageAction.Contact.value -> {
                        val intent = Intent(Intent.ACTION_PICK).also {
                            it.type = ContactsContract.CommonDataKinds.Phone.CONTENT_TYPE
                        }
                        startActivityForResult(intent, SELECT_PHONE_NUMBER)
                    }
                    MessageAction.Location.value -> updateLocation()
                    MessageAction.PHOTO_VIDEO.value -> {
                        val options = arrayOf<CharSequence>(
                            getString(com.android.vadify.R.string.Camera),
                            getString(com.android.vadify.R.string.VideoRecording)
                        )
                        this.messagePicker(options) {
                            when (it) {

                                CAMERA -> startActivityForResult(
                                    getIntent(false),
                                    CAMERA_REQUEST_CODE
                                )
                                VIDEO -> startActivityForResult(getIntent(true), VIDEO_REQUEST_CODE)
                            }
                        }
                    }
                }
            }.show(supportFragmentManager, null)
        }


        changeLangugeSwitch.setOnCheckedChangeListener { _, condition ->
            viewModel.switchMotherLanguageButton(condition)
            viewModel.isLanguageSwitch.postValue(condition)
        }
        listening_input.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(charSequence: CharSequence, i: Int, i1: Int, i2: Int) {}
            override fun onTextChanged(charSequence: CharSequence, i: Int, i1: Int, i2: Int) {}
            override fun afterTextChanged(editable: android.text.Editable) {
                listening_input.setSelection(editable.toString().length)
            }
        })

    }


    private fun uploadImage() {
        intent?.extras?.getString(BaseBackStack.IMAGE_PATH)?.let {
            if (it.isNotBlank()) viewModel.uploadFileOnBackground(
                it,
                "",
                MessageType.IMAGE.value,
                FileUploadRepositry.IMAGE_FILE
            )
        }
    }

    private fun setReplyValueNull() {
        if (viewModel.replyMessage.value != null) {
            viewModel.replyMessage.value = null
        }
    }

    override fun onBackPressed() {
        isHasValue = false
        val fragment = supportFragmentManager.findFragmentById(com.android.vadify.R.id.command_container)
        if (fragment is CommandsFragment && fragment != null) {
            if (!fragment.Recording!!) {
                //  supportFragmentManager.popBackStack();
                super.onBackPressed()
            } else {
                showSnackMessage("Recording...")
            }
        } else {
            super.onBackPressed()
        }
        //super.onBackPressed()
    }

    /* private fun initSpeechVoice(condition: Boolean) {
         viewModel.isLanguageSwitch.postValue(condition)
         if (condition) {
             var locale: Locale? = null
             val languageCode = viewModel.checkLanguageCodeForSpeech()
             locale = if (Build.VERSION.SDK_INT >= 21) {
                 Locale.forLanguageTag(languageCode)
             } else {
                 val item = languageCode.split("-")
                 if (item.size > 2) locale = Locale(item[0], item[1])
                 locale
             }
             // Speech.getInstance().setLocale(locale)
         } else {
             val local = if (Build.VERSION.SDK_INT >= 21) Locale.forLanguageTag("en-US") else Locale(
                     "en",
                     "US"
             )
             // Speech.getInstance().setLocale(local)
         }
     }*/


    private fun documentPicker() {
        val zipTypes = arrayOf("zip", "rar")
        FilePickerBuilder.instance
            .addFileSupport("ZIP", zipTypes)
            .setActivityTheme(com.android.vadify.R.style.AppTheme)
            .setMaxCount(1)
            .pickFile(this)
    }


    private fun getIntent(condition: Boolean): Intent {
        return Intent(this@ChatActivity, CameraActivity::class.java).also {
            it.putExtra(CameraActivity.IS_VIDEO_REORDING, condition)
        }
    }

    private fun openReplyOptionsPopup(chat: Chat) {

        var commandListion: String = ""

        val myMessage = chat.members.filter { it.userId == viewModel.preferenceService.getString(com.android.vadify.R.string.pkey_user_Id) }

        if (chat.from_id.equals(viewModel.preferenceService.getString(com.android.vadify.R.string.pkey_user_Id))) {
            commandListion  = EncryptionViewModel.decryptString(myMessage[0].message)
            }

        else {
            commandListion = EncryptionViewModel.decryptString(myMessage[0].message)

        }

        ReplyMessagePopUp(
            commandListion,
            chat.createdAt,
            viewModel.checkLanguageCode(),
            chat
        ) { replyOptionSelected: ReplyOptionSelected, chat: Chat ->
            if (replyOptionSelected == ReplyOptionSelected.FORWARD) {
                val vadifyFriendFragment = VadifyFriendFragment()

                val serializedObject = Gson().toJson(chat).toString()
                val arguments = Bundle()
                arguments.putSerializable(ChatFragment.SCREENT_TYPE_KEY, serializedObject)
                vadifyFriendFragment.arguments = arguments

                supportFragmentManager.beginTransaction()
                    .replace(com.android.vadify.R.id.command_container, vadifyFriendFragment, VadifyFriendFragment.TAG)
                    .addToBackStack(null).commit()
            } else if (replyOptionSelected == ReplyOptionSelected.DELETE) {
                deleteMessageConfirmation(chat)
            } else if (replyOptionSelected == ReplyOptionSelected.REPLY) {
                viewModel.replyMessage.value = chat
            }
        }.show(
            supportFragmentManager,
            null
        )
    }

    private fun mediaPopUp(chat: Chat) {
        when (chat.type) {
            MessageType.TEXT.value -> {
                stopSpeechRecoganization()


                var commandListion: String = ""

                val myMessage = chat.members.filter { it.userId == viewModel.preferenceService.getString(
                    com.android.vadify.R.string.pkey_user_Id) }

                if (chat.from_id.equals(viewModel.preferenceService.getString(com.android.vadify.R.string.pkey_user_Id))) {
                    commandListion  = EncryptionViewModel.decryptString(myMessage[0].message)
                }

                else {
                    commandListion = EncryptionViewModel.decryptString(myMessage[0].message)

                }

                ReplyMessagePopUp(
                    commandListion,
                    chat.createdAt,
                    viewModel.checkLanguageCode(),
                    chat
                ) { replyOptionSelected: ReplyOptionSelected, chat: Chat ->
                    if (replyOptionSelected == ReplyOptionSelected.FORWARD) {
                        val vadifyFriendFragment = VadifyFriendFragment()

                        val serializedObject = Gson().toJson(chat).toString()
                        val arguments = Bundle()
                        arguments.putSerializable(ChatFragment.SCREENT_TYPE_KEY, serializedObject)
                        vadifyFriendFragment.arguments = arguments

                        supportFragmentManager.beginTransaction()
                            .replace(
                                com.android.vadify.R.id.command_container,
                                vadifyFriendFragment,
                                VadifyFriendFragment.TAG
                            )
                            .addToBackStack(null).commit()
                    } else if (replyOptionSelected == ReplyOptionSelected.DELETE) {
                        deleteMessageConfirmation(chat)
                    } else if (replyOptionSelected == ReplyOptionSelected.REPLY) {
                        viewModel.replyMessage.value = chat
                    }
                }.show(
                    supportFragmentManager,
                    null
                )
            }
            MessageType.IMAGE.value -> {
                stopSpeechRecoganization()
                ImagePopUp(chat.url) {
                }.show(supportFragmentManager, null)
            }
            MessageType.VIDEO.value -> {
                val path = viewModel.checkLocalPathExist(
                    this.getFilePath(LocalStorage.DOWNLOAD_VIDEO_FILE_PATH),
                    chat
                )
                when {
                    chat.downloadStatus == DownloadUploadStatus.FAILED.value && (chat.url.isBlank() || !chat.url.contains(
                        "http"
                    )) -> viewModel.uploadFileOnBackground(
                        chat.localUrl,
                        chat.message,
                        chat.type,
                        FileUploadRepositry.VIDEO_FILE,
                        chat.localOrignalPath
                    )
                    path.isNotBlank() -> startActivity(
                        Intent(
                            this@ChatActivity,
                            VideoPlayerActivity::class.java
                        ).also { it.putExtra("uri", path) })
                    else -> viewModel.downloadFileOnBackground(chat.url, chat.type)
                }
            }
            MessageType.DOCUMENT.value -> {
                val path = viewModel.checkLocalPathExist(
                    this.getFilePath(LocalStorage.DOWNLOAD_DOCUMENT_FILE_PATH),
                    chat
                )
                when {
                    chat.downloadStatus == DownloadUploadStatus.FAILED.value && (chat.url.isBlank() || !chat.url.contains(
                        "http"
                    )) -> viewModel.uploadFileOnBackground(
                        chat.localUrl,
                        chat.message,
                        chat.type,
                        FileUploadRepositry.DOCUMENT_FILE,
                        chat.localOrignalPath
                    )
                    path.isNotBlank() -> openFile(path)
                    else -> viewModel.downloadFileOnBackground(chat.url, chat.type)
                }
            }
            MessageType.LOCATION.value -> {
                chat.lat_location?.let { it ->
                    chat.long_location?.let { lng ->
                        val baseUrl = "$MAP_BASE_URL$it , $lng"
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(baseUrl))
                        startActivity(intent)
                    }
                }
            }

            MessageType.CONTACT.value -> {
            }
        }
    }

    private fun openFile(url: String) {
        val intent = Intent(Intent.ACTION_VIEW)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            val file = File(url)
            val uri = FileProvider.getUriForFile(this, "$packageName.provider", file)
            intent.setDataAndType(uri, "application/*")
            intent.flags = Intent.FLAG_ACTIVITY_NO_HISTORY
            intent.flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
        } else {
            intent.setDataAndType(Uri.parse(url), "application/*")
        }
        try {
            startActivity(intent)
        } catch (t: Throwable) {
            t.printStackTrace()
            //attemptInstallViewer();
        }
    }


    override fun onResults(results: Bundle?) {

        val matches = results!!.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
        val text = matches!![0]
        Log.d("speech-new-result: ", "" + results)


        text?.let {
            commandDrivenOperation(it)

        }
//        if (!isListenCall) {
//            if(!isAudioOn)
//                liveData.value = "text"
//        }
        if (listening_layout.visibility == View.VISIBLE && !isListenCall) {

            try {
                GlobalScope.launch {
                    runOnUiThread {
                        speech?.cancel()
                    }
                    delay(50)
                    runOnUiThread {
                        if (!isAudioOn)
                            startCommand("", false, AudioManager.ADJUST_MUTE)
                    }
                }
            } catch (ex: Exception) {
            }

        }
    }

    override fun onError(error: Int) {
        Log.d("onSpeechError: ", "there is an error" + "   " + Thread.currentThread().name)
        val errorMessage: String = getErrorText(error)

        if (listening_layout.visibility == View.VISIBLE && !isListenCall) {

            try {
                GlobalScope.launch {
                    runOnUiThread {
                        speech?.cancel()
                    }
                    delay(50)
                    runOnUiThread {
                        if (!isAudioOn)
                            startCommand("", false, AudioManager.ADJUST_MUTE)
                    }
                }
            } catch (ex: Exception) {
            }

        }
    }


    private fun connectMethod(message: String = "") {
        try {
            viewModel.getSocketInstance()?.let {
                it.on(RECEIVE_MESSAGE, onNewMessage)
                it.on(ONLINE_STATUS, onlineStatus)
                it.on(DISPLAY_TYPING, typingResult)
                it.on(MOTHER_LANGUAGE_SWITCH, motherLanguageSwitchChanged)
                it.on(USER_LANGUAGE_CHANGE, userLanguageChanged)
                it.on(UPDATED_MESSAGE, onMessageUpdated)

                it.connect()
                    .on(Socket.EVENT_CONNECT) {
                        println("socket connected--")
                    }
                    .on(Socket.EVENT_DISCONNECT) {
                        println("socket disconnected--")
                    }
                    .on(Socket.EVENT_CONNECT_ERROR) {
                        println("SOCKET CONNECTION ERROR-- $it")
                    }

                if (message.isNotBlank()) {
                    viewModel.sendOrListenMessage(message)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

    }

    private val onNewMessage: Emitter.Listener = Emitter.Listener { args ->
        viewModel.receiveNewMessage(args)
    }

    private val onMessageUpdated: Emitter.Listener = Emitter.Listener { args ->
        viewModel.messageUpdated(args)
    }

    private val onlineStatus: Emitter.Listener = Emitter.Listener { args ->
        viewModel.checkOnListStatus(args)
    }

    private val typingResult: Emitter.Listener = Emitter.Listener { args ->
        CoroutineScope(Dispatchers.Main).launch {
            if (viewModel.onlineOfflineTypingLabel.value.equals("online")) {
                viewModel.onlineOfflineTypingLabel.value = TYPING_PROGRESS
            }
            delay(5000)
            viewModel.onlineOfflineTypingLabel.value = viewModel.tempStatus
        }
    }

    private val userLanguageChanged: Emitter.Listener = Emitter.Listener { args ->
        CoroutineScope(Dispatchers.IO).launch {
            print("User language changed")

            val data: JSONObject = args[0] as JSONObject

            // Log.d("other_language-code", Gson().toJson(data))
            val userId = data.getString("userId")
            val language = data.getString("language")

            viewModel.updateMotherLanguageForUserId(userId, language)
        }
    }

    private val motherLanguageSwitchChanged: Emitter.Listener = Emitter.Listener { args ->
        CoroutineScope(Dispatchers.Main).launch {
            print("Mother language switch changed")

            val data: JSONObject = args[0] as JSONObject
            val userId = data.getString("userId")
            val motherSwitch = data.getBoolean("motherSwitch")
            Log.d("mother-switch-ommit", ": " + motherSwitch)

            viewModel.updateLanguageSwitchForUserId(userId, motherSwitch)
        }
    }

    private fun disconnectMethod() {
        viewModel.mSocket?.let {
            viewModel.joinLeaveStatus(LEAVE_ONLINE_STATUS)
            it.disconnect()
            it.off(RECEIVE_MESSAGE, onNewMessage)
            it.off(ONLINE_STATUS, onlineStatus)
            it.off(DISPLAY_TYPING, typingResult)
            it.off(MOTHER_LANGUAGE_SWITCH, motherLanguageSwitchChanged)
            it.off(USER_LANGUAGE_CHANGE, userLanguageChanged)
            it.off(UPDATED_MESSAGE, onMessageUpdated)
        }
    }


    private fun sendSocketData(textMessage: String) {
        Log.d("sendSocketData: ", "" + viewModel.isSocketConnected())
        when {
            viewModel.isSocketConnected() -> viewModel.sendOrListenMessage(textMessage)
            else -> {
                disconnectMethod()
                connectMethod(textMessage)
            }
        }
    }

    fun initAlls() {
        initView()

        val isAudioPlayOrNot: (Boolean) -> Unit = { isAudioPlay ->
            if (isAudioPlay) {
                stopSpeechRecoganization()
            } else {
                onRecordAudioPermissionGranted(languageCode) //Removed because of speech recognition text not available error
            }
        }

  /*      viewModel.getChatList.items.observe(this, Observer {
            Log.d("TAGinitAlls: ",Gson().toJson(it))
        })*/

        adapter = chatAdapter(
            ChatAdapter(
                viewModel,
                this.getFilePath(LocalStorage.DOWNLOAD_AUDIO_FILE_PATH),
                viewModel.groupType.value!!,
                isAudioPlayOrNot = isAudioPlayOrNot
            ,this), chatList, viewModel.getChatList, isNewMessage = viewModel.smoothScrolling
        ) {
            mediaPopUp(it)
        }
        // chatList.setItemViewCacheSize(RecyclerViewCache)


        subscribe(adapter.messageClick) {
            startActivity(Intent(Intent.ACTION_VIEW, Uri.fromParts("sms", it, null)))
        }

        subscribe(adapter.longPress) {
            openReplyOptionsPopup(it)
        }

        subscribe(adapter.clickPress) {
            mediaPopUp(it)
        }

        subscribe(adapter.callClick) { phone ->
            runWithPermissions(Permission.CALL_PHONE) {
                val intent = Intent(Intent.ACTION_CALL, Uri.parse("tel:$phone"))
                startActivity(intent)
            }
        }

    }

    override fun onDestroy() {
        super.onDestroy()

        releaseRecording()
        //disconnectMethod()
        // Speech.getInstance()?.shutdown()
        adapter.stopAudio()
        timer?.cancel()
    }


    private fun initView() {

        getAllLanguages()

        viewModelProfile.languageList.observe(this, Observer {
            var allLanguage = ""
            it.forEach {it1 ->
                allLanguage += "${it1.countryCode}/${it1.name}/${it1.languageCode}#"
            }
            countryLanguageSelected.setCustomMasterCountries(allLanguage)
        })
        countryLanguageSelected.setOnCountryChangeListener {

            if (countryLanguageSelected.selectedLanguageCode!=null && firstCall) {
                countryLanguageSelected.also {
                    val parts = it.selectedCountryName.split("(")
                    Log.d( "language----", parts[0])
                    commandViewModel.getCommandAll(parts[0])
                    val edit = defaultSharedPreferences.edit()
                    edit.putString("pkey_skip_language", "")
                    edit.putString("pkey_each_command_Language", parts[0])
                    edit.putString("pkey_each_command_Code",it.selectedLanguageCode)
                    edit.putString("pkey_each_command_country_code",it.selectedCountryNameCode)
                    edit.commit()

                    languageChange(it.selectedLanguageCode + "-" + it.selectedCountryNameCode)
                        viewModelChat.preferenceService.putString(R.string.pkey_each_userLanguage_Code, it.selectedLanguageCode + "-" + it.selectedCountryNameCode)

                    Log.d( "room-idid: ",""+viewModel.roomId.value)
                    bindNetworkState(
                        viewModelProfile.updateUserLanguageModel(
                            viewModel.roomId.value,
                            it.selectedCountryName.split("\\s".toRegex())[0],
                            it.selectedLanguageCode,
                            it.selectedCountryNameCode
                        ),
                        progressDialog(com.android.vadify.R.string.sending)
                    )
                    selected_lang.text = it.selectedLanguageCode.toUpperCase()

                }
            }
            firstCall = true
        }


        viewModel.userlanguageList.observe(this, Observer {
            if (it!=null) {
                Log.d("roommmytdft: ",""+viewModel.isSpeechLanguageEnable.value)
                countryLanguageSelected.setCountryForNameCode(it.countryCode)
                selected_lang.text = it.languageCode.toUpperCase()
                languageCode = it.languageCode + "-" + it.countryCode
                commandViewModel.getCommandAll(it.language)
                speechToText(it.languageCode + "-" + it.countryCode)
                languageChange(it.languageCode + "-" + it.countryCode)
                viewModelChat.preferenceService.putString(com.android.vadify.R.string.pkey_each_userLanguage_Code, it.languageCode + "-" + it.countryCode)

               // viewModel.isSpeechLanguageEnable.value = ChatType.SPEECH_TO_TEXT

            }else{
                Log.d("roommmytdft: ","click23")
                Log.d("idsdsdnitView: ", "english")
                commandViewModel.getCommandAll("English")
                countryLanguageSelected.setCountryForNameCode("en");
            bindNetworkState(
                viewModelProfile.updateUserLanguageModel(
                    viewModel.roomId.value,
                    "English", "en", "US"
                ),
                progressDialog(com.android.vadify.R.string.sending)
            )
                speechToText("en-US")
            languageChange("en-US")
                languageCode = "en-US"
              //

            }
            if (viewModel.isSpeechLanguageEnable.value==ChatType.NORMAL)
            {

                viewModel.isSpeechLanguageEnable.value = ChatType.NORMAL
            }
            else{
                viewModel.isSpeechLanguageEnable.value = ChatType.SPEECH_TO_TEXT
            }
        })

        System.setProperty(GOOGLE_KEY, BuildConfig.GOOGLE_API_KEY)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        intent.extras?.getString(BaseBackStack.MESSAGE_ID)?.let {
            viewModel.readMessage(it)


        }
        intent.extras?.getString(BaseBackStack.ROOM_ID)?.let {
            roomIdChatFrag = it
            viewModel.updateRoomId(it)
        }
        intent.extras?.getString(BaseBackStack.ANOTHER_USER_NAME)?.let {
            viewModel.userName(it)
        }
        intent.extras?.getString(BaseBackStack.ANOTHER_USER_URL)?.let {
            viewModel.updateUrl(it)
        }
        intent.extras?.getString(BaseBackStack.ANOTHER_USER_ID)?.let {
            viewModel.anotherUserId = it
        }
        intent.extras?.getString(BaseBackStack.PHONE_NUMBER)?.let {
            viewModel.phoneNumber = it
        }

        intent.extras?.getString(BaseBackStack.MOTHER_LANGUAGE)?.let {
            viewModel.anotherUserMotherLanguage = it
        }
        intent.extras?.getBoolean(BaseBackStack.TYPE)?.let {
            viewModel.isGroup.value = it
        }

        intent.extras?.getString(BaseBackStack.GROUP_TYPE)?.let {
            viewModel.groupType.value = it

        }

        intent.extras?.getString(BaseBackStack.ANOTHER_USER_LANGUAGE_CODE)?.let {
            viewModel.anotherUserMotherLanguageCode = it

           // Log.d("another_code: ", it)
        }

        intent.extras?.getBoolean(BaseBackStack.TYPE)?.let {

            viewModel.isGroup.value = it

          //  Log.d("another_code: ", it)
        }

        intent.extras?.getBoolean(BaseBackStack.LANGUAGE_SWITCH)?.let {
            viewModel.anotherUserMotherSwitch = it
        }

        subscribe(backArrow.throttleClicks()) {
            isHasValue = false
            finish()
        }
        searchText.doOnTextChanged { text, _, _, _ ->
            viewModel.viewVisibility.value = !text.isNullOrBlank()
        }


        bindNetworkState(
            viewModel.networkState,
            onSuccess = { playAudio(viewModel.audioFile) },
            onError = {
                Log.d("playAudioGoogle", "came here")
                showMessage(resources.getString(com.android.vadify.R.string.speechRecoganizationProcess))
                speechToText(languageCode)


            })



        if (intent.extras?.getBoolean(BaseBackStack.FIRST_TIME_START_CHAT) == true) {
            Log.d("first-time: ", "" + viewModel.preferenceService.getString(com.android.vadify.R.string.pkey_user_Id))
            viewModel.isFirstTimeVisit = true
//            viewModel.updateRoomId(
//                viewModel.preferenceService.getString(R.string.pkey_user_Id) ?: ""
//            )
            bindNetworkState(viewModel.createRoomApi("Single","", listOf(viewModel.anotherUserId)))

            //viewModel.callChatOptions()
            viewModel.isSpeechLanguageEnable.value = ChatType.SPEECH_TO_TEXT
            speechToText("en-US")

        }




        (applicationContext as VadifyApplication).firstTimeRoomId.observe(
            this,
            androidx.lifecycle.Observer {

                Log.d("first-time-room: ", "" + it)
                if (viewModel.isFirstTimeVisit && !it.isNullOrBlank()) {
                    viewModel.updateRoomId(it)
                }

//            if (it.isNullOrEmpty())
//                return@Observer
//
//            val workInfo = it[0]
//            when {
//                workInfo.state.isFinished -> {
//                    Log.e("infoamtionae", "" + workInfo.outputData)
//                }
//                else -> {
//                    Log.e("infoamtionae", "progress")
//                }
                //           }
            })

    }

    private fun playAudio(base64EncodedString: String) {
        try {
            isListenCall = true
            speech?.cancel()
            stopSpeech()
            stopAudio()
            muteBeepSoundOfRecorder(false, AudioManager.ADJUST_UNMUTE)
            val url = "data:audio/mp3;base64,$base64EncodedString"
            mMediaPlayer = MediaPlayer()
            mMediaPlayer?.setDataSource(url)
            mMediaPlayer?.prepare()
            mMediaPlayer?.start()
            //val duration = TimeUnit.MILLISECONDS.toSeconds(mMediaPlayer!!.duration.toLong())
            mic.setImageResource(com.android.vadify.R.drawable.ic_mic)
            mic.clearAnimation()
            mMediaPlayer?.setOnCompletionListener {
              //  Log.d("TAGdsidsdsi", "onCompletion: ")
                mic.setImageResource(com.android.vadify.R.drawable.ic_mic_red)
                speechToText(languageCode)
                isAudioOn = false
                isListenCall = false
            }
            //  speakSuccess(mMessage)
        } catch (IoEx: IOException) {
            isListenCall = false
            isAudioOn = false
            showMessage(this.resources.getString(com.android.vadify.R.string.speechRecoganizationProcess))
            // speakFail(mMessage, IoEx)
        }
    }

    private fun stopAudio() {
        if (mMediaPlayer != null && mMediaPlayer?.isPlaying == true) {
            mMediaPlayer?.stop()
            mMediaPlayer?.reset()
        }
    }

    override fun onBindView(binding: ActivityChatBinding) {
        binding.vm = viewModel
    }

    private fun commandDrivenOperation(result: String) {

        Log.i("maya-command", result)
        val language = defaultSharedPreferences.getString("pkey_motherLanguage", "")

        //Log.d(TAG, "commandDrivenOperation: ")

        // Toast.makeText(this, language, Toast.LENGTH_LONG).show()

        if (viewModel.isLanguageSwitch.value ==true) {
            when {
                checkcommands(
                    result,
                    "MAYA SEND"
                ) || viewModel.sendCommand(result, "MAYA SEND") -> sendListeningData.performClick()
                // viewModel.sendCommand(result) -> sendListeningData.performClick()


                checkcommands(
                    result,
                    "MAYA OFF"
                ) || viewModel.stopTypeCommand(
                    result,
                    "MAYA OFF"
                ) -> {
                    viewModel.isSpeechLanguageEnable.value =

                        ChatType.NORMAL //viewModel.isStopTyping = true
                    listening_input.setText("")
                }
                checkcommands(result, "MAYA CLOSE") || viewModel.closeChatCommand(
                    result,
                    "MAYA CLOSE"
                ) -> finish()
                checkcommands(
                    result,
                    "MAYA BYE"
                ) || viewModel.closeApplicationCommand(result, "MAYA BYE") -> finishAffinity()
                checkcommands(result, "MAYA LISTEN") || viewModel.readLastMessageCommand(
                    result,
                    "MAYA LISTEN"
                ) -> {
                    if (adapter.currentList.isNullOrEmpty()) {
                        return
                    }

                    isAudioOn = true
                    if (speech != null) {
                        speech?.stopListening()
                        speech?.cancel()
                    }
                    // stopSpeech()
                    var commandListion: String = ""

                    if (adapter.currentList?.get(0)?.from_id.equals(
                            viewModel.preferenceService.getString(com.android.vadify.R.string.pkey_user_Id)
                                ?: ""
                        )
                    ) {
                        commandListion = adapter.currentList?.get(0)?.messageSender.toString()
                    } else {
                        commandListion = adapter.currentList?.get(0)?.messageReceiver.toString()

                    }
                    commandListion.let {

                        viewModel.messageText.value = it
                        viewModel.googleTextToSpeech(it)

                    }

                }
                !viewModel.isStopTyping && viewModel.isSpeechLanguageEnableOrDisable() == ChatType.SPEECH_TO_TEXT -> {
                    viewModel.stopTyping(result)

                }
            }

        } else {
            when {
                viewModel.mayaSendEnglish(result) -> sendListeningData.performClick()

                viewModel.mayaOffEnglish(
                    result
                ) -> {
                    viewModel.isSpeechLanguageEnable.value =
                        ChatType.NORMAL
                    listening_input.setText("")
                }
                viewModel.mayaCloseEnglish(
                    result
                ) -> finish()
                viewModel.mayaByeEnglish(result) -> finishAffinity()
                viewModel.mayaListenEnglish(
                    result
                ) -> {

                    speech?.cancel()
                    stopSpeech()
                    var commandListion: String = ""

                    if (adapter.currentList?.get(0)?.from_id.equals(
                            viewModel.preferenceService.getString(com.android.vadify.R.string.pkey_user_Id)
                                ?: ""
                        )
                    ) {
                        commandListion = adapter.currentList?.get(0)?.messageSender.toString()
                    } else {
                        commandListion = adapter.currentList?.get(0)?.messageReceiver.toString()

                    }
                    commandListion.let {

                        viewModel.messageText.value = it
                        viewModel.googleTextToSpeech(it)

                    }
                }
                !viewModel.isStopTyping && viewModel.isSpeechLanguageEnableOrDisable() == ChatType.SPEECH_TO_TEXT -> {
                    viewModel.stopTyping(result)
                }
            }

        }


    }

    fun checkcommands(result: String, mainCmd: String): Boolean {
        val id = commanData.indexOfFirst {
            it.command1?.toLowerCase().equals(result.toLowerCase()) || it.command2?.toLowerCase()
                .equals(result?.toLowerCase()) || it.command3?.toLowerCase()
                .equals(result?.toLowerCase())
        }
        Log.d("checkcommands---: ", "$id $mainCmd $result")
        if (id > -1) {

            if (commanData[id].commandName.equals(mainCmd)) {
                return true
            }
        }
        return false
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        when {
            requestCode == VIDEO_REQUEST_CODE && resultCode == Activity.RESULT_OK -> {
                responseCall(data, MessageType.VIDEO.value, FileUploadRepositry.VIDEO_FILE)
            }
            requestCode == CAMERA_REQUEST_CODE && resultCode == Activity.RESULT_OK -> {
                responseCall(data, MessageType.IMAGE.value, FileUploadRepositry.IMAGE_FILE)
            }
            requestCode == SELECT_PHONE_NUMBER && resultCode == Activity.RESULT_OK -> {
                contactData(data)
            }
            requestCode == FilePickerConst.REQUEST_CODE_DOC && resultCode == Activity.RESULT_OK && data != null -> {
                val list = data.getParcelableArrayListExtra<Uri>(FilePickerConst.KEY_SELECTED_DOCS)
                if (!list.isNullOrEmpty()) {
                    data.getParcelableArrayListExtra<Uri>(FilePickerConst.KEY_SELECTED_DOCS)?.get(0)
                        ?.let {
                            getPath(it)?.let {
                                viewModel.uploadFileOnBackground(
                                    it,
                                    "",
                                    MessageType.DOCUMENT.value,
                                    FileUploadRepositry.DOCUMENT_FILE
                                )
                            }
                        }
                }
            }
        }
    }


    private fun contactData(data: Intent?) {
        data?.data?.let { uri ->
            val cursor = contentResolver.query(uri, null, null, null, null)
            cursor?.moveToFirst()
            cursor?.let {
                val name =
                    it.getString(it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME))
                val phone =
                    it.getString(it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER))
                viewModel.sendContactToUser(name, phone)
            }
        }
    }

    private fun responseCall(data: Intent?, type: String, fileType: String) {
        data?.let {
            it.getStringExtra(CameraActivity.URL)?.let { url ->
                it.getStringExtra(CameraActivity.MESSAGE)?.let { message ->
                    when {
                        url.isBlank() && message.isBlank() -> {
                        }
                        url.isBlank() && message.isNotBlank() -> sendSocketData(message)
                      /*  (checkFileSize(File(url).length())) -> viewModel.uploadFileOnBackground(
                            url,
                            message,
                            type,
                            fileType
                        )*/
                        else -> viewModel.uploadFileOnBackground(
                            url,
                            message,
                            type,
                            fileType
                        )
                    }
                }
            }
        }
    }

    private fun callSpeechListener() {

        viewModel.callChatOptions()
        viewModel.isSpeechLanguageEnable.value = ChatType.SPEECH_TO_TEXT
        /*when {
        Speech.getInstance()?.isListening ?: false -> {
            viewModel.isSilentMode = 0
            stopSpeechRecoganization()
        }
        else ->*/
      //  Log.d("sitch-on", "" + viewModel.isManuallyMotherLangeSwitch())
        speechToText(languageCode)


        // callTimerCountMethod()
        //}
    }


    override fun onResume() {
        super.onResume()
        Log.d("roommmytdft: ","click1")
        //NotificationManagerCompat.from().cancelAll()
        firstCall = false
        if (viewModel.isGroup.value!!) {
            Log.d("roommmytdft: ","click1")
            viewModel.getMemberName(roomIdChatFrag)
        }
        if (!roomIdChatFrag.isNullOrEmpty()) {
            Log.d("roommmytdft: ","click")
            getUserLanguages(roomIdChatFrag)
        }
            else{
            Log.d("roommmytdft: ",roomIdChatFrag)
            countryLanguageSelected.setCountryForNameCode("en");
            commandViewModel.getCommandAll("English (English)")
        }
        commandViewModel.data.observe(this, androidx.lifecycle.Observer {
            commanData.clear()
            commanData.addAll(it)
            Log.d("calling-",Gson().toJson(it))
            if (!commanData.isEmpty()) {

                viewModel.isLanguageSwitch.postValue(true)

            }
            else{
                viewModel.isLanguageSwitch.postValue(false)
            }

            if (viewModel.isBlockedUserOrNot(this).value == false) {
                if (intent.extras?.getBoolean(BaseBackStack.GOTO_SPEECH_TO_TEXT) == true && !isHasValue) {
                    //


                    intent.extras?.putBoolean(BaseBackStack.GOTO_SPEECH_TO_TEXT, false)


                    if (!commanData.isEmpty()) {
                        //viewModel.callChatOptions()
                       //z viewModel.isSpeechLanguageEnable.value = ChatType.SPEECH_TO_TEXT

                         speechToText(languageCode)

                        //isHasValue = true
                    } else {
                        //  viewModel.callChatOptions()
                        listening_input.setText("")
                        viewModel.isSpeechLanguageEnable.value = ChatType.NORMAL


                        val skiplanguage =
                            defaultSharedPreferences.getString("pkey_skip_language", "")

                      /*  if (skiplanguage.equals("")) {
                           // withButtonCentered()
                            viewModel.callChatOptions()
                        } else {
                            Log.d("roommmytdft: ","click12")*/
                            viewModel.isSpeechLanguageEnable.value = ChatType.SPEECH_TO_TEXT

                            // speechToText(languageCode)
                       // }
                        /* supportFragmentManager.beginTransaction()
                         .replace(R.id.command_container, CommandsFragment(), "COMMAND_FRAG")
                         .addToBackStack(null).commit()*/
                    }
                    isHasValue = false
                }
            }
            else{
                viewModel.isSpeechLanguageEnable.value = ChatType.NORMAL
            }
        })


        VadifyApplication.messageList.get(viewModel.phoneNumber)?.clear()
        NotificationManagerCompat.from(this).deleteNotificationChannel(viewModel.phoneNumber)
        viewModel.updateChatActivity(viewModel.phoneNumber)
        connectMethod()
        uploadImage()
        viewModel.joinLeaveStatus(JOIN_ONLINE_STATUS)
        // viewModel.markChatAsRead()


    viewModel.isBlockedUserOrNot(this).observe(this, androidx.lifecycle.Observer {
        Log.d("block-visibility", "" + it)
        if (it!!) {
            //ln.visibility == View.GONE
            videoCall.visibility = View.INVISIBLE
            callBtnT.visibility == View.GONE
            this.cardView12.visibility = View.GONE
            this.cameraView.visibility = View.GONE
            this.cardView10.visibility = View.GONE
            this.plusBtn.visibility = View.GONE
            //this.listening_layout.visibility = View.GONE

            txtMsg.visibility = View.VISIBLE
        } else {
            //  ln.visibility == View.VISIBLE
            videoCall.visibility = View.VISIBLE
            callBtnT.visibility == View.VISIBLE
            txtMsg.visibility = View.GONE
            this.cardView12.visibility = View.VISIBLE
            this.cameraView.visibility = View.VISIBLE
            this.cardView10.visibility = View.VISIBLE
            this.plusBtn.visibility = View.VISIBLE
        }

       // textView41.setText(viewModelChat.name.)
    })



        viewModel.updateData()
    }

    override fun onPause() {
        super.onPause()
        stopVoiceRecord()
        viewModel.updateChatActivity("")
        adapter.stopAudio()
        stopSpeechRecoganization()
    }


    private fun recordAudio() {
        viewModel.voiceRecordingPath =
            getAudioPath(this.getFilePath(LocalStorage.DOWNLOAD_AUDIO_FILE_PATH))
        mRecorder = MediaRecorder()
        mRecorder?.setAudioSource(MediaRecorder.AudioSource.MIC)
        mRecorder?.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
        mRecorder?.setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
        mRecorder?.setAudioSamplingRate(16000);
        mRecorder?.setAudioChannels(1);
        mRecorder?.setOutputFile(viewModel.voiceRecordingPath)
        try {
            mRecorder?.prepare()
        } catch (e: IOException) {
        }
        mRecorder?.start()
    }

    private fun audioRecordingView() {
        when {
            isAllGranted(
                Permission.WRITE_EXTERNAL_STORAGE,
                Permission.READ_EXTERNAL_STORAGE
            ) -> startVoiceRecord()
            else -> runWithPermissions(
                Permission.WRITE_EXTERNAL_STORAGE,
                Permission.READ_EXTERNAL_STORAGE
            ) {
                startVoiceRecord()
            }
        }
    }

    private fun releaseRecording() {
        try {
            mRecorder?.stop()
            mRecorder?.release()
            mRecorder = null
        } catch (e: Exception) {
        }
    }

    private fun startVoiceRecord() {
        voiceRecordTimerCountMethod()
        stopSpeechRecoganization()
        viewModel.callChatOptions()
        viewModel.isSpeechLanguageEnable.value = ChatType.VOICE_REOCRDING
        recordAudio()
    }

    public fun stopVoiceRecord() {
        voiceTimer?.cancel()
        voiceTimer = null
        viewModel.isSpeechLanguageEnable.value = ChatType.NORMAL
        releaseRecording()
    }

    private fun voiceRecordTimerCountMethod() {
        if (voiceTimer == null) {
            AnimationUtils.loadAnimation(applicationContext, com.android.vadify.R.anim.blink_image)
                .also {

                    audioMic.startAnimation(it)
                }
            //  mic.setImageResource(R.drawable.ic_mic_red)
            voiceTimer = object : CountDownTimer(TIMER, DELAY) {
                override fun onTick(millisUntilFinished: Long) {
                    viewModel.voiceCounter.value = (millisUntilFinished / 1000).toInt()
                }

                override fun onFinish() {
                    voiceTimer?.cancel()
                    voiceTimer = null
                    viewModel.voiceCounter.value = 0
                    audioRecordingSendBtn.performClick()
                }
            }
            voiceTimer?.start()
        }
    }

    private fun loadJSONFromAsset(): ArrayList<LocalLanguageModel.LocalLanguageModelItem>? {
        var listOfLangauge: ArrayList<LocalLanguageModel.LocalLanguageModelItem>? = null
        try {
            val json = assets.open(PersonalInformationFragment.LOCAL_FILE).bufferedReader()
                .use { it.readText() }
            listOfLangauge = Gson().fromJson(
                json,
                object : TypeToken<List<LocalLanguageModel.LocalLanguageModelItem>?>() {}.type
            ) as ArrayList<LocalLanguageModel.LocalLanguageModelItem>?
        } catch (e: Exception) {
        }
        return listOfLangauge
    }

    private fun updateLocation() {
        when {
            isAllGranted(
                Permission.ACCESS_FINE_LOCATION,
                Permission.ACCESS_COARSE_LOCATION
            ) -> callApiMethod()
            else -> runWithPermissions(
                Permission.ACCESS_FINE_LOCATION,
                Permission.ACCESS_COARSE_LOCATION
            ) { callApiMethod() }
        }
    }

    private fun callApiMethod() {
        when {
            (isNetworkEnabled() || isGPSEnabled()) -> viewModel.sendLocationToUser()
            else -> locationSnackMessage()
        }
    }

    private fun callNavigation(navigationClass: Class<*>) {
        val intent = Intent(this, navigationClass).also {
           // Log.d( "another: ", viewModel.anotherUserId +" room "+viewModel.roomId.value.toString())

            it.putExtra(CallActivity.ANOTHER_USERID, viewModel.anotherUserId)
            it.putExtra(ROOM_ID, viewModel.roomId.value.toString())
            it.putStringArrayListExtra(MEMBER_IDs, ArrayList(viewModel.memberIds))
            it.putExtra(BaseBackStack.GROUP_TYPE, viewModel.isGroup.value)
            it.putExtra(CallActivity.ANOTHER_USER_NAME, viewModel.userName.value)
            it.putExtra(Constants.SENDER_OR_RECEIVER, CallType.SENDER.value)
            it.putExtra(CallActivity.ANOTHER_USER_IMAGE, viewModel.userUrl.value)



          /*  if (getArgsString(BaseBackStack.GROUP_TYPE) == "Single") {

                it.putExtra(CallActivity.ANOTHER_USER_NAME, viewModel.userName.value)
                it.putExtra(BaseBackStack.GROUP_TYPE, false)
                it.putExtra(CallActivity.ANOTHER_USER_IMAGE, viewModel.profileImage.value)
            }
            else{
                it.putExtra(BaseBackStack.GROUP_TYPE, true)
                it.putExtra(CallActivity.ANOTHER_USER_NAME, viewModel.userNameGroup.value)

                it.putExtra(CallActivity.ANOTHER_USER_IMAGE, viewModel.profileImageGroup.value)
            }*/

            // it.putExtra("fromNotify",false)
        }
        startActivity(intent)
    }

    open fun getErrorText(errorCode: Int): String {
        return when (errorCode) {
            SpeechRecognizer.ERROR_AUDIO -> "Audio recording error"
            SpeechRecognizer.ERROR_CLIENT -> "Client side error"
            SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Insufficient permissions"
            SpeechRecognizer.ERROR_NETWORK -> "Network error"
            SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "Network timeout"
            SpeechRecognizer.ERROR_NO_MATCH -> "No match"
            SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "RecognitionService busy"
            SpeechRecognizer.ERROR_SERVER -> "error from server"
            SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "No speech input"
            else -> "Didn't understand, please try again."
        }
    }

    private fun openReplyView() {

        reply_container_view.visibility = View.VISIBLE

        viewModel.replyMessage.value?.let {

            if (it.from_id == viewModel.getUserId()) {
                reply_message_user.text = "You"
            } else {
                reply_message_user.text = it.from_name
            }

            reply_image.visibility = View.VISIBLE
            when {
                it.type.equals(MessageType.IMAGE.value, true) -> {
                    reply_text_view.text = "Image"
                    reply_image.imageUrlWithoutPlaceHolder(it.url)
                }

                it.type.equals(MessageType.VIDEO.value, true) -> {
                    reply_text_view.text = "Video"
                    reply_image.imageUrlWithoutPlaceHolder(it.url)
                }

                it.type.equals(MessageType.DOCUMENT.value, true) -> {
                    reply_text_view.text = "Document"
                    reply_image.setImageDrawable(com.android.vadify.R.drawable.icon_file_doc)
                }

                it.type.equals(MessageType.CONTACT.value, true) -> {
                    reply_text_view.text = "Contact"
                    reply_image.setImageDrawable(com.android.vadify.R.drawable.ic_chat_contact)
                }

                it.type.equals(MessageType.LOCATION.value, true) -> {
                    reply_text_view.text = "Location"
                    reply_image.setImageDrawable(com.android.vadify.R.drawable.ic_map_address)
                }

                it.type.equals(MessageType.AUDIO.value, true) -> {
                    reply_text_view.text = "Audio"
                    reply_image.setImageDrawable(com.android.vadify.R.drawable.ic_audio_play_icon)
                }

                else -> {
                   // val myMessage =  viewModel.replyMessage.value?.members?.filter { it->it1.userId == viewModel.preferenceService.getString(R.string.pkey_user_Id) }
                    val myMessage = viewModel.replyMessage.value?.members?.filter { it.userId == viewModel.preferenceService.getString(
                        com.android.vadify.R.string.pkey_user_Id) }

                    viewModel.replyMessage.value?.members?.firstOrNull()?.let {
                        reply_image.visibility = View.GONE
                        if (it.userId == viewModel.getUserId() && viewModel.replyMessage.value?.from_id!!.equals(viewModel.preferenceService.getString(
                                com.android.vadify.R.string.pkey_user_Id))) {
                            reply_text_view.text = EncryptionViewModel.decryptString(myMessage?.get(0)?.message)
                        } else {
                            reply_text_view.text = EncryptionViewModel.decryptString(myMessage?.get(0)?.message)
                        }
                    }
                }
            }
        }


        searchText.requestFocus()

        //replyToMessageId
        //replyToMessageText
    }

    private fun closeReplyView() {
        reply_container_view.visibility = View.GONE
    }

    fun deleteMessageConfirmation(chat: Chat) {
        val alertDialog = AlertDialog.Builder(this).create()
        alertDialog.setTitle(resources.getString(R.string.app_name))
        alertDialog.setMessage(resources.getString(R.string.delete_message_confirmation))

        alertDialog.setButton(
            AlertDialog.BUTTON_POSITIVE, resources.getString(R.string.yes)
        ) { dialog, which ->
            bindNetworkState(
                viewModel.deletMessage(chat),
                progressDialog(R.string.deleting)
            )
            dialog.dismiss()
        }

        alertDialog.setButton(
            AlertDialog.BUTTON_NEGATIVE, resources.getString(R.string.no)
        ) { dialog, which -> dialog.dismiss() }
        alertDialog.show()

        val btnPositive = alertDialog.getButton(AlertDialog.BUTTON_POSITIVE)
        val btnNegative = alertDialog.getButton(AlertDialog.BUTTON_NEGATIVE)

        val layoutParams = btnPositive.layoutParams as LinearLayout.LayoutParams
        layoutParams.weight = 10f
        btnPositive.layoutParams = layoutParams
        btnNegative.layoutParams = layoutParams
        alertDialog.getButton(AlertDialog.BUTTON_POSITIVE)
            .setTextColor(resources.getColor(R.color.blue));
        alertDialog.getButton(AlertDialog.BUTTON_NEGATIVE)
            .setTextColor(resources.getColor(R.color.blue));
    }

    fun forwardMessage(forwardedMessage: Chat, userIds: ArrayList<String>) {
        viewModel.isGroup.value=false
        if (userIds.size == 1) {
            viewModel.forward(forwardedMessage, userIds.first()) { success: Boolean, chat: Chat? ->
                if (success) {

                    finish()
                    chat?.let {
                        RxBus.publish(it.roomId)
                    }
                }
            }
            return
        }

        bindNetworkState(
            viewModel.forwardMessage(forwardedMessage, userIds),
            progressDialog(R.string.sending)
        )
    }


    fun withButtonCentered() {

        val language = defaultSharedPreferences.getString("pkey_motherLanguage", "")



        viewModel.callChatOptions()

        val alertDialog = AlertDialog.Builder(this).create()
        alertDialog.setTitle(resources.getString(R.string.app_name))
        alertDialog.setMessage(
            resources.getString(R.string.voice_command_setup_pop)
                    + "\n\n" + resources.getString(R.string.voice_command_setup_pop2)
        )

        alertDialog.setButton(
            AlertDialog.BUTTON_POSITIVE, resources.getString(R.string.record_now)
        ) { dialog, which ->
            dialog.dismiss()
            val bundle = Bundle().apply {
                putString("RETAKE", "NO")
            }
             var fragment  = CommandsFragment()
            fragment.arguments = bundle
            supportFragmentManager.beginTransaction()
                .replace(R.id.command_container, fragment, "COMMAND_FRAG")
                .addToBackStack(null).commit()
        }

        alertDialog.setButton(
            AlertDialog.BUTTON_NEGATIVE, resources.getString(R.string.skip_now)
        ) { dialog, which ->
            dialog.dismiss()
            val edit = defaultSharedPreferences.edit()
            edit.putString("pkey_skip_language", "Yes")
            edit.commit()
            viewModel.isSpeechLanguageEnable.value = ChatType.SPEECH_TO_TEXT
            speechToText(languageCode)
        }
        alertDialog.show()

        val btnPositive = alertDialog.getButton(AlertDialog.BUTTON_POSITIVE)
        val btnNegative = alertDialog.getButton(AlertDialog.BUTTON_NEGATIVE)

        val layoutParams = btnPositive.layoutParams as LinearLayout.LayoutParams
        layoutParams.weight = 10f
        btnPositive.layoutParams = layoutParams
        btnNegative.layoutParams = layoutParams
        alertDialog.getButton(AlertDialog.BUTTON_POSITIVE)
            .setTextColor(resources.getColor(R.color.blue));
        alertDialog.getButton(AlertDialog.BUTTON_NEGATIVE)
            .setTextColor(resources.getColor(R.color.blue));
    }

    private fun getAllLanguages() {
        bindNetworkState(
            viewModelProfile.getAllLanguageResponseMo()
        )
    }

    private fun getUserLanguages(roomid: String) {

        bindNetworkState(
            viewModelChat.getUserLanguage(roomid)
        )

    }



}