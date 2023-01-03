package com.android.vadify.widgets

import android.content.Context
import android.media.MediaPlayer
import android.telephony.TelephonyManager
import android.util.Log
import com.google.android.gms.tasks.OnCompleteListener
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.messaging.FirebaseMessaging
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.jakewharton.rxbinding3.widget.textChanges
import io.michaelrocks.libphonenumber.android.PhoneNumberUtil
import io.michaelrocks.libphonenumber.android.Phonenumber
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody
import java.io.File
import java.util.*
import java.util.concurrent.TimeUnit
import kotlin.collections.ArrayList

const val DEFAULT_SECOND = 1L
const val FILE_SIZE = 500


fun Context.numberFormat(dialCode: String, countryCode: String, number: String): String {
    val updatedPhoneNumber: String
    updatedPhoneNumber = try {
        val util = PhoneNumberUtil.createInstance(this)
        val phoneNumber: Phonenumber.PhoneNumber = util.parse(number, dialCode)
        util.format(phoneNumber, PhoneNumberUtil.PhoneNumberFormat.INTERNATIONAL)
    } catch (e: Exception) {
        "$countryCode $number"
    }
    return updatedPhoneNumber
}


fun TextInputEditText.onTextChange(ms: Long = DEFAULT_SECOND): Observable<CharSequence>? {
    return this.textChanges().debounce(ms, TimeUnit.SECONDS)
        .observeOn(AndroidSchedulers.mainThread())
}


fun Context.getCountryCode(): String {
    var countryCode = "IN"
    try {
        val tm = this.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
        countryCode = tm.networkCountryIso.toUpperCase(Locale.ROOT)
    } catch (e: Exception) {
    }

    return countryCode

}


fun File.multiPartObject(key: String, fileFormat: String): MultipartBody.Part {
    return MultipartBody.Part.createFormData(
        key,
        this.name,
        RequestBody.create(fileFormat.toMediaTypeOrNull(), this)
    )
}

fun String.requestBody(): RequestBody {
    return RequestBody.create("text/plain".toMediaTypeOrNull(), this)

}

val MediaPlayer.currentSeconds: Int
    get() {
        return this.currentPosition / 1000
    }
val MediaPlayer.seconds: Int
    get() {
        return this.duration / 1000
    }


fun checkFileSize(fileSizeInBytes: Long): Boolean {
    val fileSizeInKB: Long = fileSizeInBytes / 1024
    val fileSizeInMB: Long = fileSizeInKB / 1024.toLong()
    return fileSizeInMB < FILE_SIZE
}

fun getDeviceToken(returnValue: (String?) -> Unit) {
    FirebaseMessaging.getInstance().token.addOnCompleteListener(OnCompleteListener { task ->
        if (!task.isSuccessful) {
            Log.w("device token", "Fetching FCM registration token failed", task.exception)
            return@OnCompleteListener
        }
        returnValue.invoke(task.result)
    })
}


fun <T> restoreList(listOfString: String?): ArrayList<T>? =
    Gson().fromJson(listOfString, object : TypeToken<List<T>>() {}.type)

fun <T> saveList(listOfString: List<T>): String? = Gson().toJson(listOfString)










