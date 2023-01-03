package com.android.vadify.data.api.models

data class GoogleRequestModel(
    val audioConfig: AudioConfig,
    val input: Input,
    val voice: Voice
) {
    data class AudioConfig(
        val audioEncoding: String,
        val pitch: String,
        val speakingRate: String
    )

    data class Input(
        val text: String?
    )

    data class Voice(
        val languageCode: String,
        val name: String
    )
}