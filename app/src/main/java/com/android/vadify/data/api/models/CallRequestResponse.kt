package com.android.vadify.data.api.models

import android.os.Parcelable
import kotlinx.android.parcel.Parcelize

data class CallRequestResponse(
    val data: Data,
    val message: String,
    val statusCode: Int
) {
    data class Data(
        val call: Call,
        val token: String
    ) {
        @Parcelize
        data class Call(
            val __v: Int,
            val _id: String,
            val createdAt: String,
            val from: From,
            val members: List<Member>,
            val mode: String,
            val status: String,
            val roomId: RoomId,
            val type: String,
            val updatedAt: String
        ) : Parcelable {
            @Parcelize
            data class RoomId(val _id: String) : Parcelable

            @Parcelize
            data class From(
                val _id: String,
                var name: String,
                val number: String,
                val profileImage: String
            ) : Parcelable

            @Parcelize
            data class Member(
                val _id: String?,
                val duration: Int,
                val isDeleted: Boolean,
                val read: Boolean,
                val status: String,
                val userId: String,
                val name:String
            ) : Parcelable
        }


    }
}