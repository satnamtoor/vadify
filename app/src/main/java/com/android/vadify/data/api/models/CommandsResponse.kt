package com.android.vadify.data.api.models

data class CommandsResponse(
    val data: Data,
    val message: String,
    val statusCode: Int
) {


    data class Data(
        val _id: String,
        val userId: String,
        val commandName: String,
        val command1: Boolean,
        val command2: Boolean,
        val command3: String,
        val createdAt: String,
        val updatedAt: String,
        val __v: String
    )
}