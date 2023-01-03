package com.android.vadify.data.api.models

data class UserCommandResponse(
    val data: List<Data>,
    val message: String,
    val statusCode: Int
) {
    data class Data(
        val _id: String,
        val language: String,
        val userId: String,
        val commandName: String,
        val command1: String,
        val command2: String,
        val command3: String,
        val createdAt: String,
        val updatedAt: String,
        val __v: String
    )
}