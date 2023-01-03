package com.android.vadify.ui.chat.viewmodel

import android.annotation.SuppressLint
import android.app.Application
import android.content.Context
import android.content.Context.INPUT_METHOD_SERVICE
import android.os.Build
import android.util.Base64
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.work.*
import com.android.vadify.BuildConfig
import com.android.vadify.R
import com.android.vadify.data.api.enums.ChatType
import com.android.vadify.data.api.enums.MessageType
import com.android.vadify.data.api.models.*
import com.android.vadify.data.db.chat.Chat
import com.android.vadify.data.db.chat.ChatListCache
import com.android.vadify.data.db.chatThread.ChatThread
import com.android.vadify.data.db.chatThread.ChatThreadCache
import com.android.vadify.data.repository.ChatRepository
import com.android.vadify.data.repository.LocationRepository
import com.android.vadify.data.repository.UserRepository
import com.android.vadify.data.service.PreferenceService
import com.android.vadify.ui.baseclass.BaseBackStack
import com.android.vadify.ui.chat.ChatActivity
import com.android.vadify.ui.chat.ChatActivity.Companion.ACKNOWLEDGE_MESSAGE
import com.android.vadify.ui.chat.ChatActivity.Companion.MESSAGE
import com.android.vadify.ui.chat.ChatActivity.Companion.OFFLINE
import com.android.vadify.ui.chat.ChatActivity.Companion.PAGE_COUNT
import com.android.vadify.ui.chat.ChatActivity.Companion.TOTAL_ITEMS
import com.android.vadify.ui.chat.ChatActivity.Companion.TYPING
import com.android.vadify.ui.util.PagedListViewModel
import com.android.vadify.utils.CountryCodeSelector
import com.android.vadify.viewmodels.EncryptionViewModel
import com.google.cloud.translate.Translate
import com.google.cloud.translate.TranslateOptions
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.sdi.joyersmajorplatform.common.livedataext.mutableLiveData
import com.sdi.joyersmajorplatform.uiview.NetworkState
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.rxkotlin.subscribeBy
import io.reactivex.schedulers.Schedulers
import io.socket.client.Ack
import io.socket.client.IO
import io.socket.client.Socket
import io.socket.engineio.client.transports.WebSocket
import org.json.JSONException
import org.json.JSONObject
import java.io.File
import java.net.URISyntaxException
import java.util.*
import javax.inject.Inject
import kotlin.math.log
import android.view.inputmethod.InputMethodSubtype

import android.view.inputmethod.InputMethodInfo
import android.view.inputmethod.InputMethodManager
import androidx.core.content.ContextCompat

import androidx.core.content.ContextCompat.getSystemService
import androidx.core.content.ContextCompat.getSystemService
import androidx.lifecycle.viewModelScope
import com.android.vadify.ui.chat.ChatActivity.Companion.GROUP_MESSAGE
import com.sdi.joyersmajorplatform.common.progressDialog
import kotlinx.coroutines.*
import java.util.concurrent.TimeUnit


