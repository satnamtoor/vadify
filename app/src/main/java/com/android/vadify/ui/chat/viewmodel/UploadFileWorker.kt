/*
 * Copyright (C) 2018 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.vadify.ui.chat.viewmodel

//import com.github.nkzawa.socketio.client.Ack
//import com.github.nkzawa.socketio.client.Socket

import ProgressEmittingRequestBody
import android.app.*
import android.content.Context
import android.net.Uri
import android.os.Build
import android.util.Log
import android.webkit.MimeTypeMap
import androidx.core.app.NotificationCompat
import androidx.work.*
import com.abedelazizshe.lightcompressorlibrary.CompressionListener
import com.abedelazizshe.lightcompressorlibrary.VideoCompressor
import com.abedelazizshe.lightcompressorlibrary.VideoQuality
import com.android.vadify.R
import com.android.vadify.VadifyApplication
import com.android.vadify.data.api.ApiClient
import com.android.vadify.data.api.AwsUploadApi
import com.android.vadify.data.api.FileUploadApi
import com.android.vadify.data.api.enums.DownloadUploadStatus
import com.android.vadify.data.api.enums.MessageType
import com.android.vadify.data.api.models.MessageResponseList
import com.android.vadify.data.api.models.UploadAWSResponse
import com.android.vadify.data.api.models.UploadAwsURL
import com.android.vadify.data.db.chat.Chat
import com.android.vadify.data.db.chat.ChatListCache
import com.android.vadify.data.db.filter.ChatListTranslatedText.Mapper.fromObject
import com.android.vadify.data.repository.FileUploadRepositry
import com.android.vadify.data.service.PreferenceService
import com.android.vadify.di.WorkerKey
import com.android.vadify.ui.chat.ChatActivity
import com.android.vadify.utils.LocalStorage.DOWNLOAD_VIDEO_FILE_PATH
import com.android.vadify.utils.LocalStorage.getDownloadFilePath
import com.android.vadify.utils.LocalStorage.getFilePath
import com.android.vadify.utils.RxBus
import com.android.vadify.utils.getLocalDateTimeToUtc
import com.android.vadify.viewmodels.EncryptionViewModel
import com.google.gson.Gson
import dagger.Binds
import dagger.Module
import dagger.multibindings.IntoMap
import io.socket.client.Ack
import io.socket.client.Socket
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.ResponseBody
import org.json.JSONException
import org.json.JSONObject
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.InputStream
import java.io.UnsupportedEncodingException
import java.net.URLDecoder
import java.util.*
import javax.inject.Inject


class UploadFileWorker @Inject constructor(
    var context: Context, params: WorkerParameters,
    private var preferenceService: PreferenceService,
    private var fileUploadApi: FileUploadApi,
    private var awsupload: AwsUploadApi,
    private var mSocke: Socket?,
    private var chatListCache: ChatListCache
) : CoroutineWorker(context, params) {


    // val viewModel: ChatViewModel by viewModels()
    companion object {
         var mSockett: Socket? = null
        fun setSocket(mSocket: Socket) {
            mSockett = mSocket
        }
        const val File: String = "file"
        const val TYPE: String = "type"
        const val MESSAGE: String = "message"
        const val MESSAGE_SENDER: String = "messageSender"
        const val MESSAGE_RECEIVER: String = "messageReceiver"
        const val ANOTHER_ID: String = "anotherId"
        const val DATA: String = "data"
        const val FROM: String = "from"
        const val TO: String = "to"
        const val URL: String = "url"
        const val FILE_FORMAT = "file_format"
        const val ROOM_ID = "roomId"
        const val ORIGNAL_PATH = "orignalLocalPath"
        const val LANGUAGE_CODE = "languageCode"
        const val SOCKET_MESSAGE_SENDER = "messageSender"
        const val SOCKET_MESSAGE_RECEIVER = "messageReceiver"
        const val SOCKET_MESSAGE_REPLY_TO = "replyToMessageId"
        const val IS_GROUP = "is_group"
        const val ROOM_ID_GROUP = "roomid_group"
        const val TAG = "ForegroundWorker"

        const val CHANNEL_ID = "Job progress"
        const val ARG_PROGRESS = "Progress"
        private const val DELAY_DURATION = 100L

    }
     var NOTIFICATION_ID = 1234
    var buf: ByteArray? = null
    var orignallocalPath: String = ""
    var roomId: String = ""
    var path: String? = null
    var fileFormat: String? = null
    var fileType: String? = null
    var result = Result.success()
    var isResultOk= false
    private val notificationManager = context.getSystemService(NotificationManager::class.java)
    lateinit var workerManager: WorkManager
    private val notificationBuilder = NotificationCompat.Builder(applicationContext, CHANNEL_ID)
        .setSmallIcon(R.drawable.logo)

    override suspend fun doWork(): Result {
        if (mSockett==null)
            return result

        workerManager = WorkManager.getInstance(context)
        val random = Random()
        val generatedPassword = java.lang.String.format("%04d", random.nextInt(10000))
        NOTIFICATION_ID = generatedPassword.toInt()
        fileType = inputData.getString(TYPE)
        notificationBuilder.setContentTitle("Sending $fileType...")
        createNotificationChannel()
        path = inputData.getString(File)
        fileFormat = inputData.getString(FILE_FORMAT)

        when (inputData.getString(FILE_FORMAT)) {
            FileUploadRepositry.VIDEO_FILE -> {
                filterRetryFunctionality(path!!, MessageType.VIDEO.value)
                videoUpload(path!!)
            }
            FileUploadRepositry.AUDIO_FILE -> apiCall(MessageType.AUDIO.value)
            FileUploadRepositry.IMAGE_FILE -> apiCall(MessageType.IMAGE.value)
            FileUploadRepositry.DOCUMENT_FILE -> apiCall(MessageType.DOCUMENT.value)
        }

        while (!isResultOk){
            delay(100)
            Log.d( "progress----- ","uploading")
        }
        Log.d( "progress----- ","uploaded")
        return result
    }



    private suspend fun showProgress(progress: Int) {
        val notification = notificationBuilder
            .setProgress(100, progress, false)
            .build()
        val foregroundInfo = ForegroundInfo(NOTIFICATION_ID, notification)
        setForeground(foregroundInfo)
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationChannel = notificationManager?.getNotificationChannel(CHANNEL_ID)
            if (notificationChannel == null) {
                notificationManager?.createNotificationChannel(
                    NotificationChannel(
                        CHANNEL_ID, TAG, NotificationManager.IMPORTANCE_LOW
                    )
                )
            }
        }
    }


    private fun apiCall(filter: String) {

        if (path != null) {
            filterRetryFunctionality(path!!, filter)
            buf = readBytes(Uri.fromFile(File(path!!)))
            callApi()
        }
    }

    /*
    * Check need to perform retry functionality or not
    * */

    fun readBytes(uri: Uri?): ByteArray? {
        // this dynamically extends to take the bytes you read
        var inputStream: InputStream? = null
        try {
             inputStream = uri?.let {
                context.contentResolver.openInputStream(
                    it
                )
            }
        }catch (e:Exception){
            e.printStackTrace()
        }
        val byteBuffer = ByteArrayOutputStream()
        val bufferSize = 1024
        val buffer = ByteArray(bufferSize)
        var len = 0
        while (inputStream?.read(buffer).also {
                if (it != null) {
                    len = it
                }
            } != -1) {
            byteBuffer.write(buffer, 0, len)
        }
        return byteBuffer.toByteArray()
    }

    private fun filterRetryFunctionality(path: String, filter: String) {
        Log.d(
            "new-view_or",
            inputData.getString(ORIGNAL_PATH) + "" + inputData.getString(MESSAGE_RECEIVER) + "--" + inputData.getString(
                MESSAGE_SENDER
            )
        )

        val isRetryPath = inputData.getString(ORIGNAL_PATH) ?: ""
        if (isRetryPath.isNotBlank()) {
            orignallocalPath = isRetryPath
            chatListCache.updateDummyStatus(
                DownloadUploadStatus.IN_PROGRESS.value,
                orignallocalPath
            )
        } else {
            orignallocalPath = path + getLocalDateTimeToUtc()
            chatListCache.insert(
                Chat.Mapper.from(
                    dummyView(filter, path),
                    localFileUrl = path,
                    localOrignalPath = orignallocalPath,
                    downloadStatus = DownloadUploadStatus.IN_PROGRESS.value
                )
            )
        }
    }

    private fun sendMessage(messageBundle: JSONObject, isGroup: Boolean) {

        /* mSocket?.connect()
         if (!mSocket!!.connected())
         {
             mSocket?.connect()
         }*/

        val message = messageBundle

        val finalPayLoad = EncryptionViewModel.encryptString(
            message.toString().replace("\n", "").replace("\r", "")
        )

        val finalData = JSONObject()
        finalData.put("data", finalPayLoad)


        var event = "";

        if (isGroup) {
            event = ChatActivity.GROUP_MESSAGE
        } else {
            event = ChatActivity.MESSAGE
        }

        mSockett?.emit(event, finalData, object : Ack {
            override fun call(vararg args: Any?) {
                CoroutineScope(Dispatchers.Main).launch {

                    val data: JSONObject = args[0] as JSONObject
                    val encryptedPayload = data.getString("data")
                    val decryptedPayload = EncryptionViewModel.decryptString(encryptedPayload)

                    try {
                        val data: JSONObject = args[0] as JSONObject
                        // Log.d("call: response", Gson().toJson(data))
                        Log.d("call--response", data.getString("statusCode"))
                        if (data.getString("statusCode").equals("200")) {
                            Gson().fromJson(
                                decryptedPayload,
                                MessageResponseList.Data.DataX::class.java
                            )?.let {
                                (applicationContext as VadifyApplication).firstTimeRoomId.value =
                                    it.roomId
                                insertDataIntoDataBase(
                                    fromObject(
                                        it,
                                        inputData.getString(LANGUAGE_CODE) ?: ""
                                    )
                                )
                            }
                        }
                    } catch (e: JSONException) {
                        Log.e("error", e.message.toString())
                    }
                }
            }
        })
    }

    fun insertDataIntoDataBase(data: MessageResponseList.Data.DataX) {
        CoroutineScope(Dispatchers.IO).launch {

            val decryptedMessage = EncryptionViewModel.decryptString(data.message)
            val decryptedURL = EncryptionViewModel.decryptString(data.url)

            chatListCache.updateDummyView(
                data.__v,
                data._id,
                data.members,
                decryptedMessage,
                decryptedMessage,
                decryptedMessage,
                data.roomId,
                data.type,
                data.from?._id!!,
                data.from?.name!!,
                data.from?.number!!,
                data.from?.profileImage!!,
                "",
                decryptedURL,
                DownloadUploadStatus.SUCCESS.value,
                false,
                orignallocalPath
            )
        }
    }

    fun getJsonObject(
        url: String?,
        message: String?, anotherUserId: String?,
        messageType: String?, sender: String?,
        reciver: String?, replyTo: String?, isGroup: Boolean
    ) {

        Log.d( "new-viewo",""+sender+"--"+reciver)
        sendMessage(JSONObject().also {
            it.put(FROM, preferenceService.getString(R.string.pkey_user_Id) ?: "")
            it.put(TO, anotherUserId)
            it.put(TYPE, messageType)

            val encryptedMessage = EncryptionViewModel.encryptString(message)
            val encryptedURL = EncryptionViewModel.encryptString(url)


            it.put(
                MESSAGE_SENDER,
                EncryptionViewModel.encryptString(sender).replace("\n", "").replace("\r", "")
            )
            it.put(
                MESSAGE_RECEIVER,
                EncryptionViewModel.encryptString(reciver).replace("\n", "").replace("\r", "")
            )


            // it.put(SOCKET_MESSAGE_SENDER, EncryptionViewModel.encryptString().replace("\n", "").replace("\r", ""))

            it.put(MESSAGE, encryptedMessage.replace("\n", "").replace("\r", ""))


           it.put(URL, encryptedURL.replace("\n", "").replace("\r", ""))

            if (isGroup) {
                it.put(ChatActivity.ROOM_ID, inputData.getString(ROOM_ID))
            }

            replyTo?.let { replyToMessageId ->
                it.put(SOCKET_MESSAGE_REPLY_TO, replyToMessageId)
            }

        }, isGroup)
    }

    @Module
    abstract class Builder {
        @Binds
        @IntoMap
        @WorkerKey(UploadFileWorker::class)
        abstract fun bindHelloWorldWorker(worker: UploadFileWorker): CoroutineWorker
    }

    private fun videoUpload(path: String) {

        val localUrl = getDownloadFilePath(context.getFilePath(DOWNLOAD_VIDEO_FILE_PATH))
        VideoCompressor.start(path, localUrl, object : CompressionListener {
            override fun onProgress(percent: Float) {
                Log.e("progress-----", "" + percent)
                CoroutineScope(Dispatchers.Main).launch {
                    setProgress(workDataOf(ARG_PROGRESS to percent))
                    showProgress(percent.toInt())
                }
              //  chatListCache.updateProgressStatus(percent.toInt(), orignallocalPath)
                VadifyApplication.progress.postValue(hashMapOf(orignallocalPath to percent.toInt()))
            }

            override fun onStart() {
                isResultOk=false
                CoroutineScope(Dispatchers.Main).launch {

                    val notification = notificationBuilder.build()
                    val foregroundInfo = ForegroundInfo(NOTIFICATION_ID, notification)
                    setForeground(foregroundInfo)
                }
            }
            override fun onSuccess() {
               // isResultOk=true
                Log.e("progress-----", "100" )

                inputData.getString(FILE_FORMAT)?.let {
                    inputData.getString(TYPE)?.let { _ ->
                        buf = readBytes(Uri.fromFile(File(localUrl!!)))

                       /* CoroutineScope(Dispatchers.Main).launch {
                            setProgress(workDataOf(ARG_PROGRESS to 100))
                            showProgress(100)
                        }*/
                        //  chatListCache.updateProgressStatus(percent.toInt(), orignallocalPath)
                        VadifyApplication.progress.postValue(hashMapOf(orignallocalPath to 100))
                        workerManager.cancelWorkById(this@UploadFileWorker.id)
                        callApi()
                    }
                }
            }

            override fun onFailure(failureMessage: String) {
                callFailureMethod()
            }


            override fun onCancelled() {}
        }, VideoQuality.LOW, isMinBitRateEnabled = false, keepOriginalResolution = false)

    }

    fun callFailureMethod() {
        workerManager.cancelWorkById(this@UploadFileWorker.id)
        CoroutineScope(Dispatchers.IO).launch {
            chatListCache.updateDummyStatus(DownloadUploadStatus.FAILED.value, orignallocalPath)
        }
    }


    private fun callApi() {

        val name = File(inputData.getString(File)).name
        val videoFile = File(inputData.getString(File))
        val contantType=  getMimeType(getMimeType1(inputData.getString(File).toString())).toString();

        Log.d("new-name", "" + name +" " +contantType + " "+ inputData.getString(TYPE)!!)

        val call = awsupload.uploadImageWithProgress(
            UploadAwsURL(
                inputData.getString(TYPE)!!,
                contantType,
                name
            )
        )
        call.enqueue(object : Callback<UploadAWSResponse> {
            override fun onFailure(call: Call<UploadAWSResponse>, t: Throwable) {
                callFailureMethod()
            }

            override fun onResponse(
                call: Call<UploadAWSResponse>,
                response: Response<UploadAWSResponse>
            ) {
                if (response.isSuccessful) {
                    var imagePathUrl = response.body()?.data?.uploadUrl?.split("?")?.get(0)
                    Log.d("onResponse:upload ", ""+imagePathUrl)

                   val requestFile = buf?.toRequestBody(
                        "application/octet-stream".toMediaTypeOrNull(),
                        0,
                        buf!!.size
                    )
                   // showProgress(50)ppppppppp
                    val videoRequestBody = ProgressEmittingRequestBody(mediaType = "video/*", file = videoFile)

                    val vFile = MultipartBody.Part.createFormData(inputData.getString(TYPE)!!, videoFile.name, videoRequestBody)

                    val call1 = ApiClient.getClient.uploadAsset(response.body()?.data?.uploadUrl.toString(),requestFile!!)
                    call1.enqueue(object : Callback<ResponseBody> {
                        override fun onFailure(call: Call<ResponseBody>, t: Throwable) {
                            callFailureMethod()
                        }

                        override fun onResponse(
                            call: Call<ResponseBody>,
                            response: Response<ResponseBody>
                        ) {

                            if (response.code()==200) {
                                Log.d(TAG, "onResponse:callingggg ")
                                workerManager.cancelWorkById(this@UploadFileWorker.id)

                                getJsonObject(
                                    imagePathUrl,
                                    inputData.getString(MESSAGE),
                                    inputData.getString(ANOTHER_ID),
                                    inputData.getString(TYPE),
                                    inputData.getString(MESSAGE_SENDER),
                                    inputData.getString(MESSAGE_RECEIVER),
                                    inputData.getString(SOCKET_MESSAGE_REPLY_TO),
                                    inputData.getBoolean(IS_GROUP, false)
                                )

                            }
                        }
                    })

                }
            }
        })

    }

    fun getMimeType1(fallback: String = "image/*"): String {
        return MimeTypeMap.getFileExtensionFromUrl(toString())
            ?.run { MimeTypeMap.getSingleton().getMimeTypeFromExtension(toLowerCase()) }
            ?: fallback // You might set it to */*
    }

    fun getMimeType(url: String?): String? {
        var type: String? = null
        val extension = MimeTypeMap.getFileExtensionFromUrl(url)
        if (extension != null) {
            type = MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension)
        }
        return type
    }

    private fun dummyView(typeOfView: String, path: String): MessageResponseList.Data.DataX {
        val currentDateTime = getLocalDateTimeToUtc()
        RxBus.publish("Message Sent")
        var replyTo = inputData.getString(SOCKET_MESSAGE_REPLY_TO) ?: ""

        val messageResponseList = MessageResponseList.Data.DataX(
            -1,
            "",
            getLocalDateTimeToUtc(),
            currentDateTime,
            MessageResponseList.Data.DataX.From(
                preferenceService.getString(R.string.pkey_user_Id) ?: "", "", "", "", ""
            ),
            emptyList(),
            EncryptionViewModel.encryptString(inputData.getString(MESSAGE) ?: "").replace("\n", "")
                .replace("\r", ""),
            EncryptionViewModel.encryptString(inputData.getString(MESSAGE_SENDER) ?: "")
                .replace("\n", "").replace("\r", ""),
            EncryptionViewModel.encryptString(inputData.getString(MESSAGE_RECEIVER) ?: "")
                .replace("\n", "").replace("\r", ""),
            inputData.getString(ROOM_ID) ?: preferenceService.getString(R.string.pkey_user_Id)
            ?: "",
            typeOfView,
            currentDateTime,
            path,
            "",
            "",
            MessageResponseList.Data.DataX.Contact("", ""),
            "",
            replyTo,
            false, "",0
        )
        return messageResponseList
    }

    fun decodeMessage(message: String?): String? {
        return try {
            URLDecoder.decode(message, "UTF-8")

        } catch (e: UnsupportedEncodingException) {
            message
        }
    }
}
