package com.android.vadify.utils

import com.android.vadify.R
import com.android.vadify.data.api.models.ProfileUpdateRequestModel
import com.android.vadify.data.service.PreferenceService

abstract class ProfileModel(
    private val preferenceService: PreferenceService
) : BaseViewModel() {

    fun getProfileRequestModel(): ProfileUpdateRequestModel {
        return ProfileUpdateRequestModel(
            preferenceService.getString(R.string.pkey_emailId),
            preferenceService.getBoolean(R.string.pkey_groupNotification),
            preferenceService.getString(R.string.pkey_groupPermission),
            preferenceService.getBoolean(R.string.pkey_inAppSound),
            preferenceService.getBoolean(R.string.pkey_inAppVibrate),
            preferenceService.getString(R.string.pkey_motherLanguage),
            preferenceService.getBoolean(R.string.pkey_liveLocationShare),
            preferenceService.getBoolean(R.string.pkey_messageNoficaction),
            preferenceService.getString(R.string.pkey_userName),
            preferenceService.getString(R.string.pkey_profileImage),
            preferenceService.getString(R.string.pkey_status),
            preferenceService.getBoolean(R.string.pkey_saveToCameraRole),
            preferenceService.getBoolean(R.string.pkey_securityNotification),
            preferenceService.getBoolean(R.string.pkey_showPreview),
            preferenceService.getString(R.string.pkey_motherLanguage_Code)
        )
    }

}