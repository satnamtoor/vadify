package com.android.vadify.di

import android.util.Log
import com.android.vadify.BuildConfig
import com.android.vadify.R
import com.android.vadify.data.api.AuthApi
import com.android.vadify.data.api.AwsUploadApi
import com.android.vadify.data.api.ChatApi
import com.android.vadify.data.api.FileUploadApi
import com.android.vadify.data.service.PreferenceService
import dagger.Module
import dagger.Provides
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit
import javax.inject.Named
import javax.inject.Singleton


@Module
class ApiModule {

    @Singleton
    @Provides
    fun provideRetrofit(
        @Named("api_url") baseUrl: String,
        httpClient: OkHttpClient
    ): Retrofit {
        return Retrofit.Builder().baseUrl(baseUrl).client(httpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .addCallAdapterFactory(RxJava2CallAdapterFactory.create()).build()
    }

    @Singleton
    @Provides
    fun provideAuthApi(retrofit: Retrofit): AuthApi {
        return buildApiClient(retrofit, AuthApi.MODULE_PATH).create(AuthApi::class.java)
    }

    @Singleton
    @Provides
    fun provideChatApi(retrofit: Retrofit): ChatApi {
        return buildApiClient(retrofit, ChatApi.MODULE_PATH).create(ChatApi::class.java)
    }


    @Singleton
    @Provides
    fun provideFileUploadApi(retrofit: Retrofit): FileUploadApi {
        return buildApiClient(retrofit, FileUploadApi.MODULE_PATH).create(FileUploadApi::class.java)
    }

    @Singleton
    @Provides
    fun provideFileUploadApiAWS(retrofit: Retrofit): AwsUploadApi {
        return buildApiClient(retrofit, AwsUploadApi.MODULE_PATH).create(AwsUploadApi::class.java)
    }

    @Singleton
    @Provides
    fun provideOkHttpClient(
        loggingInterceptor: HttpLoggingInterceptor,
        preferenceService: PreferenceService
    ): OkHttpClient {
        val httpClient = OkHttpClient.Builder().apply {
            readTimeout(HTTP_TIMEOUT, TimeUnit.SECONDS)
            connectTimeout(HTTP_TIMEOUT, TimeUnit.SECONDS)
        }
        Log.d( "authontication: ",""+ preferenceService.getString(R.string.pkey_secure_token))
        httpClient.addNetworkInterceptor(loggingInterceptor)
//        httpClient.addNetworkInterceptor(authenticationInterceptor)
//        httpClient.addNetworkInterceptor(deprecatedInterceptor)
        httpClient.addInterceptor { chain ->
            val ongoing = chain.request().newBuilder()
            ongoing.addHeader(
                "Authorization",
                preferenceService.getString(R.string.pkey_secure_token) ?: ""
            )
            ongoing.addHeader("Content-Type", "application/json;charset=utf-8")
            ongoing.addHeader("Accept", "application/json; application/problem+json")
            val response = chain.proceed(ongoing.build())
            response
        }
        return httpClient.build()
    }

    @Singleton
    @Provides
    @Named("api_url")
    fun provideApiUrl(): String {
        return BuildConfig.API_URL
    }

    private fun buildApiClient(retrofit: Retrofit, path: String): Retrofit {
        return retrofit.newBuilder().baseUrl(retrofit.baseUrl().toUrl().toString() + path)
            .validateEagerly(true).build()
    }

    companion object {
        const val HTTP_TIMEOUT = 40L
    }

    @Singleton
    @Provides
    fun provideLoggingInterceptor(): HttpLoggingInterceptor {
        return HttpLoggingInterceptor().setLevel(HttpLoggingInterceptor.Level.BODY)
    }
}