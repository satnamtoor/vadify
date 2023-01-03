package com.android.vadify.ui.chat.viewmodel

import android.util.Log
import androidx.lifecycle.MutableLiveData
import com.android.vadify.BuildConfig
import com.android.vadify.R
import com.android.vadify.data.api.models.TextToSpeechResponse
import com.android.vadify.utils.BaseViewModel
import com.google.cloud.translate.TranslateOptions
import com.google.gson.Gson
import com.sdi.joyersmajorplatform.common.livedataext.mutableLiveData
import com.sdi.joyersmajorplatform.uiview.NetworkState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import org.json.JSONObject
import java.io.IOException
import java.util.*

abstract class TextToSpeechModel : BaseViewModel() {


    var audioFile = ""
    var messageText = mutableLiveData("")
    var networkState: MutableLiveData<NetworkState> = MutableLiveData()
    var localNetworkState: MutableLiveData<NetworkState> = MutableLiveData()

    private fun requestModel(languageCode: String): Request? {
        val body: RequestBody = RequestBody.create(
            "application/json; charset=utf-8".toMediaTypeOrNull(),
            jsonRequestModel(messageText.value.toString(), languageCode)
        )
        return Request.Builder()
            .url(BuildConfig.GOOGLE_API_URL)
            .addHeader("X-Goog-Api-Key", BuildConfig.GOOGLE_API_KEY)
            .addHeader("Content-Type", "application/json; charset=utf-8")
            .post(body)
            .build()
    }

    private fun jsonRequestModel(message: String, languageCode: String): String {
        return JSONObject().also {
            val textObject = JSONObject().also { it.put("text", message) }
            val voiceObject = JSONObject().also {
                it.put("languageCode", languageCode)
                it.put("ssmlGender", "NEUTRAL")
            }
            val audioConfigObject = JSONObject().also {
                it.put("audioEncoding", "MP3")
                it.put("speakingRate", "1.0")
                it.put("pitch", "0.0")
            }
            it.put("input", textObject)
            it.put("voice", voiceObject)
            it.put("audioConfig", audioConfigObject)

        }.toString()
    }


    fun googleTextToSpeech(text: String) {


        audioFile = ""
        networkState.postValue(NetworkState.loading)
        CoroutineScope(Dispatchers.IO).launch {
            val contactList = async { getLanguageCodeFromText(text) }
            requestModel(contactList.await())?.let {
                OkHttpClient().newCall(it).enqueue(object : Callback {
                    override fun onFailure(call: Call, e: IOException) {
                        networkState.postValue(NetworkState.error(R.string.speechRecoganizationProcess))
                    }

                    override fun onResponse(call: Call, response: Response) {
                        response?.let {


                            if (it.isSuccessful) {
                                Gson().fromJson(
                                    response.body?.string(),
                                    TextToSpeechResponse::class.java
                                )?.let {
                                    if (it.audioContent.isBlank()) {
                                        networkState.postValue(NetworkState.error(R.string.speechRecoganizationProcess))
                                    } else {
                                        audioFile = it.audioContent
                                        networkState.postValue(NetworkState.success)

                                    }
                                }
                            } else networkState.postValue(NetworkState.error(R.string.speechRecoganizationProcess))
                        }
                    }
                })
            }
        }
    }

    fun getLanguageCodeFromText(message: String): String {
        Log.d("language_code_1",message)
        var languageCode = "en"
        try {
            val texts: MutableList<String> = LinkedList()
            texts.add(message)
            val detections = TranslateOptions.getDefaultInstance().service.detect(texts)
            if (!detections.isNullOrEmpty()) {
                languageCode =
                    detections.singleOrNull {
                        Log.d("language_",it.language)
                        !it.language.isNullOrEmpty() }?.language ?: ""

                Log.d("language_code",languageCode)


            }
        } catch (e: Exception) {
            Log.e("message", "message")
        }
        return languageCode
    }


}