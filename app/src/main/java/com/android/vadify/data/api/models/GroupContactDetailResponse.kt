package com.android.vadify.data.api.models

data class GroupContactDetailResponse(
    val data: Data,
    val message: String,
    val statusCode: Int
) {
    data class Data(
        val `data`: DataX,
        val mediaLinkCount: Int
    )

    data class DataX(
        val _id: String,
        val createdAt: String,
        val members: List<Member>,
        val name: String,
        val profileImage: String?
    )

    data class Member(
        val userId: UserId
    )

    data class UserId(
        val _id: String,
        val isSupportNumber: Boolean,
        val name: String,
        val number: String,
        val profileImage: String,
        val profileStatus: String
    )
}