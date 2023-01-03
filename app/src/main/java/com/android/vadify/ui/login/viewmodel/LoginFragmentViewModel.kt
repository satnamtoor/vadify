package com.android.vadify.ui.login.viewmodel

import android.annotation.SuppressLint
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.android.vadify.R
import com.android.vadify.data.api.models.ChangeNumberRequestModel
import com.android.vadify.data.api.models.ChangeNumberWithOtpModel
import com.android.vadify.data.api.models.LoginRequestModel
import com.android.vadify.data.api.models.LoginResponse
import com.android.vadify.data.repository.UserRepository
import com.android.vadify.data.service.PreferenceService
import com.android.vadify.utils.BaseViewModel
import com.android.vadify.viewmodels.EncryptionViewModel
import com.android.vadify.widgets.getDeviceToken
import com.android.vadify.widgets.saveList
import com.cossacklabs.themis.SecureCell
import com.cossacklabs.themis.SecureCell.ContextImprint
import com.google.gson.Gson
import com.sdi.joyersmajorplatform.common.livedataext.mutableLiveData
import com.sdi.joyersmajorplatform.uiview.NetworkState
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.rxkotlin.subscribeBy
import io.reactivex.schedulers.Schedulers
import java.util.*
import java.util.regex.Matcher
import java.util.regex.Pattern
import javax.inject.Inject


