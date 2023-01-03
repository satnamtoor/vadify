package com.android.vadify.data.api

import com.android.vadify.data.api.models.MessageResponseList
import com.google.gson.JsonObject
import io.reactivex.Single
import retrofit2.Call
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Path


interface ChatApi {

    companion object {
        const val MODULE_PATH = "api/v1/room/"
    }

    @GET("{userId}/")
    fun getUserList(@Path("userId") userId: String?): Call<JsonObject>

    @GET("chat/{roomId}/{userId}")
    fun getChatList(
        @Path("roomId") roomId: String?,
        @Path("userId") userId: String?
    ): Single<Response<MessageResponseList>>


}