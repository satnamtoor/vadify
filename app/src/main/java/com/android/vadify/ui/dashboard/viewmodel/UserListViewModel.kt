package com.android.vadify.ui.dashboard.viewmodel

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Transformations
import com.android.vadify.R
import com.android.vadify.data.api.models.MuteUnMuteRequest
import com.android.vadify.data.db.chat.ChatListCache
import com.android.vadify.data.db.chatThread.ChatThread
import com.android.vadify.data.db.chatThread.ChatThreadCache
import com.android.vadify.data.repository.UserRepository
import com.android.vadify.data.service.PreferenceService
import com.android.vadify.ui.util.ResourceViewModel
import com.android.vadify.utils.BaseViewModel
import com.android.vadify.widgets.restoreList
import com.android.vadify.widgets.saveList
import com.sdi.joyersmajorplatform.common.livedataext.map
import com.sdi.joyersmajorplatform.uiview.NetworkState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

class UserListViewModel @Inject constructor(
    private val userRepository: UserRepository,
    private val chatListCache: ChatListCache,
    private val chatThreadCache: ChatThreadCache,
    private val preferenceService: PreferenceService

) :
    BaseViewModel() {

    var updateList = MutableLiveData<String>()

    var filterText = MutableLiveData<String>()

    var userListResource = ResourceViewModel(updateList) {
        userRepository.getListOfUser(preferenceService.getString(R.string.pkey_user_Id)!!, preferenceService.getString(R.string.pkey_blocked_user))
    }

    var listOfMembers = userListResource.data.map { it?.data }

    var forwardedMessageChatThread = MutableLiveData<ChatThread>()

    val filteredChatThreads: LiveData<List<ChatThread>?>
        get() = Transformations.switchMap(filterText) { text ->
            val allThreads = chatThreadCache.getAll()
            val threads = when {
                text == null -> allThreads
                else -> {
                    Transformations.switchMap(allThreads) { threadsList ->
                        val filteredThreads = MutableLiveData<List<ChatThread>>()
                        try {
                            val filteredList = threadsList?.filter { thread -> thread.members[0]?.name?.contains(text, ignoreCase = true) || thread.name?.contains(text, ignoreCase = true) == true }
                            filteredThreads.value = filteredList
                        }
                        catch (ex:Exception)
                        {
                            ex.printStackTrace()
                        }

                        filteredThreads
                    }
                }
            }
            threads
        }

    fun getChatThreads(): LiveData<List<ChatThread>?> {
        return chatThreadCache.getAll()
    }

    fun getUserId(): String {
        return preferenceService.getString(R.string.pkey_user_Id)!!
    }

    fun getChatThreadsFromAPI() {
        userRepository.getListOfUser(preferenceService.getString(R.string.pkey_user_Id)!!, preferenceService.getString(R.string.pkey_blocked_user))
    }

    fun getChatThreadsFromAPINew() {
        userRepository.getListOfUserNew(preferenceService.getString(R.string.pkey_user_Id)!!, preferenceService.getString(R.string.pkey_blocked_user))
    }

    fun getBlockUnBlockUser(userId: String): LiveData<NetworkState> {
        //var countryCode: Int = preferenceService.getString(R.string.pkey_countryCode)!!.length
      //  val result = userId.drop(countryCode)
       // Log.d("phone-number--", "$result countyCode $countryCode")

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
                        chatListCache.deleteSpecificChat(roomId)
                    }
                }
            }
        }
        return request.networkState
    }

    fun searchNameChanged(name: String){
        filterText.value = name
    }


    fun deleteChat(roomId: String): LiveData<NetworkState> {
        val request = userRepository.deleteChat(roomId).also { it ->
            subscribe(it.request) {
                if (it.isSuccessful) {
                    CoroutineScope(Dispatchers.IO).launch {
                        chatThreadCache.deleteChatThread(roomId)
                        chatListCache.deleteSpecificChat(roomId)
                    }
                }
            }
        }
        return request.networkState
    }

    fun muteUnmuteMethod(roomId: String, status: Boolean): LiveData<NetworkState> {
        val request = userRepository.muteUnmute(roomId, MuteUnMuteRequest(status)).also {
            subscribe(it.request)
        }
        return request.networkState
    }


    fun updateApi() {
        updateList.value = getRandomString(4)
    }


    fun updatePreferenceList(userId: String) {
        //var countryCode: Int = preferenceService.getString(R.string.pkey_countryCode)!!.length
       // val result = userId.drop(countryCode)
        //Log.d("phone-number--", "$result countyCode $countryCode")

        preferenceService.getString(R.string.pkey_blocked_user)?.let { respose ->
            val list = restoreList<String>(respose) ?: arrayListOf()
            list.add(userId)
            val removeDuplicateItem = list.distinct()
            preferenceService.putString(R.string.pkey_blocked_user, saveList(removeDuplicateItem))
        }
    }

    fun getRandomString(length: Int) : String {
        val allowedChars = ('A'..'Z') + ('a'..'z') + ('0'..'9')
        return (1..length)
                .map { allowedChars.random() }
                .joinToString("")
    }

    fun getChatThread(forUserId: String) {
        CoroutineScope(Dispatchers.IO).launch {
            val chatThreads = chatThreadCache.getChatThread(forUserId)
            CoroutineScope(Dispatchers.Main).launch {
                forwardedMessageChatThread.value = chatThreads
            }
        }
    }
}