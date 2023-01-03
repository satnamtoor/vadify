package com.android.vadify.data.api.models

data class UploadAWSResponse(
    val `data`: Data2,
    val message: String,
    val statusCode: Int
)

data class Data2(
    val filePath: String,
    val uploadUrl: String
)