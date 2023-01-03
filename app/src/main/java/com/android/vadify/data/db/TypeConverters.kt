package com.android.vadify.data.db

import androidx.room.TypeConverter
import com.android.vadify.data.api.models.MessageResponseList
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken


class DataConverter {

    @TypeConverter
    fun restoreList(listOfString: String?): List<MessageResponseList.Data.DataX.Member?>? {
        return Gson().fromJson(listOfString, object :
            TypeToken<List<MessageResponseList.Data.DataX.Member>?>() {}.type)
    }

    @TypeConverter
    fun saveList(listOfString: List<MessageResponseList.Data.DataX.Member>): String? {
        return Gson().toJson(listOfString)
    }
}





