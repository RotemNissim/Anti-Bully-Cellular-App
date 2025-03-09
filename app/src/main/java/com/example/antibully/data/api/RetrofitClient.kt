package com.example.antibully.data.api

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object RetrofitClient {
    private const val API_PREFIX = "api/v1/" // Define API prefix
    private const val BASE_URL = "https://67cd7757dd7651e464ee70df.mockapi.io/"

    val instance: FlagApiService by lazy {
        val retrofit = Retrofit.Builder()
            .baseUrl(BASE_URL + API_PREFIX) // Apply prefix globally
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        retrofit.create(FlagApiService::class.java)
    }
}
