package com.android.vadify.data.api

import com.android.vadify.data.api.models.YoutubeResponse
import retrofit2.Call
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET

interface ApiInterface {

    @GET("search?key=AIzaSyD8NENkeXFSKT5w7ZJsh4fEz0lGyf_NT-U&channelId=UCcEcrH7HaE5JTnbkNW-ky8A&part=snippet,id&order=date&maxResults=20")
    fun getPhotos(): Call<YoutubeResponse>

}