class LoginFragmentViewModel @Inject constructor(
    private val userRepository: UserRepository,
    private val preferenceService: PreferenceService,
) : BaseViewModel() {

    val countryName = mutableLiveData("")
    val countryCode = mutableLiveData("")
    val dialCode = mutableLiveData("")
    val countryFlag = mutableLiveData("")
    val phoneNumber = MutableLiveData<String>()
    val languageFlag = MutableLiveData<String>()

    var tempPhoneNumber = MutableLiveData<String>()
    val otpCode = mutableLiveData("")

    var existingCountryCode =
        mutableLiveData(preferenceService.getString(R.string.pkey_countryCode))
    var existingPhoneNumber = mutableLiveData(preferenceService.getString(R.string.pkey_phone))


    init {
        checkDeviceToken()
    }

    fun updateOtpCode(otpCode: String) {
        this.otpCode.value = otpCode
    }

    fun updatePhoneNumber(countryPostalCode: String, formatedNumber: String, number: String) {
        this.phoneNumber.value = number
        countryCode.value = countryPostalCode
        tempPhoneNumber.value = formatedNumber
    }

    fun checkDeviceToken() {
        preferenceService.getString(R.string.pkey_device_token)?.let {
            Log.e("information are", "" + it)
            if (it.isBlank()) {
                getDeviceToken { preferenceService.putString(R.string.pkey_device_token, it) }
            }
        } ?: run { getDeviceToken { preferenceService.putString(R.string.pkey_device_token, it) } }
    }

    fun loginApiHit(
        countryCode: String,
        phoneNumber: String,
        name: String,
    ): LiveData<NetworkState> {
//        var deviceToken: String = ""
//        if (preferenceService.getString(R.string.pkey_device_token).equals("")) {
//            getDeviceToken {
//                if (!it.isNullOrBlank()) {
//                    deviceToken = it
//                }
//            }
//        } else {
//            deviceToken = preferenceService.getString(R.string.pkey_device_token)!!
        //  }
        // Log.d( "loginApiHit: ")

        val otpCodeRequest = LoginRequestModel(
            countryCode.replace("-", ""),
            preferenceService.getString(R.string.pkey_device_token),
            "android",
            otpCode.value,
            phoneNumber.replace("-", "")
        )

        val dataString = Gson().toJson(otpCodeRequest);
        Log.d("otpVerfication: ", dataString)

        val cell: ContextImprint = SecureCell.ContextImprintWithKey(
            preferenceService.getString(R.string.pkey_master_key)?.toByteArray(
                Charsets.UTF_8
            )
        )
        val encrypt = cell.encrypt(
            dataString.toByteArray(Charsets.UTF_8),
            "login"?.toByteArray(Charsets.UTF_8)
        );

        val encryptedString =
            EncryptionViewModel.encodeBase64String(encrypt)?.toString(Charsets.UTF_8)!!

        Log.d("otpVerfication: ", encryptedString)

        val request = userRepository.signInApi(
            UserRepository.OtpEncriptedRequestModel(encryptedString.trim())
        ).also { it ->
            subscribe(it.request) {
                if (it.isSuccessful) {
                    it.body()?.let {
                        updateDefaultLanguageSwitch(it)
                    }
                }
            }
        }

        return request.networkState
    }

    @SuppressLint("CheckResult")
    fun updateDefaultLanguageSwitch(loginResponse: LoginResponse) {

        updateResponse(loginResponse)

        userRepository.updateLanguageSwitchState(true).subscribeOn(Schedulers.io()).observeOn(
            AndroidSchedulers.mainThread()
        ).subscribeBy(
            onSuccess = {
                if (it.isSuccessful) {
                    preferenceService.putBoolean(R.string.pkey_language_switch, true)
                }
            },
            onError = { })
    }


    fun phoneNumberFilterMethod(callBack: (Boolean, Int?) -> Unit) {
        when {
            phoneNumber.value.isNullOrBlank() || phoneNumber.value.toString().length < 10 -> callBack(
                false,
                R.string.phone_validation
            )

            else -> callBack(true, null)

        }
    }

    fun filterMethod(callBack: (Boolean, Int?) -> Unit) {
        when {
            otpCode.value.isNullOrBlank() || otpCode.value.toString().length < 4 -> {
                callBack(false, R.string.otp_validation)
            }
            else -> {
                callBack(true, null)
            }
        }
    }


    fun otpVerfication(countryCode: String, phoneNumber: String): LiveData<NetworkState> {
        val otpCodeRequest = OtpRequestModel(
            countryCode.replace("-", ""),
            phoneNumber.replace("-", "")
        )
        val dataString = Gson().toJson(otpCodeRequest);
        Log.d("otpVerfication: ", dataString)

        val cell: ContextImprint = SecureCell.ContextImprintWithKey(
            preferenceService.getString(R.string.pkey_master_key)?.toByteArray(
                Charsets.UTF_8
            )
        )

        val encrypt = cell.encrypt(
            dataString.toByteArray(Charsets.UTF_8),
            "get-otp"?.toByteArray(Charsets.UTF_8)
        );

        val encryptedString =
            EncryptionViewModel.encodeBase64String(encrypt)?.toString(Charsets.UTF_8) ?: ""
        Log.d("otpVerfication: ", encryptedString)

        val request = userRepository.otpVerification(
            OtpEncriptedRequestModel(encryptedString)
        ).also { it ->
            subscribe(it.request)
        }
        return request.networkState
    }

    fun changePasswordRequest(): LiveData<NetworkState> {
        val request = userRepository.changePasswordRequest(
            ChangeNumberRequestModel(
                existingCountryCode.value,
                existingPhoneNumber.value,
                countryCode.value?.replace("-", ""),
                phoneNumber.value?.replace("-", "")
            )
        ).also { it ->
            subscribe(it.request)
        }
        return request.networkState
    }


    fun changePasswordWithOtpReqeust(
        newCountryCode: String?,
        newPhone: String,
    ): LiveData<NetworkState> {
        val request = userRepository.changePasswordWithOtpRequest(
            ChangeNumberWithOtpModel(
                existingCountryCode.value, existingPhoneNumber.value, newCountryCode,
                newPhone.replace("-", ""), otpCode.value
            )
        ).also { it ->
            subscribe(it.request) {
                if (it.isSuccessful) {
                    preferenceService.putString(R.string.pkey_countryCode, countryCode.value)
                    preferenceService.putString(R.string.pkey_phone, phoneNumber.value)
                }
            }
        }
        return request.networkState
    }


    fun updateCountryData(countryPinCode: String, countryShortName: String) {
        preferenceService.putString(
            R.string.pkey_motherLanguage,
            Locale.getDefault().displayLanguage
        )
        countryName.value = Locale("", countryShortName).displayCountry
        countryCode.value = if (countryPinCode.contains("+")) countryPinCode else "+$countryPinCode"
        dialCode.value = countryShortName
    }

    fun updateCountryInformation(country: String, code: String, countryFlag: String) {
        countryName.value = country
        countryCode.value = code
        dialCode.value = code
        flagSet(countryFlag)
    }

    fun flagSet(countryCode: String) {
        val flagOffset = 0x1F1E6
        val asciiOffset = 0x41
        val firstChar = Character.codePointAt(countryCode, 0) - asciiOffset + flagOffset
        val secondChar = Character.codePointAt(countryCode, 1) - asciiOffset + flagOffset
        val flag = (String(Character.toChars(firstChar)) + String(Character.toChars(secondChar)))
        countryFlag.value = flag
    }


    fun parseCode(message: String): String {
        val p: Pattern = Pattern.compile("\\b\\d{4}\\b")
        val m: Matcher = p.matcher(message)
        var code = ""
        while (m.find()) code = m.group(0) ?: ""
        return code
    }


    data class OtpRequestModel(val countryCode: String?, val phone: String?)
    data class OtpEncriptedRequestModel(val data: String?)

    // TODO USER DATABASE INSTEAD OF SHARED PREFERENCE
    private fun updateResponse(loginResponse: LoginResponse) {
        // Log.d( "updateResponse: ", Gson().toJson(loginResponse))
        preferenceService.putString(R.string.pkey_user_Id, loginResponse.data._id)
        preferenceService.putString(R.string.pkey_secure_token, loginResponse.data.token)
        preferenceService.putString(R.string.pkey_userName, loginResponse.data.name)
        preferenceService.putBoolean(
            R.string.pkey_groupNotification,
            loginResponse.data.groupNoficaction
        )
        preferenceService.putString(
            R.string.pkey_groupPermission,
            loginResponse.data.groupPermission
        )
        preferenceService.putBoolean(R.string.pkey_inAppSound, loginResponse.data.inAppSound)
        preferenceService.putBoolean(R.string.pkey_inAppVibrate, loginResponse.data.inAppVibrate)
        preferenceService.putString(R.string.pkey_lastLogin, loginResponse.data.lastLogin)
        preferenceService.putBoolean(
            R.string.pkey_liveLocationShare,
            loginResponse.data.liveLocationShare
        )
        preferenceService.putBoolean(
            R.string.pkey_messageNoficaction,
            loginResponse.data.messageNoficaction
        )
        preferenceService.putString(R.string.pkey_phone, loginResponse.data.phone)
        preferenceService.putString(R.string.pkey_profileImage, loginResponse.data.profileImage)
        preferenceService.putBoolean(
            R.string.pkey_securityNotification,
            loginResponse.data.securityNotification
        )
        preferenceService.putBoolean(R.string.pkey_showPreview, loginResponse.data.showPreview)
        preferenceService.putString(R.string.pkey_number, loginResponse.data.number)
        preferenceService.putString(R.string.pkey_emailId, loginResponse.data.email)
        preferenceService.putString(R.string.pkey_status, loginResponse.data.profileStatus)
        preferenceService.putString(R.string.pkey_motherLanguage, loginResponse.data.language)
        preferenceService.putString(
            R.string.pkey_groupPermission,
            loginResponse.data.groupPermission
        )
        preferenceService.putString(R.string.pkey_dialCode, dialCode.value)
        preferenceService.putString(R.string.pkey_countryCode, loginResponse.data.countryCode)
        preferenceService.putBoolean(
            R.string.pkey_saveToCameraRole,
            loginResponse.data.saveToCameraRole
        )
        preferenceService.putString(
            R.string.pkey_blocked_user,
            saveList(loginResponse.data.blockedUsers)
        )

    }

    fun putToken(it: String) {
        preferenceService.putString(R.string.pkey_device_token, it)
    }
}