package com.android.vadify.data.api.models

import com.google.gson.annotations.SerializedName

data class ImageUploadResponse(
    val data: Data,
    val message: String,
    val statusCode: Int
) {
    data class Data(
        val url: String
    )
}