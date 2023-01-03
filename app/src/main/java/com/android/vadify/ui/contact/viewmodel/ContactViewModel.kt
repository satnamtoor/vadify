package com.android.vadify.ui.contact.viewmodel

import android.text.TextUtils
import androidx.lifecycle.MutableLiveData
import com.android.vadify.R
import com.android.vadify.data.api.models.ContactSyncingResponse
import com.android.vadify.data.db.contact.Contact
import com.android.vadify.data.db.contact.ContactListCache
import com.android.vadify.data.repository.UserRepository
import com.android.vadify.data.service.PreferenceService
import com.android.vadify.ui.dashboard.Dashboard
import com.android.vadify.ui.util.ResourceViewModel
import com.android.vadify.utils.BaseViewModel
import com.android.vadify.widgets.sticklist.CallPinyinComparator
import com.android.vadify.widgets.sticklist.CharacterParser
import com.android.vadify.widgets.sticklist.PinyinComparator
import com.sdi.joyersmajorplatform.common.livedataext.map
import com.sdi.joyersmajorplatform.common.livedataext.mutableLiveData
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.*
import javax.inject.Inject

class ContactViewModel @Inject constructor(
    val contactListCache: ContactListCache,
    userRepository: UserRepository, private val preferenceService: PreferenceService
) : BaseViewModel() {

    private var characterParser = CharacterParser.getInstance()
    var pinyinComparator = PinyinComparator()
    var callPinyinComparator = CallPinyinComparator()

    var contactList = contactListCache.getAll()
    var updateView = mutableLiveData(true)


    var listOfContact = contactListCache.getAll().map {
        val list = it?.map {
            val pinin = characterParser?.getSelling(it.name)
            val sortString = pinin?.substring(0, 1)?.toUpperCase(java.util.Locale.ROOT)
            it.sortLetter = sortString
            it
        }
        Collections.sort(list, pinyinComparator)
        list
    }


    fun filterData(searchCharacter: String): ArrayList<Contact> {
        var filterDataList = arrayListOf<Contact>()
        if (TextUtils.isEmpty(searchCharacter)) {
            listOfContact.value?.let {
                filterDataList = it as ArrayList<Contact>
            }
        } else {
            filterDataList.clear()
            listOfContact.value?.map {
                val name = it.name
                if (name.indexOf(searchCharacter) != -1 || characterParser?.getSelling(name)
                        ?.contains(searchCharacter, ignoreCase = true) == true
                ) {
                    filterDataList.add(it)
                }
            }
        }
        Collections.sort(filterDataList, pinyinComparator)
        return filterDataList
    }


    private val contactListRequestModel = MutableLiveData<List<Dashboard.ContactInformation>>()

    suspend fun updateContactListMethod(contacts: ArrayList<Dashboard.ContactInformation>) {
        withContext(Dispatchers.Main) {
            contactListRequestModel.value = contacts
        }
    }

    private var contactListUpdateNetwork = ResourceViewModel(contactListRequestModel) {
        userRepository.updateContactList(it)
    }

    var registerContactList = contactListUpdateNetwork.data.map {


        val list = it?.data?.users?.run {
            this.map {

                if (it.name.isNullOrEmpty())

                    it.name = it.name.capitalize()
                it

            }
        }
        CoroutineScope(Dispatchers.IO).launch {
            Collections.sort(list, callPinyinComparator)
        }
list


}


fun filterDataWithCall(
    searchCharacter: String, contactList: ArrayList<ContactSyncingResponse.Data.User>
): ArrayList<ContactSyncingResponse.Data.User> {
    var filterDataList = arrayListOf<ContactSyncingResponse.Data.User>()
    if (TextUtils.isEmpty(searchCharacter)) {
        registerContactList.value?.let {
            filterDataList = it as ArrayList<ContactSyncingResponse.Data.User>
        }
    } else {
        filterDataList.clear()
        contactList.map {
            val name = it.name
            if (name.indexOf(searchCharacter) != -1 || characterParser?.getSelling(name)
                    ?.contains(searchCharacter, ignoreCase = true) == true
            ) {
                filterDataList.add(it)
            }
        }
    }
    Collections.sort(filterDataList, callPinyinComparator)
    return filterDataList
}

fun updatePhoneNumber(number: String): String {
    return preferenceService.getString(R.string.pkey_countryCode) + number


}

fun updateView(condition: Boolean) {
    updateView.value = condition
}


}