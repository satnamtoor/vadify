package com.android.vadify.data.api.models

data class ContactSyncingResponse(
    val data: Data,
    val message: String,
    val statusCode: Int
) {
    data class Data(
        val notRegistered: List<String>,
        val users: List<User>
    ) {
        data class User(
            val _id: String,
            var name: String,
            val number: String,
            val profileImage: String,
            val languageCode: String,
            val profileStatus: String,
            val language: String,
            val isBlocked: Boolean

            //   val profileImage: String,
        )
    }

    data class UserGroup(
        val _id: String,
        var name: String,
        val number: String,
        val profileImage: String

        //   val profileImage: String,
    )


}