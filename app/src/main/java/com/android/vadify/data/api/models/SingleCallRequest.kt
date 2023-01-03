package com.android.vadify.data.api.models

data class SingleCallRequest(
    val mode: String,
    val to: String
)

data class GroupCallRequest(
    val users:ArrayList<String>,
    val mode: String,
    val roomId: String
)