package com.android.vadify.ui.chat.viewmodel

import android.content.Intent
import com.android.vadify.data.repository.UserRepository
import com.android.vadify.data.service.PreferenceService
import com.android.vadify.ui.chat.camera.CameraActivity
import com.android.vadify.utils.BaseViewModel
import com.sdi.joyersmajorplatform.common.livedataext.mutableLiveData
import java.io.UnsupportedEncodingException
import java.net.URLEncoder
import javax.inject.Inject

class CameraViewModel @Inject constructor(
    private val userRepository: UserRepository,
    private val preferenceService: PreferenceService
) : BaseViewModel() {


    var imageUrl = mutableLiveData("")
    var message = mutableLiveData("")
    var isVideoRecording = mutableLiveData(false)
    var isVideoAvailable = mutableLiveData(false)


    fun getIntent(): Intent {
        return Intent().also {
            it.putExtra(CameraActivity.URL, imageUrl.value)
            it.putExtra(CameraActivity.MESSAGE,message.value)
        }
    }

    fun filterMethod(callBack: (Boolean) -> Unit) {
        when {
            imageUrl.value.isNullOrBlank() -> callBack(false)
            else -> callBack(true)
        }
    }



    fun encodeMessage(message: String?): String? {
        return try {
            URLEncoder.encode(message,
                    "UTF-8")
        } catch (e: UnsupportedEncodingException) {
            message
        }
    }
}