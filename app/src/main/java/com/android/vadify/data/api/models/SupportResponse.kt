package com.android.vadify.data.api.models

data class SupportResponse(
    val `data`: Data,
    val message: String,
    val statusCode: Int
) {
    data class Data(
        val __v: Int,
        val _id: String,
        val activeUsers: List<Any>,
        val blockedUsers: List<Any>,
        val contacts: List<String>,
        val countryCode: String,
        val createdAt: String,
        val deviceAPNToken: Any,
        val deviceToken: String,
        val deviceType: String,
        val email: String,
        val expireTime: String,
        val groupNoficaction: Boolean,
        val groupPermission: String,
        val inAppSound: Boolean,
        val inAppVibrate: Boolean,
        val isDeleted: Boolean,
        val isSupportNumber: Boolean,
        val language: String,
        val languageCode: String,
        val lastActive: String,
        val lastLogin: String,
        val liveLocationShare: Boolean,
        val loginToken: List<LoginToken>,
        val messageNoficaction: Boolean,
        val motherSwitch: Boolean,
        val name: String,
        val number: String,
        val onlineStatus: String,
        val orderNumber: Int,
        val otp: String,
        val phone: String,
        val profileImage: String,
        val profileStatus: String,
        val role: Int,
        val saveToCameraRole: Boolean,
        val securityNotification: Boolean,
        val showPreview: Boolean,
        val status: Int,
        val updatedAt: String
    )

    data class LoginToken(
        val _id: String,
        val createdAt: String,
        val deviceAPNToken: Any,
        val deviceToken: String,
        val deviceType: String,
        val token: String
    )
}