package com.example.antibully.data.api

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object RetrofitClient {
    private const val BASE_URL = "https://67cd7757dd7651e464ee70df.mockapi.io/api/v1/"

    val apiService: MessageApiService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(MessageApiService::class.java)
    }
}
