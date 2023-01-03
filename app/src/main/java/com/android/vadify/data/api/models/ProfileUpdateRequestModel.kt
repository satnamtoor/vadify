package com.android.vadify.data.api.models

data class ProfileUpdateRequestModel(
    var email: String? = "",
    var groupNoficaction: Boolean? = false,
    var groupPermission: String? = "",
    var inAppSound: Boolean? = false,
    var inAppVibrate: Boolean? = false,
    var language: String? = "",
    var liveLocationShare: Boolean? = false,
    var messageNoficaction: Boolean? = false,
    var name: String? = "",
    var profileImage: String? = "",
    var profileStatus: String? = "",
    var saveToCameraRole: Boolean? = false,
    var securityNotification: Boolean? = false,
    var showPreview: Boolean? = false,
    var languageCode: String?=""
)