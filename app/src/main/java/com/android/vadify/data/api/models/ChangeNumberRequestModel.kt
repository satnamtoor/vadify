package com.android.vadify.data.api.models

data class ChangeNumberRequestModel(
    val existingCountryCode: String?,
    val existingPhone: String?,
    val newCountryCode: String?,
    val newPhone: String?
)


