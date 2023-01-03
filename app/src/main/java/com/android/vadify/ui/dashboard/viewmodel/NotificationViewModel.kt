package com.android.vadify.ui.dashboard.viewmodel

import android.util.Log
import com.android.vadify.R
import com.android.vadify.data.service.PreferenceService
import com.android.vadify.utils.BaseViewModel
import com.sdi.joyersmajorplatform.common.livedataext.mutableLiveData
import javax.inject.Inject

class NotificationViewModel @Inject constructor(private val preferenceService: PreferenceService) :
    BaseViewModel() {

    val messageNotification =
        mutableLiveData(preferenceService.getBoolean(R.string.pkey_messageNoficaction))
    var groupNotification =
        mutableLiveData(preferenceService.getBoolean(R.string.pkey_groupNotification))
    var soundSwitch = mutableLiveData(preferenceService.getBoolean(R.string.pkey_inAppSound))
    var viberateSwitch = mutableLiveData(preferenceService.getBoolean(R.string.pkey_inAppVibrate))
    var perviewMessageSwitch =
        mutableLiveData(preferenceService.getBoolean(R.string.pkey_showPreview))

    fun upateMessageNotification(isChecked: Boolean) {
        preferenceService.putBoolean(R.string.pkey_messageNoficaction, isChecked)
        needToUpdateDataOnServer()
    }

    fun upateGroupNotification(isChecked: Boolean) {
        preferenceService.putBoolean(R.string.pkey_groupNotification, isChecked)
        needToUpdateDataOnServer()
    }

    fun upateSoundSwitch(isChecked: Boolean) {
        preferenceService.putBoolean(R.string.pkey_inAppSound, isChecked)
        needToUpdateDataOnServer()
    }

    fun upateViberateSwitch(isChecked: Boolean) {
        preferenceService.putBoolean(R.string.pkey_inAppVibrate, isChecked)
        needToUpdateDataOnServer()
    }

    fun upatePerviewMessageSwitch(isChecked: Boolean) {
        preferenceService.putBoolean(R.string.pkey_showPreview, isChecked)
        needToUpdateDataOnServer()
    }

    private fun needToUpdateDataOnServer(updateData: Boolean = true) {
       // Log.d( "update_over",""+updateData)
        preferenceService.putBoolean(R.string.pkey_needToUpdateNotificationData, updateData)
    }


}