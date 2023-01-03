package com.android.vadify.data.api.models

data class CreateRoomResponce(
        val `data`: Data1,
        val message: String,
        val statusCode: Int
    )

    data class Data1(
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
        val isActive: Boolean,
        val isDeleted: Boolean,
        val joinedAt: String,
        val mute: Boolean,
        val type: String,
        val userId: String
    )
