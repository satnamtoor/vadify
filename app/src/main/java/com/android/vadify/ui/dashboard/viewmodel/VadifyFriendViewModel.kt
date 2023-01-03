package com.android.vadify.ui.dashboard.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.android.vadify.R
import com.android.vadify.data.api.models.ContactSyncingResponse
import com.android.vadify.data.db.chatThread.ChatThread
import com.android.vadify.data.db.chatThread.ChatThreadCache
import com.android.vadify.data.repository.UserRepository
import com.android.vadify.data.service.PreferenceService
import com.android.vadify.ui.dashboard.Dashboard
import com.android.vadify.ui.util.ResourceViewModel
import com.android.vadify.utils.BaseViewModel
import com.sdi.joyersmajorplatform.common.livedataext.map
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject

class VadifyFriendViewModel @Inject constructor(
    private val userRepository: UserRepository,
    private val chatThreadCache: ChatThreadCache,
     val preferenceService: PreferenceService
) :
    BaseViewModel() {

    private val contactListRequestModel = MutableLiveData<List<Dashboard.ContactInformation>>()


    suspend fun updateContactListMethod(contacts: ArrayList<Dashboard.ContactInformation>) {
        withContext(Dispatchers.Main) {
            contactListRequestModel.value = contacts
        }
    }

    var contactListUpdateNetwork = ResourceViewModel(contactListRequestModel) {
        userRepository.updateContactList(it)
    }

    var registerContactList: LiveData<List<ContactSyncingResponse.Data.User>?> = contactListUpdateNetwork.data.map { it?.data?.users }

    var contactListNetworkState = contactListUpdateNetwork.networkState.map { it }

    var contactSize = contactListUpdateNetwork.data.map { it?.data?.users?.size }


    fun isBlockedUserOrNot(anotherUserId: String): Boolean? {
        return preferenceService.getString(R.string.pkey_blocked_user)?.run {
            val condition = anotherUserId.isNotBlank() && this.contains(anotherUserId)
            condition
        }
    }

    fun updatePhoneNumber(number: String): String {
        return preferenceService.getString(R.string.pkey_countryCode) + number
    }

    fun getChatThreads() :LiveData<List<ChatThread>?> {
        return  chatThreadCache.getAll()
    }

}