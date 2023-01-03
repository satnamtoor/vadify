package com.android.vadify.data.db.filter

import com.android.vadify.data.api.models.MessageResponseList
import com.android.vadify.ui.util.callMethod


class ChatListTranslatedText {
    object Mapper {
        fun from(
            data: List<MessageResponseList.Data.DataX>,
            languageCode: String
        ): List<MessageResponseList.Data.DataX> {
            data.map { item ->
                item.message?.let {
                    if (it.isNotEmpty()) item.translatedText = callMethod(true, it, languageCode)
                    else item.translatedText = item.message ?: ""
                }
            }
            return data
        }

        fun fromObject(
            data: MessageResponseList.Data.DataX,
            languageCode: String
        ): MessageResponseList.Data.DataX {
            data.message?.let {
                if (it.isNotEmpty()) data.translatedText = callMethod(true, it, languageCode)
                else data.translatedText = data.message ?: ""
            }
            return data
        }
    }
}