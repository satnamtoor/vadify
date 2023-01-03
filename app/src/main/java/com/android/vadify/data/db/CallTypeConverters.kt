package com.android.vadify.data.db

import androidx.room.TypeConverter
import com.android.vadify.data.api.models.CallLogResponse
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken


class CallTypeConverters {

    @TypeConverter
    fun restoreList(listOfString: String?): List<CallLogResponse.DataX.Member?>? {
        return Gson().fromJson(listOfString, object :
            TypeToken<List<CallLogResponse.DataX.Member>?>() {}.type)
    }

    @TypeConverter
    fun saveList(listOfString: List<CallLogResponse.DataX.Member>): String? {
        return Gson().toJson(listOfString)
    }
}





