package com.android.vadify.data.api

import com.android.vadify.data.api.models.ImageUploadResponse
import com.android.vadify.data.api.models.UploadAWSResponse
import com.android.vadify.data.api.models.UploadAwsURL
import io.reactivex.Single
import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.Call
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part

interface AwsUploadApi {
    companion object {
        const val MODULE_PATH = "api/v1/common/"
    }

   /* @Multipart
    @POST("upload/")
    fun uploadImage(
        @Part file: MultipartBody.Part?,
        @Part("type") userId: RequestBody?
    ): Single<Response<ImageUploadResponse>>*/
   /*@POST("common/getpresignedurl")
   fun uploadImageAws(@Body singleRequest: UploadAwsURL): Single<Response<UploadAWSResponse>>*/


    @POST("getpresignedurl/")
    fun uploadImageWithProgress(
        @Body singleRequest: UploadAwsURL
    ): Call<UploadAWSResponse>


}