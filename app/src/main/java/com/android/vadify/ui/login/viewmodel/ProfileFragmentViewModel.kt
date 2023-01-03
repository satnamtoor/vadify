package com.android.vadify.ui.login.viewmodel

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.android.vadify.R
import com.android.vadify.data.api.models.*
import com.android.vadify.data.db.commands.Commands
import com.android.vadify.data.network.IRequest
import com.android.vadify.data.repository.FileUploadRepositry
import com.android.vadify.data.repository.UserRepository
import com.android.vadify.data.service.PreferenceService
import com.android.vadify.ui.util.ResourceViewModel
import com.android.vadify.utils.ProfileModel
import com.google.gson.Gson
import com.sdi.joyersmajorplatform.common.livedataext.map
import com.sdi.joyersmajorplatform.common.livedataext.mutableLiveData
import com.sdi.joyersmajorplatform.uiview.NetworkState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import retrofit2.Response
import java.io.File
import java.util.*
import javax.inject.Inject

class ProfileFragmentViewModel @Inject constructor(
    private val fileUploadRepositry: FileUploadRepositry,
    private val userRepository: UserRepository,
    val preferenceService: PreferenceService
) :
    ProfileModel(preferenceService) {

    var defaultDrawable = R.drawable.circle_with_white_color
    val imagePath = mutableLiveData(preferenceService.getString(R.string.pkey_profileImage))
    val userName = mutableLiveData(preferenceService.getString(R.string.pkey_userName))
    val commandList = MutableLiveData<List<UserCommandResponse.Data>>()

    val languageList = MutableLiveData<List<AllLanguageResponse.Data>>()

    val responseOK = mutableLiveData("fail")
    val motherTounge = mutableLiveData(isMotherLanguageAvailable())

    fun puttlanguageCode(languageCode: String) {


        preferenceService.putString(R.string.pkey_motherLanguage_Code, languageCode)
    }

    fun saveCommandData(command: Commands): LiveData<com.sdi.joyersmajorplatform.uiview.NetworkState> {
        val networkRequest = userRepository.saveCommandsApi(command)
        //  val networkRequest = userRepository.updateProfile(uploadData(getProfileRequestModel()))
        subscribe(networkRequest.request) {
            Log.d("saveCommandData: ", "calling-2")
        }
        return networkRequest.networkState

    }

    fun getLanguageUserResponse(): LiveData<NetworkState> {
        val networkRequest = userRepository.getUserCommands()
        subscribe(networkRequest.request) {

            //
            //Log.d( "default-keyword_response: ",Gson().toJson(it))
            commandList.postValue(it.body()?.data)
        }
        return networkRequest.networkState
    }

    fun getAllLanguageResponseMo(): LiveData<NetworkState> {
        val networkRequest = userRepository.getAllLanguageResponse()
        subscribe(networkRequest.request) {
            //Log.d( "response--",Gson().toJson(it.body()))
            val dataa = it.body()?.data!!.filter {
                it.isActive && (it.supportedOS.equals("android") || it.supportedOS.equals("both"))
            }
            languageList.postValue(dataa)
        }
        return networkRequest.networkState
    }

    fun compressedImageFile(file: File): LiveData<NetworkState> {
        return fileUploadRepositry.uploadProfilePic(file, "profile").also {
            subscribe(it.request) {
                if (it.isSuccessful) {
                    imagePath.postValue(it.body()?.data?.url)
                    preferenceService.putString(R.string.pkey_profileImage, it.body()?.data?.url)

                }
            }
        }.networkState
    }


    var isImagePathEmptyOrNot = imagePath.map {
        !it.isNullOrBlank()
    }


    fun updateProfileData(name: String): LiveData<com.sdi.joyersmajorplatform.uiview.NetworkState> {
        //  preferenceService.putString(R.string.pkey_userName, name)
        val networkRequest = userRepository.updateProfile(uploadData(getProfileRequestModel(),name))
        subscribe(networkRequest.request) {

            // Log.d("profile-response", Gson().toJson(it.body()))

            if (it.isSuccessful) {

                response(name)
            }
        }
        return networkRequest.networkState
    }


    fun getLanguages(): List<String>? {
        var list = Locale.getAvailableLocales().filter {
            it.language.length == 2
        }.map {
            it.displayLanguage
        }
        list = list.distinct()
        return list
    }


    private fun response(name: String) {
        //     Log.d("language_name", ""+ motherTounge.value)
        preferenceService.putString(R.string.pkey_userName, name)
        preferenceService.putString(R.string.pkey_motherLanguage, motherTounge.value)


        preferenceService.putString(R.string.pkey_profileImage, imagePath.value)

    }


    fun filterMethod(callBack: (Boolean, Int?) -> Unit) {
        // Log.d("mother-language", motherTounge.value.toString())
        when {


            userName.value.isNullOrBlank() -> callBack(false, R.string.name_validation)
            // motherTounge.value.isNullOrBlank() || motherTounge.value.equals("Select Language")-> callBack(false, R.string.language_validation)
            else -> callBack(true, null)

        }
    }


    private fun uploadData(profileRequestModel: ProfileUpdateRequestModel,name:String): ProfileUpdateRequestModel {
        return profileRequestModel.also {
            it.language = motherTounge.value.toString()
            it.languageCode = preferenceService.getString(R.string.pkey_motherLanguage_Code)
            it.name = name
            it.profileImage = imagePath.value.toString()
        }
    }


    private fun isMotherLanguageAvailable(): String? {
        return if (!preferenceService.getString(R.string.pkey_motherLanguage).isNullOrBlank())
            preferenceService.getString(R.string.pkey_motherLanguage)
        else

            Locale.getDefault().displayLanguage
    }
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


}