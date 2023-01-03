package com.android.vadify.data.db

import androidx.room.TypeConverter
import com.android.vadify.data.api.models.ListOfUserResponse
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class ChatThreadMemberConverter {
    @TypeConverter
    fun restoreList(listOfString: String?): List<ListOfUserResponse.Data.Member?>? {
        return Gson().fromJson(listOfString, object :
            TypeToken<List<ListOfUserResponse.Data.Member>?>() {}.type)
    }

    @TypeConverter
    fun saveList(listOfString: List<ListOfUserResponse.Data.Member>): String? {
        return Gson().toJson(listOfString)
    }
}

class ChatThreadUserConverter {
    @TypeConverter
    fun restoreList(string: String?): ListOfUserResponse.Data.User? {
        return Gson().fromJson(string, object :
            TypeToken<ListOfUserResponse.Data.User?>() {}.type)
    }

    @TypeConverter
    fun saveList(string: ListOfUserResponse.Data.User): String? {
        return Gson().toJson(string)
    }
}