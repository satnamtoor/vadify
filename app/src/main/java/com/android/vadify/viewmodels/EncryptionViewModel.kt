package com.android.vadify.viewmodels

import android.util.Base64
import android.util.Log
import androidx.lifecycle.LiveData
import com.android.vadify.R
import com.android.vadify.data.repository.UserRepository
import com.android.vadify.data.service.PreferenceService
import com.android.vadify.utils.BaseViewModel
import com.cossacklabs.themis.SecureCell
import com.sdi.joyersmajorplatform.uiview.NetworkState
import org.json.JSONException
import org.json.JSONObject
import javax.inject.Inject

class EncryptionViewModel @Inject constructor(
        private val userRepository: UserRepository,
        private val preferenceService: PreferenceService
) : BaseViewModel() {

    companion object {

        const val launchSecretKey = "qBWSiYdloK"
        const val launchContextKey = "zIxC6YNTUw"

        private var secretKey = launchSecretKey
        private var contextKey = launchContextKey

        fun encryptString(string: String?, context: String = contextKey): String {

            if (string.isNullOrEmpty()) {
                return ""
            }

            val contextImprintKey = SecureCell.ContextImprintWithKey(secretKey.toByteArray(Charsets.UTF_8))

            val encodedByteArray =
                    contextImprintKey!!.encrypt(string.toByteArray(), context.toByteArray(Charsets.UTF_8))
            Log.d("encription_string", "encryptString: "+encodeBase64String(encodedByteArray)?.toString(Charsets.UTF_8)!!.trim().replace("\n ", ""))
            return encodeBase64String(encodedByteArray)?.toString(Charsets.UTF_8)!!.trim().replace("\n ", "");
        }

        fun decryptString(string: String?, context: String = contextKey): String {

            if (string.isNullOrEmpty()) {
                return ""
            }
            val decodedEncryptedString = decodeBase64String(string)
            val contextImprintKey = SecureCell.ContextImprintWithKey(secretKey.toByteArray(Charsets.UTF_8))
            return contextImprintKey!!.decrypt(
                    decodedEncryptedString,
                    context.toByteArray(Charsets.UTF_8)
            ).toString(Charsets.UTF_8)
        }

        fun decodeBase64String(encoded: String): ByteArray? {
            return Base64.decode(encoded, Base64.DEFAULT)
        }

        fun encodeBase64String(encoded: ByteArray): ByteArray? {
            return Base64.encode(encoded, Base64.DEFAULT)
        }
    }

    private fun configure(atLaunch: Boolean = false) {

        if (atLaunch) {
            secretKey = launchSecretKey
            contextKey = launchContextKey
        } else {
            secretKey = preferenceService.getString(R.string.pkey_master_key)!!
            contextKey = preferenceService.getString(R.string.pkey_context_key, launchSecretKey)!!
        }
    }

    fun staticContent(): LiveData<NetworkState> {

        val request = userRepository.getStaticContent().also { it ->
            subscribe(it.request)
            {
                if (it.isSuccessful) {
                    storeKeys(it.body()?.data!!)
                }
            }
        }
        return request.networkState
    }

    private fun storeKeys(jsonData: String) {

        val cell: SecureCell.ContextImprint =
                SecureCell.ContextImprintWithKey(launchSecretKey.toByteArray(Charsets.UTF_8))

        val decrypted =
                cell.decrypt(decodeBase64String(jsonData), launchContextKey.toByteArray(Charsets.UTF_8))

        try {
            val jsonObj = JSONObject(decrypted.toString(Charsets.UTF_8))
            val masterKey = jsonObj.getString("masterKey")
            val contextKey = jsonObj.getString("contextKey")

            preferenceService.putString(R.string.pkey_master_key, masterKey)
            preferenceService.putString(R.string.pkey_context_key, contextKey)

            configure()

        } catch (e: JSONException) {
            e.printStackTrace()
        }
    }
}
