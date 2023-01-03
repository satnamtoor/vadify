package com.android.vadify.data.api.models

class LocalLanguageModel : ArrayList<LocalLanguageModel.LocalLanguageModelItem>() {

    data class LocalLanguageModelItem(
        val code: String,
        val countryCode: String,
        val language: String,
        val language_code: String
    )
}