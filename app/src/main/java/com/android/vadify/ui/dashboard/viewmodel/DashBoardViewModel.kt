package com.android.vadify.ui.dashboard.viewmodel

import com.android.vadify.R
import com.android.vadify.data.db.chat.ChatListCache
import com.android.vadify.data.service.PreferenceService
import com.android.vadify.service.BadgeService
import com.android.vadify.utils.BaseViewModel
import com.sdi.joyersmajorplatform.common.livedataext.map
import javax.inject.Inject

class DashBoardViewModel @Inject constructor(
    private val preferenceService: PreferenceService,
    private val badgeService: BadgeService,
    private val cache: ChatListCache

) : BaseViewModel() {


    var totalMembers = badgeService.userListResource.data.map { it ->
        var number = 0
        it?.data?.map { if (it.members.isNotEmpty()) number += it.unreadCount }
        preferenceService.putInt(R.string.pkey_badge_count, number)
        number
    }

    var imagePath = ""

    var localCountUpdate = badgeService.localMessageUpdate.map { it }

    fun clearImagePath() {
        imagePath = ""
    }

    fun updateBadgeService() = badgeService.updateBadgeFromServer()

    fun serviceRefersh() = badgeService.refresh()

    fun getInAppSound() = preferenceService.getBoolean(R.string.pkey_inAppSound)

    fun getInAppViberate() = preferenceService.getBoolean(R.string.pkey_inAppVibrate)


}