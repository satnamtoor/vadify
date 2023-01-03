package com.android.vadify.data.network

import com.android.vadify.R
import com.google.gson.FieldNamingPolicy
import com.google.gson.GsonBuilder
import com.google.gson.JsonSyntaxException
import com.sdi.joyersmajorplatform.uiview.NetworkState
import retrofit2.Response
import java.net.HttpURLConnection

interface IRetrofitNetworkRequestCallback<RemoteType : Any> :
    INetworkRequestCallback<Response<RemoteType>> {


    data class ResponseData(val message: String, val statusCode: Int)


    override fun getErrorState(response: Response<RemoteType>): NetworkState {
        return when (response.code()) {
            HttpURLConnection.HTTP_FORBIDDEN -> NetworkState.error(
                response.message(),
                R.string.network_error_forbidden
            )
            HttpURLConnection.HTTP_UNAUTHORIZED, HttpURLConnection.HTTP_BAD_REQUEST -> parseErrorBody(
                response
            )
            else -> NetworkState.error(response.message(), R.string.network_error_unknown)
        }
    }


    private fun parseErrorBody(response: Response<RemoteType>): NetworkState {
        val gson = GsonBuilder().setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES)
            .create()
        return try {
            val body: ResponseData? =
                gson.fromJson(response.errorBody()?.string(), ResponseData::class.java)
            body?.message?.let {
                NetworkState.error(it)
            } ?: NetworkState.error(R.string.network_error_unknown)
        } catch (e: JsonSyntaxException) {
            // Timber.w(e)
            NetworkState.error(R.string.network_error_unknown)
        }
    }


    override fun isSuccess(response: Response<RemoteType>) = response.isSuccessful


    /**
     * Implementation of [INetworkResourceCallback] for Retrofit and [Response]
     */
    interface IRetrofitNetworkResourceCallback<LocalType, RemoteType : Any> :
        INetworkResourceCallback<LocalType, RemoteType, Response<RemoteType>>,
        IRetrofitNetworkRequestCallback<RemoteType> {

        override fun extractData(response: Response<RemoteType>): RemoteType {
            return requireNotNull(response.body())
        }
    }
}