class ChatViewModel @Inject constructor(
    val preferenceService: PreferenceService,
    private val chatRepository: ChatRepository,
    var chatListCache: ChatListCache,
    private val application: Application,
    var mSocket: Socket?,
    private val locationRepository: LocationRepository,
    private val userRepository: UserRepository,
    private val chatThreadCache: ChatThreadCache
) : TextToSpeechModel() {

    //    var mSocket: Socket? = null
    var anotherUserId: String = ""
    var phoneNumber: String = ""

    var anotherUserMotherLanguage: String = ""

    var anotherUserMotherLanguageCode: String = ""
    var anotherUserMotherSwitch = true
    var showDate = mutableLiveData("")
    var viewVisibility = mutableLiveData(false)
    var userUrl = mutableLiveData("")
    var userName = mutableLiveData("")
    var chatOption = mutableLiveData(false)
    var isLanguageSwitch = mutableLiveData(true)
    var isSpeechLanguageEnable = mutableLiveData(ChatType.SPEECH_TO_TEXT)
    var speechLanguageText = mutableLiveData("")
    var defaultKeywords: MutableLiveData<List<Command>> = MutableLiveData()
    var membersName = mutableLiveData("Tap here for more info")
    var onlineOfflineTypingLabel = mutableLiveData(OFFLINE)
    var isGroup = mutableLiveData(false)
    var groupType: MutableLiveData<String> = mutableLiveData("")
    var memberIds :List<String> = emptyList()

    //var onlineOfflineTypingLabel = mutableLiveData(OFFLINE)
    //var onlineOfflineTypingLabel = mutableLiveData(OFFLINE)
    var tempStatus = ""

    var isFirstTimeVisit = false
    var isStopTyping = false
    var isSilentMode = 0
    var roomId = MutableLiveData<String>()
    var smoothScrolling = mutableLiveData(false)
    var workerManager = WorkManager.getInstance(application)
    val backgroundDataResult: LiveData<List<WorkInfo>>
    var voiceCounter = mutableLiveData(0)
    var isMediaFile = mutableLiveData(false)
    var voiceRecordingPath = ""

    /// var localLanguage: ArrayList<AllLanguageResponse.Data>? = nul

    private var forwardingMessage = MutableLiveData<NetworkState>()

    var replyMessage = MutableLiveData<Chat>()

    init {
        backgroundDataResult = workerManager.getWorkInfosByTagLiveData("OUTPUT")
    }

    public fun getKeyForMotherLanguageSwitch(): String {
        return application.resources.getString(R.string.pkey_language_switch)
    }

    fun isManuallyMotherLangeSwitch() =
        preferenceService.getBoolean(getKeyForMotherLanguageSwitch(), true)

    @SuppressLint("CheckResult")
    fun switchMotherLanguageButton(condition: Boolean) {
        preferenceService.putBoolean(getKeyForMotherLanguageSwitch(), condition)

        userRepository.updateLanguageSwitchState(condition).subscribeOn(Schedulers.io()).observeOn(
            AndroidSchedulers.mainThread()
        ).subscribeBy(
            onSuccess = {
                print(it)
            },
            onError = {
                print(it)
            })
    }

    private fun getReceiverLanguage(): String? {
        val language = chatThreadCache.getChatThread(roomId.value!!).members.firstOrNull()?.language

        if (!language.isNullOrEmpty()) {

            if (language == "English") return "en" //TODO: Remove this line

            return language
        }
        return null
    }

    fun getMemberName(anotherUserId : String) {
        val members = chatThreadCache.getChatThread(anotherUserId).members
        memberIds = members.map { it.name+","+it.userId }
        val users = members.map {
            it.name
        }
        var namesOfMem = ""
        for (user in users) {
            namesOfMem += "$user,"
        }
        membersName.value = namesOfMem
    }

    fun updateData(): LiveData<NetworkState> {
        val networkRequest = userRepository.updateProfile(getProfileRequestModel())
        subscribe(networkRequest.request) {

            // Log.d("auth_id: ",""+ preferenceService.getString(R.string.pkey_secure_token))

            // Log.i("update-profile", "" + Gson().toJson(it.body()))
            if (it.body()!!.data.commands != null) {
                defaultKeywords.postValue(it.body()!!.data.commands.commands)
                preferenceService.putBoolean(R.string.pkey_needToUpdateNotificationData, false)
            }
        }
        return networkRequest.networkState
    }
    fun createRoomApi(
        groupName: String,
        groupPic: String?,
        membersIds: List<String>
    ): LiveData<NetworkState> {

        val networkRequest = userRepository.createGroupCallRequest(
            CreateGroupRequest(
                groupName,
                groupPic,
                membersIds,
                "Single"
            )
        )
        //Log.d("member_id", "" + membersIds)
        //  val networkRequest = userRepository.updateProfile(uploadData(getProfileRequestModel()))
        subscribe(networkRequest.request) {
            roomId.postValue(it.body()?.data?._id)
            updateUserLanguageModel(
                    it.body()?.data?._id ,
                    "English", "en", "US"
                )
        }
        return networkRequest.networkState
    }


    /*fun createAwsUrlApi(
        type: String,
        contentType: String?,
        fileName: String?
    ): LiveData<NetworkState> {

        val networkRequest = userRepository.createUploadRequest(
            UploadAwsURL(
                type,
                contentType,
                fileName
            )
        )
        subscribe(networkRequest.request) {

        }
        return networkRequest.networkState

    }*/



    fun updateUserLanguageModel(roomId: String?, language: String,languageCode: String?,countryCode: String?): LiveData<NetworkState> {
        val request = userRepository.updatedUserLanguage(roomId, UpdateLanguageRequest(language,languageCode,countryCode)).also { it ->
            subscribe(it.request)
            {
                if (it.isSuccessful)
                {
//                    preferenceService.putString(R.string.mother_language,it.body()?.data?.language)
//                    preferenceService.putString(R.string.pkey_motherLanguage_Code,it.body()?.data?.countryCode)
                }
            }
        }
        return request.networkState
    }
    private fun getProfileRequestModel(): ProfileUpdateRequestModel {
        return ProfileUpdateRequestModel(
            preferenceService.getString(R.string.pkey_emailId),
            preferenceService.getBoolean(R.string.pkey_groupNotification),
            preferenceService.getString(R.string.pkey_groupPermission),
            preferenceService.getBoolean(R.string.pkey_inAppSound),
            preferenceService.getBoolean(R.string.pkey_inAppVibrate),
            preferenceService.getString(R.string.pkey_motherLanguage),
            preferenceService.getBoolean(R.string.pkey_liveLocationShare),
            preferenceService.getBoolean(R.string.pkey_messageNoficaction),
            preferenceService.getString(R.string.pkey_userName),
            preferenceService.getString(R.string.pkey_profileImage),
            preferenceService.getString(R.string.pkey_status),
            preferenceService.getBoolean(R.string.pkey_saveToCameraRole),
            preferenceService.getBoolean(R.string.pkey_securityNotification),
            preferenceService.getBoolean(R.string.pkey_showPreview),
            preferenceService.getString(R.string.pkey_motherLanguage_Code)
        )
    }

    private val constraints = Constraints.Builder()
        .setRequiredNetworkType(NetworkType.CONNECTED)
        .build()

    fun getSocketInstance(): Socket? {
        try {
            //if (mSocket == null) {
            val opts = IO.Options()
            opts.transports = arrayOf(WebSocket.NAME)
            opts.forceNew = true
            opts.reconnection = true
            Log.d("socket-users: ", "" + preferenceService.getString(R.string.pkey_user_Id))
            mSocket = IO.socket(
                BuildConfig.API_URL + "?userId=" + preferenceService.getString(R.string.pkey_user_Id),
                opts
            )  //IO.socket("https://socketio-chat-h9jt.herokuapp.com") // IO.socket(BuildConfig.API_URL + "?userId="+ preferenceService.getString(R.string.pkey_user_Id))
            //    }
        } catch (e: URISyntaxException) {
            Log.e("message are", "" + e.message.toString())
        }
        return mSocket
    }

    fun updateDate(date: String) {
        showDate.value = date
    }

    fun isSocketConnected(): Boolean {
        return mSocket?.connected() ?: false
    }

    fun isSpeechLanguageEnableOrDisable(): ChatType? {
        return isSpeechLanguageEnable.value
    }

    fun getForwardingMessage(): LiveData<NetworkState> {
        return forwardingMessage
    }

    fun setForwardingMessage(firstName: NetworkState) {
        forwardingMessage.value = firstName
    }

    fun forwardMessage(forwardedMessage: Chat, userIds: ArrayList<String>): LiveData<NetworkState> {

        var userIdsMutable = userIds

        if (userIdsMutable.isEmpty()) {
            print("Sent messages to all ids")
            setForwardingMessage(NetworkState.success)
            return getForwardingMessage()
        }

        val userId = userIdsMutable.removeAt(0)
        setForwardingMessage(NetworkState.loading)

        forward(forwardedMessage, userId) { b: Boolean, chat: Chat? ->
            forwardMessage(forwardedMessage, userIdsMutable)
        }
        return getForwardingMessage()
    }

    fun forward(forwardedMessage: Chat, userId: String, completion: (Boolean, Chat?) -> Unit) {
        when (forwardedMessage.type) {

            MessageType.TEXT.value -> {
                messageBundle(
                    EncryptionViewModel.decryptString(forwardedMessage.members.get(0).message),
                    forwardedMessage.messageSender,
                    forwardedMessage.messageReceiver,
                    userId,
                    completion,
                    forwardedMessage.forwarded
                )
            }

            MessageType.LOCATION.value -> {

                val lat = forwardedMessage.lat_location?.toDoubleOrNull()
                val lng = forwardedMessage.long_location?.toDoubleOrNull()

                if (lat != null && lng != null) {
                    sendMessage(
                        locationReqeust(lat, lng, userId, forwardedMessage.forwarded),
                        completion
                    )
                }
            }


            else -> sendMessage(
                createJSONForForward(
                    forwardedMessage,
                    forwardedMessage.type,
                    userId
                ), completion
            )
        }
    }



    private fun sendMessage(
        messageBundle: JSONObject,
        completion: ((Boolean, Chat?) -> Unit)? = null
    ) {
//        Log.d("payload-fordward-: ", Gson().toJson(messageBundle))
        val message = messageBundle
        val gson = GsonBuilder().disableHtmlEscaping().create()
        val json = gson.toJson(message)
        val finalPayLoad = EncryptionViewModel.encryptString(message.toString())
        val finalData = JSONObject()
        finalData.put("data", finalPayLoad)
        Log.d("payload--: ", message.toString()+"\n"+json)

        var event ="";
        if (isGroup.value!!){
            event = GROUP_MESSAGE
        }
        else{
            event = MESSAGE
        }

        mSocket?.emit(event, finalData, object : Ack {
            override fun call(vararg args: Any?) {
                CoroutineScope(Dispatchers.Main).launch {
                    smoothScrolling.value = true
                    val data: JSONObject = args[0] as JSONObject
                    val encryptedPayload = data.getString("data")
                    val decryptedPayload = EncryptionViewModel.decryptString(encryptedPayload)
                    try {
                        val data: JSONObject = args[0] as JSONObject
                        Log.d("messageReceiver ","MsgSent  "+Gson().toJson(data))
                        Log.d("call--response", data.getString("statusCode"))
                        if (data.getString("statusCode").equals("200")) {
                            Gson().fromJson(
                                decryptedPayload,
                                MessageResponseList.Data.DataX::class.java
                            )?.let {

                                try {
                                    if (replyMessage.value != null) {
                                        replyMessage.value = null
                                    }
//                                    var encrytedData = it
//                                    encrytedData.members[0].message = EncryptionViewModel.decryptString(it.members[0].message)
//
                                    insertDataIntoDataBase(it)
                                    completion?.invoke(
                                        true,
                                        Chat.Mapper.from(it, isDecrypting = true)
                                    )
                                } catch (ex: Exception) {
                                    Log.e("not-inserted", ex.message.toString())
                                    completion?.invoke(false, null)
                                }

                            }
                        }
                    } catch (e: JSONException) {
                        Log.e("error-socket", e.message.toString())
                        completion?.invoke(false, null)
                    }
                }
            }
        })
    }


    fun joinLeaveStatus(joinOnlineStatus: String) {
        val request = statusJson()


        mSocket?.emit(joinOnlineStatus, request, object : Ack {
            override fun call(vararg args: Any?) {
                CoroutineScope(Dispatchers.Main).launch {
                    try {
                        val data: JSONObject = args[0] as JSONObject
                        data.getJSONObject("data").getString("status").let {
                            tempStatus = it
                            onlineOfflineTypingLabel.postValue(tempStatus)
                        }
                    } catch (e: Exception) {
                        Log.e("error message are", "" + e.message.toString())
                    }
                }
            }
        })
    }

    fun userTyping() {
        val request = userTypingJson()
        mSocket?.emit(TYPING, request, object : Ack {
            override fun call(vararg args: Any?) {
                CoroutineScope(Dispatchers.Main).launch {
                    val data: JSONObject = args[0] as JSONObject
                    Log.e("typing data", "" + data)
                }
            }
        })
    }

    fun updateRoomId(id: String) {
        roomId.postValue(id)
    }

    fun updateRoom(id: String) {
        roomId.postValue(id)
    }

    fun updateUrl(url: String) {
        userUrl.value = url
    }

    fun userName(url: String) {
        userName.value = url
    }

    // 60cb296dfebbf9005925d3dc: reciver
    //60ca3de1334202003a7a1ab8: msg send
    //60ca3de1334202003a7a1ab8: msg
    //60ca3de1334202003a7a1ab8: msg
    //60ca3de1334202003a7a1ab8
    //60ca3de1334202003a7a1ab8
    var getChatList = PagedListViewModel(roomId) {
        Log.d("rooom_id_1", "" + it)

        chatRepository.getChatMessagesFromAPI(it, PAGE_COUNT, TOTAL_ITEMS)

        chatRepository.getChatList(it, PAGE_COUNT, TOTAL_ITEMS, checkLanguageCode())

    }

    fun callChatOptions() {
        isMediaFile.value = false
        chatOption.value?.let { chatOption.value = !it }
    }

    fun callMediaOptions() {
        chatOption.value = false
        isMediaFile.value?.let { isMediaFile.value = !it }
    }


    fun insertDataIntoDataBase(it: MessageResponseList.Data.DataX, isDecrypting: Boolean = false) {
        CoroutineScope(Dispatchers.IO).launch {
            if (isFirstTimeVisit == true) {

                if (isDecrypting == false) {
                    updateRoomId(it.roomId)
                }
                //readMessage(it.roomId)
            }
            chatListCache.insert(Chat.Mapper.from(it, isDecrypting = isDecrypting))
        }
    }


    fun  sendOrListenMessage(
        text: String,
        isListening: Boolean = false,
        dataObject: MessageResponseList.Data.DataX? = null
    ) {
        Log.i("meg_sender1", " " + anotherUserMotherSwitch + "switch" + isLanguageSwitch.value!!)

        if (anotherUserMotherSwitch && isLanguageSwitch.value!!) {



            /*      translate(
                      text,
                      anotherUserMotherLanguage,
                      anotherUserMotherLanguageCode
                  ) { messageReceiver ->
                      //TODO: Handle error case where translated error returns null or empty string
                      messageReceiver?.let {

                          val lang =
                              preferenceService.getString(R.string.pkey_motherLanguage, "English")!!

                          translateUser(text,anotherUserMotherLanguageCode) {
                              //TODO: Handle error case where translated error returns null or empty string
                              it?.let { messageSender ->
                                  Log.i(
                                      "meg_sender1",
                                      " " + text + "send" + messageSender + "rec" + messageReceiver
                                  )*/
            messageBundle(text, "", "")
            /*      }
              }
          }
      }*/
        } else if (anotherUserMotherSwitch) {

            /* translate(text, anotherUserMotherLanguage, anotherUserMotherLanguageCode) {
                 //TODO: Handle error case where translated error returns null or empty string
                 it?.let {

                     Log.i("meg_sender2", " " + text + "send" + text + "rec" + it)
 */
            messageBundle(text, text, "")
            /*   }
           }*/
        } else if (isLanguageSwitch.value!!) {

            val lang = preferenceService.getString(R.string.pkey_motherLanguage, "English")!!

            /* translateUser(text,anotherUserMotherLanguageCode) {
                 //TODO: Handle error case where translated error returns null or empty string
                 it?.let {
                     Log.i("meg_sender3", " " + text + "send" + it + "rec" + text)*/
            messageBundle(text, "", text)
            /*   }
           }*/
        } else {
            Log.i("meg_sender4", " " + text + "send" + text + "rec" + text)
            messageBundle(text, text, text)
        }
    }

    fun translate(
        message: String,
        targetLanguage: String,
        anotherUserMotherLanguageCode: String,
        callBack: (String?) -> Unit
    ) {
        if (anotherUserMotherLanguageCode.substringBefore(
                "-"
            ).equals( preferenceService.getString(
                R.string.pkey_motherLanguage_Code,
                "en"
            )!!.substringBefore("-")))
        {
            callBack(message)
            return

        }
        CoroutineScope(Dispatchers.IO).launch {
            val contactList = async {

                /*  val filteredLanguage = localLanguage?.filter {
              it.name == targetLanguage
          }*/
                /*val locale: Locale =
            ConfigurationCompat.getLocales(Resources.getSystem().getConfiguration()).get(0)*/

                //Log.d( "other_user_language",locale.language)


                try {
                    TranslateOptions.getDefaultInstance().service?.let {
                        it.translate(
                            message,
                            Translate.TranslateOption.sourceLanguage(
                                preferenceService.getString(
                                    R.string.pkey_motherLanguage_Code,
                                    "en"
                                )!!.substringBefore("-")
                            ),
                            Translate.TranslateOption.targetLanguage(
                                anotherUserMotherLanguageCode.substringBefore(
                                    "-"
                                )
                            ),
                            Translate.TranslateOption.model("nmt")
                        )?.let {
                            val translatedText = it.translatedText
                            print(translatedText)
                            callBack(translatedText)
                        }
                    }
                } catch (e: Exception) {
                    Log.e("message---", "" + e.message.toString())
                    callBack(message)
                }
            }
        }

    }

    fun translateUser(message: String, anotherUserMotherLanguageCode:String,callBack: (String?) -> Unit) {
        if (anotherUserMotherLanguageCode.substringBefore(
                "-"
            ).equals( preferenceService.getString(
                R.string.pkey_motherLanguage_Code,
                "en"
            )!!.substringBefore("-")))
        {
            callBack(message)
            return
        }
        CoroutineScope(Dispatchers.IO).launch {
            val contactList = async {

                try {
                    Log.d(
                        "translate-language",
                        "" + preferenceService.getString(R.string.pkey_motherLanguage_Code, "en")
                    )
                    // Translate.TranslateOption.sourceLanguage("en"),
                    TranslateOptions.getDefaultInstance().service?.let {
                        it.translate(
                            message,
                            Translate.TranslateOption.sourceLanguage(anotherUserMotherLanguageCode.substringBefore(
                                "-"
                            )),
                            Translate.TranslateOption.targetLanguage(
                                preferenceService.getString(
                                    R.string.pkey_motherLanguage_Code,
                                    "en"
                                )!!.substringBefore("-")
                            ),
                            Translate.TranslateOption.model("nmt")
                        )?.let {
                            val translatedText = it.translatedText
                            print(translatedText)
                            callBack(translatedText)
                        }
                    }
                } catch (e: Exception) {
                    Log.e("message", "" + e.message.toString())
                    callBack(null)
                }

            }
        }
    }

    private fun receiveMessage(message: String, dataObject: MessageResponseList.Data.DataX?) {
        CoroutineScope(Dispatchers.Main).launch {
            dataObject?.let {
                it.message = message
                insertDataIntoDataBase(it)
            }
        }
    }

    fun receiveNewMessage(args: Array<out Any?>) {
        CoroutineScope(Dispatchers.Main).launch {
            try {
                smoothScrolling.value = false

                val data: String = args[0] as String
                val decryptedPayload = EncryptionViewModel.decryptString(data)


                Gson().fromJson(decryptedPayload, MessageResponseList.Data.DataX::class.java)?.let {
                    Log.d("message-recive: ","MsgReceived  "+Gson().toJson(it))
                  //if (roomId.value.isNullOrBlank())

                    if(it.roomId == roomId.value)

                        markChatAsRead(it.roomId)

                    insertDataIntoDataBase(it, true)
                }
                //60ca3de1334202003a7a1ab8
            } catch (e: JSONException) {
            }
        }
    }

    fun messageUpdated(args: Array<out Any?>) {
        CoroutineScope(Dispatchers.Main).launch {
            try {
                smoothScrolling.value = false

                var payLoad: String = ""
                if (args[0] is JSONObject) {
                    val data: JSONObject = args[0] as JSONObject
                    val encryptedPayload = data.getString("data")
                    payLoad = EncryptionViewModel.decryptString(encryptedPayload)

                } else {
                    val data: String = args[0] as String
                    val decryptedPayload = EncryptionViewModel.decryptString(data)

                    Gson().fromJson(decryptedPayload, MessageResponseList.Data.DataX::class.java)
                        ?.let {
                            insertDataIntoDataBase(it, true)
                        }
                }
                Gson().fromJson(payLoad, MessageResponseList.Data.DataX::class.java)?.let {
                    insertDataIntoDataBase(it, true)
                }
            } catch (e: JSONException) {
                print(e)
            }
        }
    }

    fun checkOnListStatus(args: Array<out Any?>) {
        CoroutineScope(Dispatchers.Main).launch {
            try {
                val data: JSONObject = args[0] as JSONObject
                tempStatus = data.getString("status")
                onlineOfflineTypingLabel.value = tempStatus
            } catch (e: JSONException) {
            }
        }
    }

    private fun messageBundle(
        message: String,
        messageSender: String ="",
        messageReceiver: String="",
        userId: String = anotherUserId,
        completion: ((Boolean, Chat?) -> Unit)? = null,
        isForwarded: Boolean = false
    ) {

        CoroutineScope(Dispatchers.Main).launch {
            sendMessage(
                createJson(
                    MessageType.TEXT.value,
                    message,
                    messageSender,
                    messageReceiver,
                    "",
                    userId,
                    isForwarded
                ),
                completion
            )
        }
    }

    fun updateMotherLanguageForUserId(userId: String, updatedLanguage: String) {

        CoroutineScope(Dispatchers.IO).launch {

            val updatedChatThreads = mutableListOf<ChatThread>()

            chatThreadCache.getAllChatThreadsSync().forEach { chatThread ->
                chatThread.members.forEach {
                    if (it.userId == userId) {
                        var members = chatThread.members

                        members.forEach {
                            if (it.userId == userId) {
                                it.language = updatedLanguage
                            }
                        }
                        chatThread.members = members
                        updatedChatThreads.add(chatThread)
                    }
                }
            }
            if (updatedChatThreads.isNotEmpty()) {
                chatThreadCache.insert(updatedChatThreads)
            }

            if (anotherUserId == userId) {
                anotherUserMotherLanguage = updatedLanguage
            }

        }
    }

    fun updateLanguageSwitchForUserId(userId: String, isSwitchOn: Boolean) {

        CoroutineScope(Dispatchers.IO).launch {

            val updatedChatThreads = mutableListOf<ChatThread>()

            chatThreadCache.getAllChatThreadsSync().forEach { chatThread ->
                chatThread.members.forEach {
                    if (it.userId == userId) {
                        var members = chatThread.members

                        members.forEach {
                            if (it.userId == userId) {
                                it.motherSwitch = isSwitchOn
                            }
                        }
                        chatThread.members = members
                        updatedChatThreads.add(chatThread)
                    }
                }
            }
            if (updatedChatThreads.isNotEmpty()) {
                chatThreadCache.insert(updatedChatThreads)
            }

            if (anotherUserId == userId) {
                anotherUserMotherSwitch = isSwitchOn
            }
        }
    }


    fun readMessage(messageId: String) {
        val messageBundle = JSONObject().also {
            it.put("userId", preferenceService.getString(R.string.pkey_user_Id) ?: "")
            it.put("messageId", messageId)
            it.put("type", MessageType.READ.value)
        }
        mSocket?.emit(ACKNOWLEDGE_MESSAGE, messageBundle, object : Ack {
            override fun call(vararg args: Any?) {
                CoroutineScope(Dispatchers.Main).launch {
                    smoothScrolling.value = true
                    val data: JSONObject = args[0] as JSONObject
                    try {
                        Log.e("data are", "" + data)
                    } catch (e: JSONException) {
                        Log.e("error", e.message.toString())
                    }
                }
            }
        })
    }

    fun checkLanguageCode(): String {

        val language = preferenceService.getString(R.string.pkey_motherLanguage, "English")
        var code = preferenceService.getString(R.string.pkey_each_userLanguage_Code, "en-US")
        /* val listOfObject = localLanguage?.find { it.name.equals(language, ignoreCase = true) }
         listOfObject?.let {
             code = it.languageCode
         }*/
        return code!!
    }


    fun checkLanguageCodeForSpeech(): String {
        // var code = "en-US"
        val language = preferenceService.getString(R.string.pkey_motherLanguage, "English")

        var code = preferenceService.getString(R.string.pkey_each_userLanguage_Code, "en-US")


        Log.d(
            "checkLanguageCodeForSpeech: ",
            "" + language + "code-" + code
        )
        return code!!
    }

    fun getUserId() = preferenceService.getString(R.string.pkey_user_Id) ?: ""


    fun uploadFileOnBackground(
        imagePath: String,
        message: String,
        type: String,
        fileFormat: String,
        orignalLocalPath: String = ""
    ) {
        Log.d( "image-path: ","$imagePath  $message  $type  $fileFormat   $orignalLocalPath")


        if (anotherUserMotherSwitch && isLanguageSwitch.value!!) {

                    val lang =
                        preferenceService.getString(R.string.pkey_motherLanguage, "English")!!

                            uploadInBackground(
                                imagePath, message, type, fileFormat,
                                orignalLocalPath, "", ""
                            )


        } else if (anotherUserMotherSwitch) {

                               uploadInBackground(
                        imagePath, message, type, fileFormat,
                        orignalLocalPath, message, ""
                    )

        } else if (isLanguageSwitch.value!!) {

            val lang = preferenceService.getString(R.string.pkey_motherLanguage, "English")!!


                    uploadInBackground(
                        imagePath, message, type, fileFormat,
                        orignalLocalPath, "", message
                    )

        } else {
            Log.i("meg_sender4", " " + message + "send" + message + "rec" + message)
            //  messageBundle(text, text, text)
            uploadInBackground(
                imagePath, message, type, fileFormat,
                orignalLocalPath, message, message
            )
        }

        /*  val data = workDataOf(
                 // UploadFileWorker.SOCKET_MESSAGE_SENDER to senderId,
                 // UploadFileWorker.SOCKET_MESSAGE_RECEIVER to receiverId,
                  UploadFileWorker.File to imagePath,
                  UploadFileWorker.TYPE to type,
                  UploadFileWorker.MESSAGE to message,
                  UploadFileWorker.ANOTHER_ID to anotherUserId,
                  UploadFileWorker.FILE_FORMAT to fileFormat,
                  UploadFileWorker.ROOM_ID to roomId.value,
                  UploadFileWorker.ORIGNAL_PATH to orignalLocalPath,
                  UploadFileWorker.LANGUAGE_CODE to checkLanguageCode()
          )

          val save = OneTimeWorkRequestBuilder<


          UploadFileWorker>()
                  .setConstraints(constraints)
                  .setInputData(data)
                  .addTag("OUTPUT")
                  .build()
          workerManager.enqueue(save)
      */
    }

    fun uploadInBackground(
        imagePath: String,
        message: String,
        type: String,
        fileFormat: String,
        orignalLocalPath: String = "",
        senderId: String,
        receiverId: String
    ) {
        UploadFileWorker.setSocket(mSocket!!)
        val data = workDataOf(
            UploadFileWorker.MESSAGE_SENDER to senderId,
            UploadFileWorker.MESSAGE_RECEIVER to receiverId,
            UploadFileWorker.File to imagePath,
            UploadFileWorker.TYPE to type,
            UploadFileWorker.MESSAGE to message,
            UploadFileWorker.ANOTHER_ID to anotherUserId,
            UploadFileWorker.FILE_FORMAT to fileFormat,
            UploadFileWorker.ROOM_ID to roomId.value,
            UploadFileWorker.ORIGNAL_PATH to orignalLocalPath,
            UploadFileWorker.LANGUAGE_CODE to checkLanguageCode(),
            UploadFileWorker.SOCKET_MESSAGE_REPLY_TO to replyMessage.value?.id,
            UploadFileWorker.IS_GROUP to isGroup.value

        )

        val save = OneTimeWorkRequestBuilder<UploadFileWorker>()
            .setConstraints(constraints)
            .setInputData(data)
            .addTag("OUTPUT")
            .build()
        workerManager.enqueue(save)


     /*   workerManager.getWorkInfoByIdLiveData(save.id).observe(this, androidx.lifecycle.Observer {
            if (it!=null)
            {
                val progress = it.progress
                val value = progress.getInt(Progress, 0)
            }
        })*/


            // requestId is the WorkRequest id
     /*   workerManager.getWorkInfoByIdLiveData(save.id)
            .observe(observer, Observer { workInfo: WorkInfo? ->
                if (workInfo != null) {
                    val progress = workInfo.progress
                    val value = progress.getInt(Progress, 0)
                    // Do something with progress information
                }
            })*/



        CoroutineScope(Dispatchers.Main).launch {
            if (replyMessage.value != null) {
                replyMessage.value == null
            }
        }
    }

    fun downloadFileOnBackground(url: String, type: String) {
        val data = workDataOf(DownloadFileWorker.URL to url, DownloadFileWorker.TYPE to type)
        val save = OneTimeWorkRequestBuilder<DownloadFileWorker>()
            .setConstraints(constraints)
            .setInputData(data)
            .addTag("Download")
            .build()
        workerManager.enqueue(save)
    }


    fun checkLocalPathExist(filePath: String, chatData: Chat): String {
        val path = filePath + "/" + File(chatData.url).name
        return when {
            chatData.localUrl.isNotEmpty() && File(chatData.localUrl).exists() -> chatData.localUrl
            File(path).exists() -> path
            else -> ""
        }
    }


    @RequiresApi(Build.VERSION_CODES.N)
    fun sendCommand(result: String, language: String?): Boolean {

        val dataa = defaultKeywords.value?.filter {
            //Log.d("default-keyword",Gson().toJson(it.defaultKeywords))
            it.mayaCommand.equals(language) && it.defaultKeywords.contains(result)

        }
        return !dataa.isNullOrEmpty() && isSpeechLanguageEnableOrDisable() == ChatType.SPEECH_TO_TEXT

        /*var found: Boolean = false

        if (language.equals("English")) {


            val strings = arrayOf(
                "maya send",
                "maya send message",
                "my send message",
                "my send",
                "mia send",
                "my ascend",
                "mayasand",
                "mayas and",
                "maia send",
                "maia send message",
                "maia ascend",
                "mia sand",
                "my ascent",
                "my accent",
                "Maja send",
                "maya friend",
                "maya sent"
            )
            found = Arrays.stream(strings).anyMatch { t -> t == result.toLowerCase() }

        }
        if (language.equals("Hindi")) {
            val strings = arrayOf(
                "मायर्स एंड",
                "माया सेंड",
                "माया सेंड मेसेज",
                "माया सेंड",
                "सेंड मेसेज",
                "सेंड",
                "सैंड",
                "माया सैंड",
                "सैंड मेसेज",
                "साइड",
                "सैड"
            )
            found = Arrays.stream(strings).anyMatch { t -> t == result }
        }

        if (language.equals("Bangala")) {
            val strings = arrayOf(
                "মায়া সেন", "মাইয়া ফ্রেন্ড", "মায়া সেন্ড", "মায়া সান", "পারসেন্ট",
                "মায়ার স্ট্যান্ড", "মাই ফ্রেন্ড", "মাই ফ্রেন্ড", "আমার আরোহী",
                "মায়া পাঠায়", "মায়া পাঠায়", "আমার আরোহী", "আমার প্রেরণ"
            )
            found = Arrays.stream(strings).anyMatch { t -> t == result }
        }

        if (language.equals("Gujarati")) {
            val strings =
                arrayOf(
                    "માયા સેન્ડ", "માયા સેન્ટ", "માય ફ્રેન્ડ", "માયાભાઈ", "મહેસાણા",
                    "માય સન", "મારું ચડવું", "માજા મોકલો", "મારું ચડવું", "માયાસિંહ", "મારું ચડવું"
                )
            found = Arrays.stream(strings).anyMatch { t -> t == result }
        }

        if (language.equals("Kannada")) {
            val strings =
                arrayOf(
                    "ಮಾಯ ಸೆಂಡ್",
                    "ಭಯ ಸೆಂಡ್",
                    "ಮೈ ಫ್ರೆಂಡ್",
                    "ಮಾಯಾ ಸೆಂಡ್",
                    "ಮಹೇಶ್ ಅಂಡ್",
                    "ಮಯಾಸ್ ಅಂಡ್",
                    "ಮಾಯಾ ಕಳುಹಿಸಿ",
                    "ನನ್ನ ಆರೋಹಣ",
                    "ಮಾಯಾ ಕಳುಹಿಸಿ",
                    "ನನ್ನ ಆರೋಹಣ",
                    "ಮಜಾ ಕಳುಹಿಸಿ"

                )
            found = Arrays.stream(strings).anyMatch { t -> t == result }
        }

        if (language.equals("Malayalam")) {
            val strings =
                arrayOf(
                    "മായാ centre",
                    "മായാ സെൻറ്",
                    "മഴ സെൻറ്",
                    "മായ cent",
                    "മായ അയയ്ക്കുക",
                    "എന്റെ കയറ്റം",
                    "മായ അയയ്ക്കുക",
                    "എന്റെ കയറ്റം",
                    "മായ അയയ്ക്കുക",
                    "മായ അയയ്ക്കുക"
                )
            found = Arrays.stream(strings).anyMatch { t -> t == result }
        }
        if (language.equals("Marathi")) {
            val strings =
                arrayOf(
                    " माया सेंट",
                    "माय फ्रेंड",
                    "माया सेंड",
                    "माझे चढणे",
                    "माझा मित्र",
                    "माया पाठवते",
                    "माझे चढणे",
                    "माझे चढणे"
                )
            found = Arrays.stream(strings).anyMatch { t -> t == result }
        }
        if (language.equals("Tamil")) {
            val strings =
                arrayOf(
                    "மாயா சன்",
                    "மை சன்",
                    "மாயா வேண்டும்",
                    "மாயா சென்ட்",
                    "மாயா அனுப்பு",
                    "மயாசுரன்",
                    "என் நண்பர்",
                    "என் ஏற்றம்",
                    "மாயா அனுப்பு",
                    "என் ஏற்றம்",
                    "என் ஏற்றம்"
                )
            found = Arrays.stream(strings).anyMatch { t -> t == result }


        }
        if (language.equals("Telgu")) {
            val strings = arrayOf(
                "మాయా సెండ్",
                "మాయా స్టాండ్",
                "మాయా ఫ్రెండ్",
                "మాయా సెండ్",
                "నా ఆరోహణ",
                "మాయ స్టాండ్",
                "మాయ పంపండి",
                "నా ఆరోహణ",
                "మాయ పంపండి"
            )
            found = Arrays.stream(strings).anyMatch { t -> t == result }


        }
        if (language.equals("Urdu")) {
            val strings = arrayOf(
                " مای سینڈ", "میاں سنگھ", "میا دوست"
                , "میرا چڑھ جانا"
                , "مایا بھیجیں"
                , "میرا چڑھ جانا"
                , "میرا چڑھ جانا"
                , "مایا بھیجیں"


            )
            found = Arrays.stream(strings).anyMatch { t -> t == result }


        }

        return found && isSpeechLanguageEnableOrDisable() == ChatType.SPEECH_TO_TEXT
    */
    }


    fun mayaSendEnglish(result: String): Boolean {
        var found: Boolean = false
        val strings = arrayOf(
            "maya send",
            "maya send message",
            "my send message",
            "my send",
            "mia send",
            "my ascend",
            "mayasand",
            "mayas and",
            "maia send",
            "maia send message",
            "maia ascend",
            "mia sand",
            "my ascent",
            "my accent",
            "Maja send",
            "maya friend",
            "maya sent"
        )
        found = Arrays.stream(strings).anyMatch { t -> t == result.toLowerCase() }

        return found && isSpeechLanguageEnableOrDisable() == ChatType.SPEECH_TO_TEXT
    }

    fun mayaOffEnglish(result: String): Boolean {
        var found: Boolean = false
        val strings = arrayOf(
            "maya off", "my off", "mia off", "my off", "maia off",
            "Maya off", "My off", "Mia off", "My off", "Maia off"
        )
        found = Arrays.stream(strings).anyMatch { t -> t == result.toLowerCase() }

        return found && isSpeechLanguageEnableOrDisable() == ChatType.SPEECH_TO_TEXT
    }

    fun mayaCloseEnglish(result: String): Boolean {
        var found: Boolean = false
        val strings = arrayOf(
            "maya close",
            "mia close",
            "my close",
            "mya close",
            "maya clothes",
            "maia close",
            "maia clothes",
            "Maya close",
            "Mia close",
            "My close",
            "Mya close",
            "Maya clothes",
            "Maia close",
            "Maia clothes"
        )
        found = Arrays.stream(strings).anyMatch { t -> t == result.toLowerCase() }

        return found && isSpeechLanguageEnableOrDisable() == ChatType.SPEECH_TO_TEXT
    }

    fun mayaByeEnglish(result: String): Boolean {
        var found: Boolean = false
        val strings = arrayOf(
            "maya bye",
            "mia bye",
            "my bye",
            "mya by",
            "maya by",
            "maia bye",
            "maia by",
            "Maya bye",
            "Mia bye",
            "My bye",
            "Mya by",
            "Maya by",
            "Maia bye",
            "Maia by",
            "maya bhai"
        )
        found = Arrays.stream(strings).anyMatch { t -> t == result.toLowerCase() }

        return found
    }


    fun mayaListenEnglish(result: String): Boolean {
        var found: Boolean = false
        val strings = arrayOf(
            "maya listen",
            "mia listen",
            "my allison",
            "my listen",
            "my lesson",
            "maya lesson",
            "maia listen",
            "maia allison",
            "Maya listen",
            "Mia listen",
            "My allison",
            "My listen",
            "My lesson",
            "Maya lesson",
            "Maia listen",
            "Maia allison"
        )
        found = Arrays.stream(strings).anyMatch { t -> t == result.toLowerCase() }

        return found && isSpeechLanguageEnableOrDisable() == ChatType.SPEECH_TO_TEXT
    }


    fun stopTypeCommand(result: String, language: String?): Boolean {
        val dataa = defaultKeywords.value?.filter {
            it.mayaCommand.equals(language) && it.defaultKeywords.contains(result)

        }
        return !dataa.isNullOrEmpty() && isSpeechLanguageEnableOrDisable() == ChatType.SPEECH_TO_TEXT

        /* var found: Boolean = false

         if (language.equals("English")) {
             val strings = arrayOf(
                 "maya off", "my off", "mia off", "my off", "maia off",
                 "Maya off", "My off", "Mia off", "My off", "Maia off"
             )
             found = Arrays.stream(strings).anyMatch { t -> t == result.toLowerCase() }

         }
         if (language.equals("Hindi")) {
             val strings =
                 arrayOf("माया स्टॉप", "माया स्टॉक", "माया स्टॉक टाइपिंग", "माया टॉक", "माया और")
             found = Arrays.stream(strings).anyMatch { t -> t == result.toLowerCase() }
         }

         if (language.equals("Bangala")) {
             val strings = arrayOf(
                 "মায়া অফ"
                 , "মায়া বন্ধ"
                 , "মেয়ারহফ"
                 , "আমার বন্ধ"
                 , "আমার বন্ধ"
                 , "মায়া বন্ধ"
             )
             found = Arrays.stream(strings).anyMatch { t -> t == result.toLowerCase() }
         }

         if (language.equals("Gujarati")) {
             val strings = arrayOf(
                 "માયા of", "માયા આપો", "માયા આવ", "માયા ઓ"
                 , "મારું ઘર", "મારો બંધ", "માયા બંધ", "મારો બંધ", "મારું"

             )
             found = Arrays.stream(strings).anyMatch { t -> t == result.toLowerCase() }
         }

         if (language.equals("Kannada")) {
             val strings = arrayOf(
                 "ಮಯಾಸ್", "ಮಯ್ಎಎಫ್", "ಮಾಯಾ"
                 , "ನನ್ನ ಆಫ್"
                 , "ನ ಮಾಯಾ"
                 , "ನ ನಕ್ಷೆ"
                 , "ಮಾಯಾ ಆಫ್"
                 , "ಮಾಯಾ ಹೋ"
             )
             found = Arrays.stream(strings).anyMatch { t -> t == result.toLowerCase() }
         }

         if (language.equals("Malayalam")) {
             val strings = arrayOf(
                 "മഴ ഓഫ്", "മനാഫ്,മനോജ്", "ബായ് ഓഫ്"
                 , "മായാപൂർ"
                 , "മായ ഓഫ്"
                 , "എന്റെ ഓഫാണ്"
                 , "മയോ"
                 , "മായ ഓഫ്"
             )
             found = Arrays.stream(strings).anyMatch { t -> t == result.toLowerCase() }
         }
         if (language.equals("Marathi")) {
             val strings = arrayOf(
                 "माया ऑफ"
                 , "माझे बंद"
                 , "माझे घर"
                 , "वायर बंद"
                 , "माया बंद"
                 , "माया बंद"
             )
             found = Arrays.stream(strings).anyMatch { t -> t == result.toLowerCase() }
         }
         if (language.equals("Tamil")) {
             val strings = arrayOf(
                 "மை வைஃப்", "மாயா", "மை ஆப்"
                 , "மாயா ஆஃப்"
                 , "ஏன் ஆஃப்"
                 , "மாயா ஆஃப்"
                 , "மேயர்ஹோஃப்"
                 , "மாயா ஆஃப்"
             )
             found = Arrays.stream(strings).anyMatch { t -> t == result.toLowerCase() }

         }
         if (language.equals("Telgu")) {
             val strings = arrayOf(
                 "మాయా ఆప్స్", "నా ఆఫ్"
                 , "మేయర్హోఫ్"
                 , "మాయ ఆఫ్"
                 , "మాయ ఆఫ్"
             )
             found = Arrays.stream(strings).anyMatch { t -> t == result.toLowerCase() }


         }
         if (language.equals("Urdu")) {
             val strings = arrayOf(
                 " مایا اوف", "مایا اوف"
                 , "میری آف"
                 , "مایا آف"
                 , "مایا آف"
                 , "مایا آف"
                 , "مایا آف"

             )
             found = Arrays.stream(strings).anyMatch { t -> t == result.toLowerCase() }


         }

         return found && isSpeechLanguageEnableOrDisable() == ChatType.SPEECH_TO_TEXT
    */
    }


    fun closeChatCommand(result: String, language: String?): Boolean {
        val dataa = defaultKeywords.value?.filter {

            it.mayaCommand.equals(language) && it.defaultKeywords.contains(result)

        }
        return !dataa.isNullOrEmpty() && isSpeechLanguageEnableOrDisable() == ChatType.SPEECH_TO_TEXT


        /*var found: Boolean = false

        if (language.equals("English")) {
            val strings = arrayOf(
                "maya close",
                "mia close",
                "my close",
                "mya close",
                "maya clothes",
                "maia close",
                "maia clothes",
                "Maya close",
                "Mia close",
                "My close",
                "Mya close",
                "Maya clothes",
                "Maia close",
                "Maia clothes"
            )
            found = Arrays.stream(strings).anyMatch { t -> t == result.toLowerCase() }

        }
        if (language.equals("Hindi")) {
            val strings = arrayOf("माया क्लोज", "माया क्लोज़")
            found = Arrays.stream(strings).anyMatch { t -> t == result.toLowerCase() }
        }

        if (language.equals("Bangala")) {
            val strings =
                arrayOf(
                    "মায়া ক্লোজ"
                    , "মায়ার কাছে"
                    , "মায়োক্লোনাস"
                    , "মায়ার কাছে"
                    , "মায়ার পোশাক"
                    , "বন্ধ"
                )
            found = Arrays.stream(strings).anyMatch { t -> t == result.toLowerCase() }
        }

        if (language.equals("Gujarati")) {
            val strings = arrayOf(
                "માયા ક્લોઝ", "મેં ક્લોઝ"
                , "માયા નજીક"
                , "માયા નજીક"
                , "માયા નજીક"
                , "માયા નજીક"
            )
            found = Arrays.stream(strings).anyMatch { t -> t == result.toLowerCase() }
        }

        if (language.equals("Kannada")) {
            val strings = arrayOf(
                "ಮಾಯ ಕ್ಲೋಸ್", "ಮಯೋಕ್ಲೋನಸ್", "ಮಾಯಾ ಕ್ಲೋಸ್", "ಮಾಯಾ ಷರತ್ತು"
                , "ಮಾಯಾ ಮುಚ್ಚಿ"
            )
            found = Arrays.stream(strings).anyMatch { t -> t == result.toLowerCase() }
        }

        if (language.equals("Malayalam")) {
            val strings =
                arrayOf(
                    "മായാ ക്ലോസ്", "മായ ക്ലോസ്"
                    , "മായ വസ്ത്രങ്ങൾ"
                    , "മായ അടച്ചു"
                    , "മായ ജോസ്"
                    , "മായ അടച്ചു"
                    , "മൈക്രോസ്"
                )
            found = Arrays.stream(strings).anyMatch { t -> t == result.toLowerCase() }
        }
        if (language.equals("Marathi")) {
            val strings = arrayOf("माया क्लोज", "मायक्रो", "माया जवळ")
            found = Arrays.stream(strings).anyMatch { t -> t == result.toLowerCase() }
        }
        if (language.equals("Tamil")) {
            val strings = arrayOf("மாயா கிளோஸ்", "மே கிளோஸ்", "மாயா மூடு", "என் மூடு", "மாயா மூடு")
            found = Arrays.stream(strings).anyMatch { t -> t == result.toLowerCase() }


        }
        if (language.equals("Telgu")) {
            val strings = arrayOf("మాయ  క్లోజ్", "మాయా crores", "మాయ దగ్గరగా", "మాయ కోట్స్")
            found = Arrays.stream(strings).anyMatch { t -> t == result.toLowerCase() }


        }
        if (language.equals("Urdu")) {
            val strings = arrayOf(
                "میاں کلوز", "مایا بلوچ",
                "مایا لوس"
                , "مایا قریب ہے"
                , "مایا قریب ہے"
                , ""
            )
            found = Arrays.stream(strings).anyMatch { t -> t == result.toLowerCase() }


        }

        return found && isSpeechLanguageEnableOrDisable() == ChatType.SPEECH_TO_TEXT
    */
    }


    fun closeApplicationCommand(result: String, language: String?): Boolean {
        val dataa = defaultKeywords.value?.filter {
            it.mayaCommand.equals(language) && it.defaultKeywords.contains(result)

        }
        return !dataa.isNullOrEmpty()


        /*var found: Boolean = false

        if (language.equals("English")) {
            val strings = arrayOf(
                "maya bye",
                "mia bye",
                "my bye",
                "mya by",
                "maya by",
                "maia bye",
                "maia by",
                "Maya bye",
                "Mia bye",
                "My bye",
                "Mya by",
                "Maya by",
                "Maia bye",
                "Maia by",
                "maya bhai"
            )
            found = Arrays.stream(strings).anyMatch { t -> t == result.toLowerCase() }

        }
        if (language.equals("Hindi")) {
            val strings = arrayOf("माया बाई")
            found = Arrays.stream(strings).anyMatch { t -> t == result.toLowerCase() }
        }

        if (language.equals("Bangala")) {
            val strings =
                arrayOf(
                    "মায়া বাই",
                    "মায়া ভাই",
                    "মায়া বাড়ে",
                    "মায়া পায়",
                    "মায়া বায়",
                    "মায়াভাই",
                    "মায়া বাই",
                    "মায়া ভাই"
                )
            found = Arrays.stream(strings).anyMatch { t -> t == result.toLowerCase() }
        }

        if (language.equals("Gujarati")) {
            val strings = arrayOf(
                "મમાયા ભાઈ", "માયા બાય"
                , "માયા ભાઈ"
                , "માયા ભાઈ"
                , "માયા બાય"
                , "માયા ભાઈ"
                , "માયા ભાઈ"
            )
            found = Arrays.stream(strings).anyMatch { t -> t == result.toLowerCase() }
        }

        if (language.equals("Kannada")) {
            val strings = arrayOf("ಮಾಯಾ ಬಾಯ್", "ಮಾಯಾ ಬೈ", "ಮಯಭೈ", "ಮಾಯಾಬೆನ್", "ಮಾಯಾ ಭಾಯ್")
            found = Arrays.stream(strings).anyMatch { t -> t == result.toLowerCase() }
        }

        if (language.equals("Malayalam")) {
            val strings = arrayOf("മായാ ബായ്", "മായ ബായ്", "മായ ഭായ്", "മായ ബൈ", "മായ ഭായ്")
            found = Arrays.stream(strings).anyMatch { t -> t == result.toLowerCase() }
        }
        if (language.equals("Marathi")) {
            val strings =
                arrayOf("माया बाय", "माया बाई", "माया भाई", "माया भाई")
            found = Arrays.stream(strings).anyMatch { t -> t == result.toLowerCase() }
        }
        if (language.equals("Tamil")) {
            val strings = arrayOf("மாயா ஸ்டோர்", "மாயா பை", "மாயா பாய்", "மாயா பாய்")
            found = Arrays.stream(strings).anyMatch { t -> t == result.toLowerCase() }
        }
        if (language.equals("Telgu")) {
            val strings = arrayOf("మాయా బాయ్", "మాయ అబ్బాయి", "మాయ భాయ్")
            found = Arrays.stream(strings).anyMatch { t -> t == result.toLowerCase() }


        }
        if (language.equals("Urdu")) {
            val strings = arrayOf("مایا بائے", "مایا بائی", "مایا بائے", "مایا بائی", "مایا بھائی")
            found = Arrays.stream(strings).anyMatch { t -> t == result.toLowerCase() }


        }
        return found*/
    }


    fun readLastMessageCommand(result: String, language: String?): Boolean {

        val dataa = defaultKeywords.value?.filter {
            it.mayaCommand.equals(language) && it.defaultKeywords.contains(result)

        }
        return !dataa.isNullOrEmpty()
        /*var found: Boolean = false

        if (language.equals("English")) {
            val strings = arrayOf(
                "maya listen",
                "mia listen",
                "my allison",
                "my listen",
                "my lesson",
                "maya lesson",
                "maia listen",
                "maia allison",
                "Maya listen",
                "Mia listen",
                "My allison",
                "My listen",
                "My lesson",
                "Maya lesson",
                "Maia listen",
                "Maia allison"
            )
            found = Arrays.stream(strings).anyMatch { t -> t == result.toLowerCase() }

        }
        if (language.equals("Hindi")) {
            val strings = arrayOf("माया", "मिया एलिसन", "माया एलिसन", "लिसन", "माय लिसन")
            found = Arrays.stream(strings).anyMatch { t -> t == result.toLowerCase() }
        }

        if (language.equals("Bangla")) {
            val strings = arrayOf(
                "মায়া লিসেন",
                "মায়া লেশন",
                "মায়া লেসন",
                "মায়া রেশন",
                " ভায়োলেশন",
                "মায়ার লিসেন",
                "মাই লিসেন",
                "মায়ান শন",
                "মাই লিসেন",
                "মাইয়া কিসন"
                , "মাইলেসন"
                , "মায়া শুনি"
                , "মায়া শুনি"
                , "আমার এলিসন"
                , "আমার শুনুন"
            )
            found = Arrays.stream(strings).anyMatch { t -> t == result.toLowerCase() }
        }

        if (language.equals("Gujarati")) {
            val strings =
                arrayOf(
                    "માયા લીસ્ટન", "lપહેલી", "માયાબેન", "માલી સિંગ", "માયા"
                    , "માયા સાંભળો"
                    , "ઉલ્લંઘન"
                )
            found = Arrays.stream(strings).anyMatch { t -> t == result.toLowerCase() }
        }

        if (language.equals("Kannada")) {
            val strings = arrayOf(
                "ಮಾಯ ಲೆಸನ್",
                "ಮಯ ಲೆಸೆನ್",
                "ಮಾಯಾ ಆಲಿಸಿ",
                "ಮಾಯಾ ಪಾಠ",
                "ಮನೇಲಿ ಸಾಂಗ್",
                "ಮೈಲಿ ಸನ್",
                "ಡಯಾಲಿಸಿಸ್",
                "ಮಾಯಾ ಎಲಿಸನ್"
                , "ಮಾಯಾ ಕೇಳು"
                , "ಔಷಧಿ"
                , "ವೈರ್ಲೆಸ್"
                , "ಮಾಯಾ ಕೇಳು"
                , "ಉಲ್ಲಂಘನೆ"
            )
            found = Arrays.stream(strings).anyMatch { t -> t == result.toLowerCase() }
        }

        if (language.equals("Malayalam")) {
            val strings = arrayOf(
                "മായാ ലെസ്സൺ",
                "മായാ ലിസൺ",
                "മായ ലിസൺ,മൈലാഞ്ചി ഡിസൈൻ",
                "മഴ ഡിസൈൻ",
                "മഴലിസൺ",
                "മായ ആലിസൺ"
                , "മായ ശ്രദ്ധിക്കൂ"
                , "മൈലെസൺ"
                , "മായ ശ്രദ്ധിക്കൂ"
                , "മയോസിൻ"
                , "എന്റെ ശ്രദ്ധിക്കൂ"
            )
            found = Arrays.stream(strings).anyMatch { t -> t == result.toLowerCase() }
        }
        if (language.equals("Marathi")) {
            val strings =
                arrayOf(
                    "माया लिसन", "मायाने सीन", "माया लीसेन", "माया फिल्म", "माऊली सॉंग"
                    , "माया ऐका"
                    , "मेयलेसन"
                    , "औषध"
                    , "माया ऐका"
                    , "माया ऐका"
                )
            found = Arrays.stream(strings).anyMatch { t -> t == result.toLowerCase() }
        }
        if (language.equals("Tamil")) {
            val strings = arrayOf(
                "மாயா லிசன்",
                "மயில் டிசைன்",
                "மலேசியன்",
                "மாயா கிலிசி",
                "மாயா மிஷின்",
                "மாயா லெசன்"
                , "மைலெசன்"
                , "மாயா கேளுங்கள்"
                , "listen     என் கேளுங்கள்"
            )
            found = Arrays.stream(strings).anyMatch { t -> t == result.toLowerCase() }


        }
        if (language.equals("Telgu")) {
            val strings = arrayOf(
                " మాయ listen",
                "mayali సీన్",
                "మాయా మెడిసిన్",
                "మాయావిశ్వం",
                "మై లవ్ సీన్",
                "మెడిసిన్",
                "మాయా లిస్ట్"
                , "ఔషధం"
                , "మైలేసన్"
                , "మైలేసన్"
                , "మాయ వినండి"
                , "మైలేసన్"
            )
            found = Arrays.stream(strings).anyMatch { t -> t == result.toLowerCase() }


        }
        if (language.equals("Urdu")) {
            val strings = arrayOf(
                "مایا لیسن", "مایا علی حسن", "مایا لیسن"
                , "میلیسا"
                , "میئلیسن"
                , "مایا سنو"
                , "میری سن"
                , "مایا سنو"
                , ""
            )
            found = Arrays.stream(strings).anyMatch { t -> t == result.toLowerCase() }


        }
        return found*/
    }

    fun updateChatActivity(number: String) {
        Log.d( "number---: ",""+number)
        preferenceService.putPhoneNumberString(R.string.pkey_phone_number, number)
    }

    fun stopTyping(result: String) {
        when {
            isSilentMode > ChatActivity.SILENT_MODE -> {
                isSilentMode = 0
                isSpeechLanguageEnable.value = ChatType.NORMAL
            }
            result.isNotBlank() -> {

                isSilentMode = 0
                speechLanguageText.value += " $result"
//                listening_input.setText(viewModel.speechLanguageText.value)
//                speechLanguageText.value?.length?.let {
//                }

            }
            else -> isSilentMode++
        }
    }

    fun sendLocationToUser() {
        locationRepository.getUserCurrentLocation {
            it?.let {
                sendMessage(locationReqeust(it.latitude, it.longitude))
            }
        }
    }

    fun sendContactToUser(name: String, number: String) {
        sendMessage(contactRequest(number, name))
    }

    private fun encodeString(encoded: ByteArray): ByteArray? {
        val dataDec: ByteArray = Base64.encode(encoded, Base64.DEFAULT)
        return dataDec
    }


    private fun createJson(
        type: String,
        message: String,
        messageSender: String,
        messageReceiver: String,
        url: String,
        userId: String,
        isForwarded: Boolean = false
    ): JSONObject {
        var encryptUrl = ""
        if (!url.isNullOrBlank()) {
            encryptUrl = EncryptionViewModel.encryptString(url)
        }
        val encryptedMessage = EncryptionViewModel.encryptString(message.trim().replace("\n ", ""))
        val json = JSONObject().also {
            it.put(
                ChatActivity.SOCKET_FROM,
                preferenceService.getString(R.string.pkey_user_Id) ?: ""
            )
            it.put(ChatActivity.SOCKET_TO, userId)
            it.put(ChatActivity.SOCKET_TYPE, type)
            it.put(
                ChatActivity.SOCKET_MESSAGE,
                encryptedMessage.replace("\n", "").replace("\r", "")
            )
            it.put(ChatActivity.INPUT_LANGUAGE,Locale.getDefault().getLanguage())
            it.put(ChatActivity.SOCKET_URL, encryptUrl.replace("\n", "").replace("\r", ""))
            replyMessage.value?.let { replyMessage ->
                it.put(ChatActivity.SOCKET_MESSAGE_REPLY_TO, replyMessage.id)
            }
            it.put(ChatActivity.IS_FORWARD, isForwarded)
            if (isGroup.value!!){
                it.put(ChatActivity.ROOM_ID,roomId.value)
            }
        }

        return json
    }

    private fun contactRequest(number: String, name: String, isForward: Boolean = false): JSONObject {
        val jsonObject = JSONObject().also {
            it.put("number", number)
            it.put("name", name)
        }
        return JSONObject().also {
            it.put(
                ChatActivity.SOCKET_FROM,
                preferenceService.getString(R.string.pkey_user_Id) ?: ""
            )
            it.put(ChatActivity.SOCKET_TO, anotherUserId)
            it.put(ChatActivity.SOCKET_TYPE, MessageType.CONTACT.value)

            it.put("contact", jsonObject)
            if (isGroup.value!!){
                it.put(ChatActivity.ROOM_ID,roomId.value)
            }
            replyMessage.value?.let { replyMessage ->
                it.put(ChatActivity.SOCKET_MESSAGE_REPLY_TO, replyMessage.id)
            }
            // it.put(ChatActivity.IS_FORWARD, isForward)

        }
    }

    private fun locationReqeust(
        lat: Double, lng: Double,
        userId: String = anotherUserId,
        isForwarded: Boolean = false
    ): JSONObject {
        return JSONObject().also {
            it.put(ChatActivity.SOCKET_TYPE, MessageType.LOCATION.value)
            it.put(
                ChatActivity.SOCKET_FROM,
                preferenceService.getString(R.string.pkey_user_Id) ?: ""
            )
            it.put(ChatActivity.SOCKET_TO, userId)
            it.put("lat", lat)
            it.put("lng", lng)
            it.put(ChatActivity.IS_FORWARD, isForwarded)
            if (isGroup.value!!) {
                it.put(ChatActivity.ROOM_ID, roomId.value)
            }
            replyMessage.value?.let { replyMessage ->
                it.put(ChatActivity.SOCKET_MESSAGE_REPLY_TO, replyMessage.id)
            }
        }
    }

    private fun statusJson(): JSONObject {
        return JSONObject().also { it.put("userId", anotherUserId) }
    }


    private fun userTypingJson(): JSONObject {
        return JSONObject().also {
            it.put(ChatActivity.SOCKET_TYPE, "Single")
            it.put(
                ChatActivity.SOCKET_FROM,
                preferenceService.getString(R.string.pkey_user_Id) ?: ""
            )
            it.put(ChatActivity.SOCKET_TO, anotherUserId)
        }
    }


    fun isBlockedUserOrNot(mContext: Context): MutableLiveData<Boolean?> {
        /* var countryCode: Int = preferenceService.getString(R.string.pkey_countryCode)!!.length
         var isBlock :MutableLiveData<Boolean?> = MutableLiveData()
         val result = phoneNumber.drop(countryCode)*/
        var isBlock: MutableLiveData<Boolean?> = MutableLiveData()
        var result =""

        if (isGroup.value!!) {
            result=""
        }
        else{
            result = CountryCodeSelector(mContext).removeCountryCode(phoneNumber)

        }

        preferenceService.getString(R.string.pkey_blocked_user)?.run {
            Log.d("block_number2", "bolcked " +"--"+ result +preferenceService.getString(R.string.pkey_blocked_user))
            val condition = result.isNotBlank() && this.contains(result)
            isBlock.value = condition

        }
        return isBlock
    }


    private fun createJSONForForward(
        chat: Chat,
        messageType: String,
        userId: String = anotherUserId
    ): JSONObject {
        //Log.d( "forwardMessage: ",Gson().toJson(chat))
        if (chat.type.equals("contact"))
        {
            val jsonObject = JSONObject().also {
                it.put("number", chat.contact_number)
                it.put("name",  chat.contact_name)
            }
            return JSONObject().also {
                it.put(
                    ChatActivity.SOCKET_FROM,
                    preferenceService.getString(R.string.pkey_user_Id) ?: ""
                )
                it.put(ChatActivity.SOCKET_TO, userId)
                it.put(ChatActivity.SOCKET_TYPE, messageType)
                it.put(ChatActivity.IS_FORWARD, chat.forwarded)
                it.put("contact", jsonObject)

                if (isGroup.value!!) {
                    it.put(ChatActivity.ROOM_ID, roomId.value)
                }

                replyMessage.value?.let { replyMessage ->
                    it.put(ChatActivity.SOCKET_MESSAGE_REPLY_TO, replyMessage.id)
                }
                // it.put(ChatActivity.IS_FORWARD, isForward)

            }
        }
        else{

            return JSONObject().also {
                it.put(
                    ChatActivity.SOCKET_FROM,
                    preferenceService.getString(R.string.pkey_user_Id) ?: ""
                )
                it.put(ChatActivity.SOCKET_TO, userId)
                it.put(ChatActivity.SOCKET_TYPE, messageType)

                val encryptedMessage = EncryptionViewModel.encryptString(chat.message)
                val encryptedURL = EncryptionViewModel.encryptString(chat.url)
                if (isGroup.value!!) {
                    it.put(ChatActivity.ROOM_ID, roomId.value)
                }
                it.put(
                    ChatActivity.SOCKET_MESSAGE_SENDER,
                    EncryptionViewModel.encryptString(chat.messageSender).replace("\n", "")
                        .replace("\r", "")
                )
                it.put(
                    ChatActivity.SOCKET_MESSAGE_RECEIVER,
                    EncryptionViewModel.encryptString(chat.messageReceiver).replace("\n", "")
                        .replace("\r", "")
                )

                it.put(MESSAGE, encryptedMessage.replace("\n", "").replace("\r", ""))
                it.put(ChatActivity.SOCKET_URL, encryptedURL.replace("\n", "").replace("\r", ""))

                it.put(ChatActivity.IS_FORWARD, chat.forwarded)
            }
        }
    }

    fun deletMessage(chat: Chat): LiveData<NetworkState> {

        val request = userRepository.deleteMessage(chat.id).also { it ->
            subscribe(it.request) {
                if (it.isSuccessful) {
                    it.body()?.let {
                        chatListCache.deleteMessage(chat.id)
                    }
                }
            }
        }
        return request.networkState
    }

    fun markChatAsRead(roomId: String) {
        userRepository.markChatAsRead(listOf(roomId)).also { it ->
            subscribe(it.request) {
                if (it.isSuccessful) {
                    print("chat mark as read")
                }
            }
        }
    }
    val userlanguageList = MutableLiveData<UserLanguageResponseModel.Data>()

    val getsupportData = MutableLiveData<SupportResponse.Data>()

    /*  fun getUserLanguage(roomId: String): LiveData<NetworkState> {
          val networkRequest = userRepository.getUserLanguageResponse(roomId)
          subscribe(networkRequest.request) {
             Log.d( "getUserLanguage: ",Gson().toJson(it))
              userlanguageList.postValue(it.body()?.data)
          }
          return networkRequest.networkState
      }*/
    //val commandList = MutableLiveData<List<UserCommandResponse.Data>>()
    fun getUserLanguage(roomId: String): LiveData<NetworkState> {
        val networkRequest = userRepository.getUserLanguageResponse(roomId)

        viewModelScope.launch {
            subscribe(networkRequest.request) {
                if (it.isSuccessful) {
                   // Log.d("user-language-response:", Gson().toJson(it.body()?.data))

                    //   userlanguageList.postValue(it.body()?.data)

                    userlanguageList.postValue(it.body()?.data)
                } else {
                    Log.d("user-language-response:", "error")
                }
            }
        }
        return networkRequest.networkState
    }

    fun getSupport(): LiveData<NetworkState> {
        val networkRequest = userRepository.getSupportResponse()

        viewModelScope.launch {
            subscribe(networkRequest.request) {
                if (it.isSuccessful) {
                   // Log.d("user-language-response:", Gson().toJson(it.body()?.data))

                    //   userlanguageList.postValue(it.body()?.data)

                    getsupportData.postValue(it.body()?.data)
                } else {
                    Log.d("user-language-response:", "error")
                }
            }
        }
        return networkRequest.networkState
    }

}
