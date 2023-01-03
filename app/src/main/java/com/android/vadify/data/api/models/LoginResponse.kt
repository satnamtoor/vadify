package com.android.vadify.data.api.models

data class LoginResponse(
    val data: Data,
    val message: String,
    val statusCode: Int
) {


    data class Data(
        val _id: String,
        val blockedUsers: List<String>,
        val countryCode: String,
        val email: String,
        val groupNoficaction: Boolean,
        val groupPermission: String,
        val inAppSound: Boolean,
        val inAppVibrate: Boolean,
        val language: String,
        val lastLogin: String,
        val liveLocationShare: Boolean,
        val messageNoficaction: Boolean,
        val name: String,
        val number: String,
        val phone: String,
        val profileImage: String,
        val profileStatus: String,
        val saveToCameraRole: Boolean,
        val securityNotification: Boolean,
        val showPreview: Boolean,
        val token: String
    )
}