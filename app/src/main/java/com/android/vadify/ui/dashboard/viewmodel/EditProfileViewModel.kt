package com.android.vadify.ui.dashboard.viewmodel

import android.util.Log
import android.util.Patterns
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.android.vadify.R
import com.android.vadify.data.api.models.AllLanguageResponse
import com.android.vadify.data.api.models.ProfileUpdateRequestModel
import com.android.vadify.data.repository.UserRepository
import com.android.vadify.data.service.PreferenceService
import com.android.vadify.utils.ProfileModel
import com.google.gson.Gson
import com.sdi.joyersmajorplatform.common.livedataext.mutableLiveData
import com.sdi.joyersmajorplatform.uiview.NetworkState
import java.util.*
import javax.inject.Inject

class EditProfileViewModel @Inject constructor(
    private val userRepository: UserRepository,
    private val preferenceService: PreferenceService
) :
    ProfileModel(preferenceService) {

    val languageList = MutableLiveData<List<AllLanguageResponse.Data>>()
    var name = mutableLiveData(preferenceService.getString(R.string.pkey_userName))
    var motherLanguage = mutableLiveData(preferenceService.getString(R.string.pkey_motherLanguage))
    var languageCodeNew =
        mutableLiveData(preferenceService.getString(R.string.pkey_motherLanguage_Code))
    var status = mutableLiveData(preferenceService.getString(R.string.pkey_status))
    var emailId = mutableLiveData(preferenceService.getString(R.string.pkey_emailId))


    fun getLanguages(): List<String>? {
        var list = Locale.getAvailableLocales().filter { it.language.length == 2 }
            .map { it.displayLanguage }
        list = list.distinct()
        return list
    }

    fun puttlanguageCode(languageCode: String) {
        val language = preferenceService.putString(R.string.pkey_motherLanguage_Code, languageCode)
    }

    fun updateProfileData(): LiveData<NetworkState> {
        val networkRequest = userRepository.updateProfile(uploadData(getProfileRequestModel()))
        subscribe(networkRequest.request) {
            // Log.d( "profile-response: ",Gson().toJson(it.body()))
            if (it.isSuccessful) {
                response()
            }
        }
        return networkRequest.networkState
    }

    fun getAllLanguageResponseMo(): LiveData<NetworkState> {
        val networkRequest = userRepository.getAllLanguageResponse()
        subscribe(networkRequest.request) {
          //  Log.d("response--", Gson().toJson(it.body()))
            val dataa = it.body()?.data!!.filter {
                it.isActive && (it.supportedOS.equals("android") || it.supportedOS.equals("both"))
            }
            languageList.postValue(dataa)
        }

        return networkRequest.networkState
    }

    private fun response() {
        preferenceService.putString(R.string.pkey_userName, name.value)
        preferenceService.putString(R.string.pkey_emailId, emailId.value)
        preferenceService.putString(R.string.pkey_motherLanguage, motherLanguage.value)
        preferenceService.putString(R.string.pkey_status, status.value)
        preferenceService.putBoolean(R.string.pkey_languageChateStatus, true)
    }


    private fun uploadData(profileRequestModel: ProfileUpdateRequestModel): ProfileUpdateRequestModel {
        return profileRequestModel.also {
            it.name = name.value.toString()
            it.languageCode = preferenceService.getString(R.string.pkey_motherLanguage_Code)
            it.email = emailId.value.toString()
            it.language = motherLanguage.value.toString()
            it.profileStatus = status.value.toString()
        }
    }


    fun filterMethod(callBack: (Boolean, Int?) -> Unit) {
        when {
            name.value.isNullOrBlank() -> callBack(false, R.string.name_validation)
            !emailId.value.isNullOrBlank()&& !Patterns.EMAIL_ADDRESS.matcher(emailId.value).matches()
            -> callBack(false, R.string.email_validation)
          //  motherLanguage.value.isNullOrBlank() -> callBack(false, R.string.language_validation)
            else -> callBack(true, null)

        }
    }

    fun filterMethodN(callBack: (Boolean, Int?) -> Unit) {
        when {
            name.value.isNullOrBlank() -> callBack(false, R.string.name_validation)
            !emailId.value.isNullOrBlank()&& !Patterns.EMAIL_ADDRESS.matcher(emailId.value).matches()
            -> callBack(false, R.string.email_validation)
            motherLanguage.value.isNullOrBlank() -> callBack(false, R.string.language_validation)
            else -> callBack(true, null)

        }
    }

}

