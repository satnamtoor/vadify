package com.android.vadify.data.api.models


data class MessageResponseList(
    val data: Data,
    val message: String,
    val statusCode: Int
) {
    data class Data(
        val data: List<DataX>,
        val total: Int
    ) {
        data class DataX(
            val __v: Int,
            val name : String,
            val _id: String,
            val createdAt: String,
            val from: From?,
            var members: List<Member>,
            var message: String?,
            var messageSender: String?,
            var messageReceiver: String?,
            val roomId: String,
            val type: String,
            val updatedAt: String,
            val url: String?,
            val lat: String?,
            val lng: String?,
            val contact: Contact?,
            var translatedText: String?,
            var replyToMessageId: String?,
            var forwarded: Boolean?,
            var chatType: String?,
            var progress : Int?
        ) {

            data class From(
                val _id: String?,
                val name: String?,
                val number: String?,
                val profileImage: String?,
                val language:String?
            )

            data class Member(
                val _id: String,
                val delivered: Boolean,
                val deliveredAt: String,
                val isDeleted: Boolean,
                val read: Boolean,
                val readAt: String,
                val userId: String,
                val language:String,
                var message:String
            )

            data class Contact(
                val name: String,
                val number: String
            )
        }
    }
}

