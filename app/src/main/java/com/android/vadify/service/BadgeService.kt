package com.android.vadify.service

import androidx.lifecycle.MutableLiveData
import com.android.vadify.R
import com.android.vadify.data.repository.UserRepository
import com.android.vadify.data.service.PreferenceService
import com.android.vadify.ui.util.ResourceViewModel
import com.sdi.joyersmajorplatform.common.livedataext.mutableLiveData
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Service for user data
 */
@Suppress("ForbiddenVoid")
@Singleton
class BadgeService @Inject constructor(
    private val userRepository: UserRepository,
    private val preferenceService: PreferenceService
) {

    var updateList = MutableLiveData<String>()
    var localMessageUpdate = mutableLiveData(0)

    /**
     * Call this when a notification has been received.
     */
    fun refresh() {
       // if (preferenceService.getInt(R.string.pkey_badge_count) )
        val badgeCount = preferenceService.getInt(R.string.pkey_badge_count) + 1
        preferenceService.putInt(R.string.pkey_badge_count, badgeCount)
        localMessageUpdate.postValue(badgeCount)
    }

    var userListResource = ResourceViewModel(updateList) {
        userRepository.getListOfUser(preferenceService.getString(R.string.pkey_user_Id)!!, preferenceService.getString(R.string.pkey_blocked_user))
    }

    fun updateBadgeFromServer() {
        updateList.postValue("")
    }

}
