package com.android.vadify.data.api.models

data class AddNewGroupRequest(
    val roomId: String,
    val members: List<String>
)