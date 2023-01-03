package com.android.vadify.data.repository

import com.android.vadify.data.api.FileUploadApi
import com.android.vadify.data.api.models.ImageUploadResponse
import com.android.vadify.data.network.AppExecutors
import com.android.vadify.data.network.IRequest
import com.android.vadify.data.network.IRetrofitNetworkRequestCallback
import com.android.vadify.data.network.NetworkRequest
import com.android.vadify.widgets.multiPartObject
import com.android.vadify.widgets.requestBody
import io.reactivex.Single
import retrofit2.Response
import java.io.File
import javax.inject.Inject


class FileUploadRepositry @Inject constructor(
    private val appExecutors: AppExecutors,
    private val fileUploadApi: FileUploadApi
) {


//    media/type

    companion object {
        const val RESPONSE_SUCCESS = 1
        const val IMAGE_FILE = "image/*"
        const val AUDIO_FILE = "audio/*"
        const val VIDEO_FILE = "video/*"
        const val DOCUMENT_FILE = "*/*"
    }

    fun uploadProfilePic(userImage: File?, type: String): IRequest<Response<ImageUploadResponse>> {
        return NetworkRequest(
            appExecutors,
            object : IRetrofitNetworkRequestCallback<ImageUploadResponse> {
                override fun createNetworkRequest(): Single<Response<ImageUploadResponse>> {
                    val userImagePart = userImage?.multiPartObject("file", IMAGE_FILE)
                    val typePart = type.requestBody()
                    return fileUploadApi.uploadImage(userImagePart, typePart)
                }
            })

    }


}
