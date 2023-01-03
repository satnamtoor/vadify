package com.android.vadify.data.api.models

data class UserLanguageResponseModel(
    val data: Data,
    val message: String,
    val statusCode: Int
) {
    data class Data(
        val __v: Int,
        val _id: String,
        val countryCode: String,
        val createdAt: String,
        val language: String,
        val languageCode: String,
        val roomId: String,
        val updatedAt: String,
        val userId: String
    )
}