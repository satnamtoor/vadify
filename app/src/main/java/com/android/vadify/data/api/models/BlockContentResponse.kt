package com.android.vadify.data.api.models

data class BlockContentResponse(
    val data: List<Data>,
    val message: String,
    val statusCode: Int
) {
    data class Data(
        val _id: String,
        val name: String,
        val number: String
    )
}