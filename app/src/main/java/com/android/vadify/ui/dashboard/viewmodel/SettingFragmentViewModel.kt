package com.android.vadify.ui.dashboard.viewmodel

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.android.vadify.R
import com.android.vadify.data.api.models.LocalLanguageModel
import com.android.vadify.data.repository.FileUploadRepositry
import com.android.vadify.data.repository.UserRepository
import com.android.vadify.data.service.PreferenceService
import com.android.vadify.utils.ProfileModel
import com.google.gson.Gson
import com.sdi.joyersmajorplatform.common.livedataext.mutableLiveData
import com.sdi.joyersmajorplatform.uiview.NetworkState
import java.io.File
import java.util.ArrayList
import javax.inject.Inject

class SettingFragmentViewModel @Inject constructor(
    private val userRepository: UserRepository,
    private val preferenceService: PreferenceService,
    private val fileUploadRepositry: FileUploadRepositry

) : ProfileModel(preferenceService) {
    var localLanguage: ArrayList<LocalLanguageModel.LocalLanguageModelItem>? = null
    var defaultDrawable = R.drawable.user_placeholder
    var userName = mutableLiveData(preferenceService.getString(R.string.pkey_userName))
    var email = mutableLiveData(preferenceService.getString(R.string.pkey_emailId))
    var motherLanguage = mutableLiveData(preferenceService.getString(R.string.pkey_motherLanguage))
    var status = mutableLiveData(preferenceService.getString(R.string.pkey_status))
    var phoneNumber = mutableLiveData(
        preferenceService.getString(R.string.pkey_countryCode) + " " + preferenceService.getString(R.string.pkey_phone)
    )
    var profileImage = mutableLiveData(preferenceService.getString(R.string.pkey_profileImage))
    var profileImageHome = mutableLiveData(preferenceService.getString(R.string.pkey_profileImage_home))

    fun updateProfileInformation() {
        userName.value = preferenceService.getString(R.string.pkey_userName)
        email.value = preferenceService.getString(R.string.pkey_emailId)
        motherLanguage.value = preferenceService.getString(R.string.pkey_motherLanguage)
        status.value = preferenceService.getString(R.string.pkey_status)
        phoneNumber.value =
            preferenceService.getString(R.string.pkey_countryCode) + " " + preferenceService.getString(
                R.string.pkey_phone
            )
    }

    var settingList = MutableLiveData<List<String>>()

    fun updateSettingList(settings: List<String>) {
        settingList.value = settings
    }

    fun getlanguageCode() =
        preferenceService.getString(R.string.pkey_motherLanguage_Code, "English")

    fun isNotificationUpdate(): Boolean {
        return preferenceService.getBoolean(R.string.pkey_needToUpdateNotificationData)
    }

    fun updateNotificationData(): LiveData<NetworkState> {
        val networkRequest = userRepository.updateProfile(getProfileRequestModel())
         subscribe(networkRequest.request) {
            if (it.isSuccessful) {
              //  Log.i("notification---", "" + Gson().toJson(it))
                preferenceService.putBoolean(R.string.pkey_needToUpdateNotificationData, false)
            }
        }
        return networkRequest.networkState
    }

    fun compressedImageFile(file: File): LiveData<NetworkState> {
        val networkRequest = fileUploadRepositry.uploadProfilePic(file, "profile")
        subscribe(networkRequest.request) {
            if (it.isSuccessful) {
                preferenceService.putString(R.string.pkey_profileImage, it.body()?.data?.url)
                Log.d( "compressedImageFile: ",Gson().toJson(it.body()?.data?.url))
                profileImage.postValue(it.body()?.data?.url)
            }
        }
        return networkRequest.networkState
    }

    /*fun compressedImageFileHome(file: File): LiveData<NetworkState> {
        val networkRequest = fileUploadRepositry.uploadProfilePic(file, "profile")
        subscribe(networkRequest.request) {
            if (it.isSuccessful) {

                preferenceService.putString(R.string.pkey_profileImage_home, it.body()?.data?.url)
                Log.d( "compressedImageFile: ",Gson().toJson(it.body()?.data?.url))
                profileImageHome.postValue(it.body()?.data?.url)
            }
        }
        return networkRequest.networkState
    }*/

    fun checkLanguageCodeForSpeech(language: String): String? {
        val listOfObject = localLanguage?.find { it.language.equals(language, ignoreCase = true) }
        return listOfObject?.code
    }
}