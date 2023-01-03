package com.android.vadify.ui.dashboard.viewmodel

import androidx.lifecycle.LiveData
import com.android.vadify.R
import com.android.vadify.data.api.enums.GroupPermission
import com.android.vadify.data.db.chat.ChatListCache
import com.android.vadify.data.db.chatThread.ChatThreadCache
import com.android.vadify.data.db.commands.CommandDao
import com.android.vadify.data.db.commands.Commands
import com.android.vadify.data.repository.UserRepository
import com.android.vadify.data.service.PreferenceService
import com.android.vadify.utils.ProfileModel
import com.android.vadify.widgets.restoreList
import com.sdi.joyersmajorplatform.common.livedataext.map
import com.sdi.joyersmajorplatform.common.livedataext.mutableLiveData
import com.sdi.joyersmajorplatform.uiview.NetworkState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

class EditMyAccountViewModel @Inject constructor(
    private val userRepository: UserRepository,
    private val preferenceService: PreferenceService,
    private val cache: ChatListCache,
    private val commandDelete: CommandDao,
    private val userList: ChatThreadCache
) :
    ProfileModel(preferenceService) {

    var numberOfBlockUser = mutableLiveData(0)

    init {
        numberOfBlockUser()
    }


    var shareLiveLocation =
        mutableLiveData(preferenceService.getBoolean(R.string.pkey_liveLocationShare))
    var securityNotification =
        mutableLiveData(preferenceService.getBoolean(R.string.pkey_securityNotification))
    var groupPermission =
        mutableLiveData(preferenceService.getString(R.string.pkey_groupPermission))
    var phoneNumber = mutableLiveData("")


    var groupPermissionFilter = groupPermission.map {
        when (it) {
            GroupPermission.EVERYONE.value -> GroupPermission.EVERYONE.value
            GroupPermission.MY_CONTACTS.value -> GroupPermission.MY_CONTACTS.value
            else -> GroupPermission.NONE.value
        }
    }

    fun updateLiveLocation(isChecked: Boolean) {
        preferenceService.putBoolean(R.string.pkey_liveLocationShare, isChecked)
        needToUpdateDataOnServer(true)
    }

    fun updateSecurityPermission(isChecked: Boolean) {
        preferenceService.putBoolean(R.string.pkey_securityNotification, isChecked)
        needToUpdateDataOnServer(true)
    }

    fun updateEveryOne() {
        preferenceService.putString(R.string.pkey_groupPermission, GroupPermission.EVERYONE.value)
        groupPermission.value = GroupPermission.EVERYONE.value
        needToUpdateDataOnServer(true)
    }

    fun updateMyContact() {
        preferenceService.putString(
            R.string.pkey_groupPermission,
            GroupPermission.MY_CONTACTS.value
        )
        groupPermission.value = GroupPermission.MY_CONTACTS.value
        needToUpdateDataOnServer(true)
    }

    private fun needToUpdateDataOnServer(updateData: Boolean) {
        preferenceService.putBoolean(R.string.pkey_needToUpdateNotificationData, updateData)
    }

    fun deleteUserandChat() {
        CoroutineScope(Dispatchers.IO).launch {

            cache.deleteAll()
            commandDelete.deleteAll()

            userList.deleteAll()
            preferenceService.putBoolean(R.string.pkey_messageNoficaction, false)
            preferenceService.clearPreference()
        }
    }


    fun deleteAllCommands() {
        CoroutineScope(Dispatchers.IO).launch {
            preferenceService.clearPreference()
            commandDelete.deleteAll()
        }
    }

    fun numberOfBlockUser() {
        preferenceService.getString(R.string.pkey_blocked_user)?.let { response ->
            numberOfBlockUser.value = (restoreList<String>(response) ?: arrayListOf()).size ?: 0
        }
    }

    fun deleteuser(): LiveData<NetworkState> {
        return userRepository.deleteUser().also {
            subscribe(it.request) { deleteUserandChat() }
        }.networkState
    }

    fun updateNumber() {
        phoneNumber.value =
            preferenceService.getString(R.string.pkey_countryCode) + " " + preferenceService.getString(
                R.string.pkey_phone
            )
    }
    /* fun logoutMethod(): LiveData<NetworkState> {
         return userRepository.logoutMethod().also {
             subscribe(it.request) { deleteChat() }
         }.networkState
     }*/
}