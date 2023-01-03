package com.android.vadify.data.api

import com.android.vadify.data.api.models.ImageUploadResponse
import io.reactivex.Single
import okhttp3.MultipartBody
import okhttp3.RequestBody
import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.Response
import retrofit2.http.*


interface FileUploadApi {

    companion object {
        const val MODULE_PATH = "api/v1/common/"
    }

    @Multipart
    @POST("upload/")
    fun uploadImage(
        @Part file: MultipartBody.Part?,
        @Part("type") userId: RequestBody?
    ): Single<Response<ImageUploadResponse>>


    @Multipart
    @POST("upload/")
    fun uploadImageWithProgress(
        @Part file: MultipartBody.Part?,
        @Part("type") userId: RequestBody?
    ): Call<ImageUploadResponse>


    @PUT

    fun uploadAsset(
        @Url uploadUrl: String,
        @Body  file: RequestBody
    ):Call<ResponseBody>
}