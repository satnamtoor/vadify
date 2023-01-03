package com.android.vadify.data.api.models

data class CallLogResponse(
    val data: Data,
    val message: String,
    val statusCode: Int
) {
    data class Data(
        val data: List<DataX>,
        val total: Int
    )

    data class DataX(
        val __v: Int,
        val _id: String,
        val createdAt: String,
        val from: String,
        val members: List<Member>,
        val mode: String,
        val roomId: RoomId,
        val type: String,
        val updatedAt: String
    ) {

        data class RoomId(
            val _id: String,
            val name : String? = null
        )

        data class Member(
            val duration: Int,
            val isDeleted: Boolean,
            val name: String,
            val number: String,
            val profileImage: String,
            val read: Boolean,
            val status: String,
            val userId: String
        )
    }
}