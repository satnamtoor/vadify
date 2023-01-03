package com.android.vadify.data.api.models

data class CreateGroupRequest(
    val name: String,
    val profileImage: String?,
    val members: List<String>,
    val type: String,
    val roomId: String? = null

)