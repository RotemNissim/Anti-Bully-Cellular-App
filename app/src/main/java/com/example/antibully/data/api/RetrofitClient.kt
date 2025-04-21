package com.example.antibully.data.api

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object RetrofitClient {
    // your existing Message API URL
    private const val MESSAGE_BASE_URL = "https://67cd7757dd7651e464ee70df.mockapi.io/api/v1/"

    // new Alerts API URL
    private const val ALERT_BASE_URL   = "http://10.0.2.2:3000/"

    // Retrofit instance for messages
    private val messageRetrofit: Retrofit by lazy {
        Retrofit.Builder()
            .baseUrl(MESSAGE_BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    // Retrofit instance for alerts
    private val alertRetrofit: Retrofit by lazy {
        Retrofit.Builder()
            .baseUrl(ALERT_BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    // keep this exactly as before for existing dependencies:
    val apiService: MessageApiService by lazy {
        messageRetrofit.create(MessageApiService::class.java)
    }

    // new entry point for your alerts calls:
    val alertApiService: AlertApiService by lazy {
        alertRetrofit.create(AlertApiService::class.java)
    }
}