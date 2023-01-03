package com.android.vadify.ui.dashboard.viewmodel

import androidx.lifecycle.MutableLiveData
import com.android.vadify.R
import com.android.vadify.data.db.callLogs.CallListCache
import com.android.vadify.data.repository.ChatRepository
import com.android.vadify.data.service.PreferenceService
import com.android.vadify.ui.dashboard.fragment.call.CallFragment
import com.android.vadify.ui.util.PagedListViewModel
import com.android.vadify.utils.BaseViewModel
import com.sdi.joyersmajorplatform.common.livedataext.mutableLiveData
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

class CallLogViewModel @Inject constructor(
    chatRepository: ChatRepository,
    private val cache: CallListCache,
    private val preferenceService: PreferenceService
) :
    BaseViewModel() {

    var searchQuery = mutableLiveData("")

    var firstTimeVisit = true

    init {
        CoroutineScope(Dispatchers.IO).launch { cache.deleteAll() }
    }

    var filterKey = MutableLiveData<String>()

    var allBtnTextColor = mutableLiveData(true)
    var missedBtnTextColor = mutableLiveData(false)


    var getCallList = PagedListViewModel(filterKey) {
        chatRepository.getCallLog(
            CallFragment.PAGE_COUNT, CallFragment.TOTAL_ITEMS,
            preferenceService.getString(R.string.pkey_user_Id), it, searchQuery.value
        )
    }

    fun updateResult(result: String) {
        CoroutineScope(Dispatchers.IO).launch { cache.deleteAll() }
        filterKey.value = result
    }


    fun searchQuery(result: String) {
        filterKey.value = result
    }


    fun updateTextColor(allTextColor: Boolean, missedTextColor: Boolean) {
        allBtnTextColor.value = allTextColor
        missedBtnTextColor.value = missedTextColor
    }


}