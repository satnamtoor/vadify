package com.android.vadify.ui.dashboard.viewmodel

import android.content.Context
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.android.vadify.R
import com.android.vadify.data.api.models.BlockContentResponse
import com.android.vadify.data.repository.UserRepository
import com.android.vadify.data.service.PreferenceService
import com.android.vadify.ui.util.ResourceViewModel
import com.android.vadify.utils.BaseViewModel
import com.android.vadify.utils.CountryCodeSelector
import com.android.vadify.widgets.restoreList
import com.android.vadify.widgets.saveList
import com.google.gson.Gson
import com.sdi.joyersmajorplatform.common.livedataext.map
import com.sdi.joyersmajorplatform.common.livedataext.mutableLiveData
import com.sdi.joyersmajorplatform.uiview.NetworkState
import javax.inject.Inject

class BlockedContentViewModel @Inject constructor(
    private val userRepository: UserRepository,
    private val preferenceService: PreferenceService
) :
    BaseViewModel() {


    var list = arrayListOf<BlockContentResponse.Data>()

    var isBlockFinish = mutableLiveData(false)
    var contactList = MutableLiveData<List<BlockContentResponse.Data>>().also {
        list.add(BlockContentResponse.Data("1", "User One", "+91 123567890"))
        list.add(BlockContentResponse.Data("2", "User Two", "+91 9876543210"))
        list.add(BlockContentResponse.Data("3", "User Three", "+91 1234543210"))
        it.value = list
    }


    var updateBlockList = mutableLiveData("")

    var blockedContentResponse
            = ResourceViewModel(updateBlockList) {
        userRepository.getBlockUser()
    }


    fun getUnblockResponse(blockData: BlockContentResponse.Data): LiveData<NetworkState> {
        val networkRequest = userRepository.getBlockUserChat()
        subscribe(networkRequest.request) {
            if (it.isSuccessful) {
                if (!it.body()?.data.isNullOrEmpty()) {

                    blockContentList.postValue(it.body()?.data)

                    getBlockUnBlockUser(blockData)
                }
            }
        }
        return networkRequest.networkState
    }


    var blockContentList
            = blockedContentResponse.data.map {
        it?.data
    } as MutableLiveData

//    var blockListSize = blockedContentResponse.data.map {
//        it?.data?.size
//    } as MutableLiveDataano


    var blockListSize = blockContentList.map { it?.size } as MutableLiveData

    fun getBlockUnBlockUser(
        blockData: BlockContentResponse.Data,
        number: String = ""
    ): LiveData<NetworkState> {

        var result = ""
        if (number.isNullOrBlank()) {
            result = blockData.number
        } else {
            result = number
        }
       // Log.d("block_data--", Gson().toJson(blockData))

        val request = userRepository.getBlockUnBlockUser(result).also { it ->

            //Log.d("block-content", Gson().toJson(it)+"block-data"+Gson().toJson(blockData))

            subscribe(it.request) {
                if (it.isSuccessful) {
                    //  Log.d("block-content", Gson().toJson(it))
                    blockContentList.value?.let {
                        (it as ArrayList).remove(blockData)
                        blockListSize.postValue(it.size)
                        updatePreferenceList(result)
                    }

                    //[{"_id":"61116e9b6648e9001b7a09b4","name":"sumit","number":"+918826557972"}]
                    //block-data{"_id":"61116e9b6648e9001b7a09b4","name":"sumit","number":"+918826557972"}
                }
            }
        }
        return request.networkState
    }

    private fun updatePreferenceList(unblockId: String) {
        preferenceService.getString(R.string.pkey_blocked_user)?.let { response ->

            Log.d("block_number1", "unbolcked " + response +" --"+preferenceService.getString(R.string.pkey_blocked_user))
            val list = restoreList<String>(response) ?: arrayListOf()
            list.remove(unblockId)
            preferenceService.putString(R.string.pkey_blocked_user, saveList(list))
            isBlockFinish.postValue(true)
        }
    }


//    fun getBlockUser(): LiveData<NetworkState> {
//        val request = userRepository.getBlockUser().also { it ->
//            subscribe(it.request) {
//                it.body()?.let {
//                    Log.e("response",""+it)
//                }
//            }
//        }
//        return request.networkState
//    }

   /* fun isBlockedUserOrNot(mContext: Context,phoneNumber:String): MutableLiveData<Boolean?>{
        *//* var countryCode: Int = preferenceService.getString(R.string.pkey_countryCode)!!.length
         var isBlock :MutableLiveData<Boolean?> = MutableLiveData()
         val result = phoneNumber.drop(countryCode)*//*
        var isBlock :MutableLiveData<Boolean?> = MutableLiveData()
        val result = CountryCodeSelector(mContext).removeCountryCode(phoneNumber)


        preferenceService.getString(R.string.pkey_blocked_user)?.run {
            Log.d("block_number","bolcked "+this)
            val condition = result.isNotBlank() && this.contains(result)
            isBlock.value= condition

        }
        return isBlock
    }
*/

    fun isBlockedUserOrNot( anotherUserId:String): Boolean? {

    //    val result = CountryCodeSelector(mContext).removeCountryCode(anotherUserId)

        return preferenceService.getString(R.string.pkey_blocked_user)?.run {
            val condition = anotherUserId.isNotBlank() && this.contains(anotherUserId)
            condition
        }
    }


}