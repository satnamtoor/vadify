package com.android.vadify.ui.chat.contact.viewmodel

import android.content.Context
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.android.vadify.R
import com.android.vadify.data.api.models.BlockContentResponse
import com.android.vadify.data.api.models.ChangeNumberRequestModel
import com.android.vadify.data.api.models.GroupContactDetailResponse
import com.android.vadify.data.db.chat.ChatListCache
import com.android.vadify.data.db.chatThread.ChatThreadCache
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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject


class UserContactViewModel @Inject constructor(
    val preferenceService: PreferenceService,
    val userRepository: UserRepository,
    val cache: ChatListCache


) : BaseViewModel() {
    //var memberIds :List<String> = emptyList()
    var userId = MutableLiveData<String>()
    var profileImageUrl : LiveData<String?> = MutableLiveData<String>()
   // var membersName = mutableLiveData("Tap here for more info")
    var defaultDrawable = R.drawable.user_placeholder


    var userResource = ResourceViewModel(userId) {

        userRepository.getContactInfoMethod(it)
    }

    var groupResource = ResourceViewModel(userId) {
        userRepository.getGroupInfo(it)
    }

    fun getBlockUnBlockUser(userId: String): LiveData<NetworkState> {
        val request = userRepository.getBlockUnBlockUser(userId).also { it ->
            subscribe(it.request)
        }
        return request.networkState
    }


    fun clearChat(roomId: String): LiveData<NetworkState> {
        val request = userRepository.clearChat(roomId).also { it ->
            subscribe(it.request) {
                if (it.isSuccessful) {
                    CoroutineScope(Dispatchers.IO).launch {
                        cache.deleteSpecificChat(roomId)
                    }
                }
            }
        }
        return request.networkState
    }

    /*fun getMemberName(roomId: String) {
        Log.d( "getMemberName: ",roomId)

        val members = chatThreadCache.getChatThread(roomId).members
        memberIds = members.map { it.userId }
        val users = members.map {
            it.name
        }
        var namesOfMem = ""
        for (user in users) {
            namesOfMem += "$user,"
        }
        membersName.value = namesOfMem
    }*/

    var userName = userResource.data.map {
        it?.data?.contactDetail?.name
    }

    var userNameGroup = groupResource.data.map {
        it?.data?.data?.name
    }

    var number = userResource.data.map {

        it?.data?.contactDetail?.number
    }
    var createdDate = groupResource.data.map {
        it?.data?.data?.createdAt
    }

    var language = userResource.data.map {
        it?.data?.contactDetail?.language
    }


    var profileStatus = userResource.data.map {
        it?.data?.contactDetail?.profileStatus
    }

    var profileImageSingle : LiveData<String?> = userResource.data.map {
        it?.data?.contactDetail?.profileImage
    }

    var profileImageGroup : LiveData<String?> = groupResource.data.map {
        it?.data?.data?.profileImage
    }

    var profileImage = if (profileImageSingle.value.isNullOrEmpty()) profileImageSingle else profileImageGroup


    var lenghtOfMedialLink = userResource.data.map {
        it?.data?.mediaLinksCount.toString()
    }

    var lenghtOfMedialLinkGroup = groupResource.data.map {
        it?.data?.mediaLinkCount.toString()
    }
    var groupMemberLength = groupResource.data.map {
        it?.data?.data?.members
    }


    fun profileImage(groupType : String){
        profileImageUrl = if (groupType == "Single") profileImageSingle else profileImageGroup
    }

    var groupMember = groupResource.data.map {
        it?.data?.data?.members
    } as MutableLiveData<List<GroupContactDetailResponse.Member>>

    /*var blockContentList
            = blockedContentResponse.data.map {
        it?.data
    } as MutableLiveData*/

    fun updatePreferenceList(userId: String) {
        Log.d("country_code", "" + preferenceService.getString(R.string.pkey_countryCode))
        //  var countryCode: Int = preferenceService.getString(R.string.pkey_countryCode)!!.length
        // val result = userId.drop(countryCode)

        // val result = CountryCodeSelector(mContext).removeCountryCode(userId)

        preferenceService.getString(R.string.pkey_blocked_user)?.let { respose ->
            val list = restoreList<String>(respose) ?: arrayListOf()
            list.add(userId)
            val removeDuplicateItem = list.distinct()
            preferenceService.putString(R.string.pkey_blocked_user, saveList(removeDuplicateItem))
        }
    }


    fun updateData(anotherUserId: String) {
        Log.d( "updateData--userid: ",anotherUserId)
        userId.value = anotherUserId
    }

    fun exitRoomRequest(roomid: String): LiveData<NetworkState> {
        val request = userRepository.exitGroup(roomid).also { it ->
            subscribe(it.request)
        }
        return request.networkState
    }


}