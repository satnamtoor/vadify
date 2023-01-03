package com.android.vadify.ui.chat.call.viewmodel

import android.annotation.SuppressLint
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.android.vadify.R
import com.android.vadify.data.api.enums.CallStatus
import com.android.vadify.data.api.enums.CallType
import com.android.vadify.data.api.models.CallStatusRequest
import com.android.vadify.data.api.models.GroupCallRequest
import com.android.vadify.data.api.models.SingleCallRequest
import com.android.vadify.data.repository.UserRepository
import com.android.vadify.data.service.PreferenceService
import com.android.vadify.utils.BaseViewModel
import com.google.gson.Gson
//import com.github.nkzawa.socketio.client.Socket
import com.sdi.joyersmajorplatform.common.livedataext.mutableLiveData
import com.sdi.joyersmajorplatform.uiview.NetworkState
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.rxkotlin.subscribeBy
import io.reactivex.schedulers.Schedulers
import io.socket.client.Socket
import javax.inject.Inject


class CallViewModel @Inject constructor(
    val preferenceService: PreferenceService,
    val userRepository: UserRepository,
    val mSocket: Socket?
) : BaseViewModel() {


    val senderImage = MutableLiveData<String>()
    val receiverImage = MutableLiveData<String>()
    val callStatus = mutableLiveData(CallStatus.CONNECTING)
    val usersName = mutableLiveData("")
    val senderName = mutableLiveData("")
    val receiverName = mutableLiveData("")
    val callRequestResponse = MutableLiveData<CallRequestResponse>()
    val receiverRequestResponse = MutableLiveData<CallRequestResponse>()
    val muteUnmuteDrawable = mutableLiveData(R.drawable.ic_mic_white_24dp)
    val volumeOnOff = mutableLiveData(R.drawable.ic_baseline_volume_mute_24)
    var isSpeekerMute = true
    var needToCallMethod = false

    var callNetworkStatus = MutableLiveData<NetworkState>()

    val isCallConnected = mutableLiveData(CallType.SENDER)


    var callSenderReceiver = CallType.SENDER.value


    fun isSoundEnable() = preferenceService.getBoolean(R.string.pkey_inAppSound)
    val isViberationEnable = preferenceService.getBoolean(R.string.pkey_inAppVibrate)

    fun getSocketInstance(): Socket? {
        return mSocket
    }


    fun callSingleRequestApi(mode: String, anotherUserId: String): LiveData<NetworkState> {
        val networkRequest =
            userRepository.singleCallRequest(SingleCallRequest(mode, anotherUserId))
        subscribe(networkRequest.request) {
            if (it.isSuccessful)
                callRequestResponse.postValue(
                    CallRequestResponse(
                        it.body()?.data?.token,
                        it.body()?.data?.call?._id
                    )
                )
        }
        return networkRequest.networkState
    }

    fun groupCallRequestApi(userList : ArrayList<String> , mode: String, anotherUserId: String): LiveData<NetworkState> {
        val networkRequest =
            userRepository.groupCallRequest(GroupCallRequest(userList,mode, anotherUserId))
        subscribe(networkRequest.request) {
            if (it.isSuccessful)
                callRequestResponse.postValue(
                    CallRequestResponse(
                        it.body()?.data?.token,
                        it.body()?.data?.call?._id
                    )
                )
        }
        return networkRequest.networkState
    }

    fun callTokenRequest(roomName: String): LiveData<NetworkState> {
        val networkRequest = userRepository.callToken()
        subscribe(networkRequest.request) {
            if (it.isSuccessful) receiverRequestResponse.postValue(
                CallRequestResponse(it.body()?.data as String, roomName)
            )
        }
        return networkRequest.networkState
    }


    fun updateCallStatus(status: CallStatus) {
        callStatus.value = status
    }

    fun updateReceiverInformation(senderUserName: String, senderPath: String) {
      //  Log.d("updateReceiverInformation: ", ""+CallType.RECEIVER.value)
        //Log.d("updateReceiverInformation: ", "$senderUserName ${preferenceService.getString(R.string.pkey_userName)}")
        usersName.value =
            "$senderUserName and ${preferenceService.getString(R.string.pkey_userName)}"
        senderImage.value = senderPath
        receiverImage.value = preferenceService.getString(R.string.pkey_profileImage)
        receiverName.value = preferenceService.getString(R.string.pkey_userName)
        senderName.value = senderUserName
        preferenceService.putString(R.string.pkey_call_user_name, senderUserName)
        preferenceService.putString(R.string.pkey_call_user_image, senderPath)
        preferenceService.putInt(R.string.pkey_call_user, CallType.RECEIVER.value)

    }

    fun updateSenderInformation(receiver: String, receiverPath: String) {
        //Log.d("updateReceiverInformation: ", ""+CallType.SENDER.value)
        //Log.d("updateReceiverInformation: ", "${preferenceService.getString(R.string.pkey_userName)} and $receiver")
        usersName.value = "${preferenceService.getString(R.string.pkey_userName)} and $receiver"
        senderImage.value = preferenceService.getString(R.string.pkey_profileImage)
        receiverImage.value = receiverPath
        senderName.value = preferenceService.getString(R.string.pkey_userName)
        receiverName.value = receiver
        preferenceService.putString(R.string.pkey_call_user_name, receiver)
        preferenceService.putString(R.string.pkey_call_user_image, receiverPath)
        preferenceService.putInt(R.string.pkey_call_user, CallType.SENDER.value)

    }


    fun updateProfileData() {
       // Log.d("updateProfileData: ","Yes-coming")
        when (preferenceService.getInt(R.string.pkey_call_user, 6)) {

            CallType.SENDER.value ->{
                Log.d("updateProfileData: ","Yes-sender-coming")
                updateSenderInformation(
                preferenceService.getString(R.string.pkey_call_user_name) ?: "",
                preferenceService.getString(R.string.pkey_call_user_image) ?: ""
            )
            }
            CallType.RECEIVER.value -> {
                Log.d("updateProfileData: ", "Yes-coming")
                updateReceiverInformation(
                    preferenceService.getString(R.string.pkey_call_user_name) ?: "",
                    preferenceService.getString(R.string.pkey_call_user_image) ?: ""
                )
            }
            else ->{
                updateReceiverInformation(
                    preferenceService.getString(R.string.pkey_receiver_name_call) ?: "",
                    preferenceService.getString(R.string.pkey_call_user_image) ?: ""
                )
            }
        }

    }


    fun updateCall(status: CallType) {
        isCallConnected.value = status
    }


    fun updateDrawable(enable: Boolean) {
        muteUnmuteDrawable.value =
            if (enable) R.drawable.ic_mic_white_24dp else R.drawable.ic_mic_off_black_24dp
    }

    fun updateVolumeDrawable(enable: Boolean) {
        volumeOnOff.value =
            if (enable) R.drawable.ic_baseline_volume_up_24 else R.drawable.ic_baseline_volume_mute_24
    }


    @SuppressLint("CheckResult")
    fun receivingAudioCallMethod(_id: String, status: String) {
        callNetworkStatus.value = NetworkState.loading
        userRepository.callTokenUpdateSingle(_id, CallStatusRequest(status))
            .subscribeOn(Schedulers.io()).observeOn(
                AndroidSchedulers.mainThread()
            ).subscribeBy(
                onSuccess = {
                    callNetworkStatus.value = NetworkState.success
                }, onError = {
                    callNetworkStatus.value = NetworkState.success
                })
    }


    data class CallRequestResponse(var token: String?, var roomName: String?)
}