package com.android.vadify.di

data class model(
    val `data`: Data,
    val message: String,
    val statusCode: Int
)

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
    val isActive: Boolean,
    val isDeleted: Boolean,
    val joinedAt: String,
    val mute: Boolean,
    val type: String,
    val userId: String
)