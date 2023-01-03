package com.android.vadify.data.api.models

data class ExitGroupResponse(
    val `data`: Data,
    val message: String,
    val statusCode: Int
)
{
data class Data(
    val __v: Int,
    val _id: String,
    val createdAt: String,
    val createdBy: String,
    val lastMessageDate: String,
    val members: List<Member>,
    val name: String,
    val type: String,
    val updatedAt: String
)

data class Member(
    val _id: String,
    val isDeleted: Boolean,
    val joinedAt: String,
    val mute: Boolean,
    val userId: String
)
}