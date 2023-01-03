package com.android.vadify.data.api.models

import com.android.vadify.viewmodels.EncryptionViewModel

data class ListOfUserResponse(
    val data: List<Data>,
    val message: String,
    val statusCode: Int
) {
    data class Data(
        val _id: String,
        var lastMessage: String?                      ,
        var lastMessageSender: String,
        var lastMessageReceiver: String,
        var lastMessageDate: String,
        val lastMessageFrom: String,
        var lastMessageType: String,
        val members: List<Member>,
        val type: String,
        val name: String,
        val profileImage: String,
        val unreadCount: Int,
        val user: User,
        var isUserBlock: Boolean = false
    ) {

        data class Member(
            var mute: Boolean,
            val name: String,
            val number: String,
            val profileImage: String,
            val userId: String,
            val profileStatus: String,
            var motherSwitch: Boolean,
            var languageCode : String,
            var language: String,
            var lastMessage: String

        )

        data class User(
            var mute: Boolean,
            val name: String,
            val number: String,
            val profileImage: String,
            val profileStatus: String,
            val userId: String,
            val lastMessage: String
        )
    }
}