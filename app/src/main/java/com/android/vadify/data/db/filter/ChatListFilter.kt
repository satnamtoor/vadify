package com.android.vadify.data.db.filter

import com.android.vadify.data.api.models.ListOfUserResponse
import com.android.vadify.widgets.restoreList


class ChatListFilter {
    object Mapper {
        fun from(name: String?, response: ListOfUserResponse): ListOfUserResponse {
            try {
                if (name.isNullOrBlank()) return response
                val list = restoreList<String>(name)
                if (!list.isNullOrEmpty()) {
                    response.data.map { data ->
                        val condition = list.map { data.members[0].userId == it }.single()
                        if (condition) data.isUserBlock = true
                        data
                    }
                }
            } catch (e: Exception) {
            }
            return response
        }
    }
}