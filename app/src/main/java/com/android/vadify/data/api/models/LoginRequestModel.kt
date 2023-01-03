package com.android.vadify.data.api.models

data class LoginRequestModel(
    val countryCode: String? = "",
    val deviceToken: String? = "",
    val deviceType: String = "",
    val otp: String? = "",
    val phone: String? = ""
)

