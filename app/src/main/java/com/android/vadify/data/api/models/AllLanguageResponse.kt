package com.android.vadify.data.api.models

data class AllLanguageResponse(
    val data: List<Data>,
    val message: String,
    val statusCode: Int
) {
    data class Data(
        val _id: String,
        val name: String,
        val code: String,
        val languageCode: String,
        val countryCode: String,
        val supportedOS: String,
        val isActive: Boolean,
        val country: String
    )
}