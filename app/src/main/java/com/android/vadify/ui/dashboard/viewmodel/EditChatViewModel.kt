package com.android.vadify.ui.dashboard.viewmodel

import com.android.vadify.R
import com.android.vadify.data.repository.UserRepository
import com.android.vadify.data.service.PreferenceService
import com.android.vadify.utils.BaseViewModel
import com.sdi.joyersmajorplatform.common.livedataext.mutableLiveData
import javax.inject.Inject

class EditChatViewModel @Inject constructor(private val preferenceService: PreferenceService) :
    BaseViewModel() {


    val cameraRollNotification =
        mutableLiveData(preferenceService.getBoolean(R.string.pkey_saveToCameraRole))


    fun upateCameraRollNotification(isChecked: Boolean) {
        preferenceService.putBoolean(R.string.pkey_saveToCameraRole, isChecked)
        needToUpdateDataOnServer(true)
    }

    private fun needToUpdateDataOnServer(updateData: Boolean) {
        preferenceService.putBoolean(R.string.pkey_needToUpdateNotificationData, updateData)

    }


}