package com.android.vadify.data.api.models

data class ContactDetailResponse(
    val data: Data,
    val message: String,
    val statusCode: Int
) {
    data class Data(
        val contactDetail: ContactDetail,
        val groups: List<Any>,
        val mediaLinksCount: Int
    )

    data class ContactDetail(
        val _id: String,
        val language: String,
        val name: String,
        val number: String,
        val profileStatus: String,
        val profileImage: String
    )
}
