package com.example.antibully.data.api

import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object RetrofitClient {
    // === Message API ===
    private const val MESSAGE_BASE_URL =
        "https://67cd7757dd7651e464ee70df.mockapi.io/api/v1/"

    // === Alerts API ===
    private const val ALERT_BASE_URL = "http://10.0.2.2:3000/"
    private val logging = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY
    }

    private val httpClient = OkHttpClient.Builder()
        .addInterceptor(logging)
        .build()

    private val messageRetrofit: Retrofit by lazy {
        Retrofit.Builder()
            .baseUrl(MESSAGE_BASE_URL)
            .client(httpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    val apiService: MessageApiService by lazy {
        messageRetrofit.create(MessageApiService::class.java)
    }

    private val alertRetrofit: Retrofit by lazy {
        Retrofit.Builder()
            .baseUrl(ALERT_BASE_URL)
            .client(httpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    val alertApiService: AlertApiService by lazy {
        alertRetrofit.create(AlertApiService::class.java)
    }

    private val childRetrofit: Retrofit by lazy {
        Retrofit.Builder()
            .baseUrl(ALERT_BASE_URL) // Assuming the base URL is the same as MESSAGE_BASE_URL
            .client(httpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    val childApiService: ChildApiService by lazy {
        childRetrofit.create(ChildApiService::class.java)
    }
    private val userRetrofit: Retrofit by lazy {
        Retrofit.Builder()
            .baseUrl(ALERT_BASE_URL)
            .client(httpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }
    val userApiService: UserApiService by lazy {
        userRetrofit.create(UserApiService::class.java)
    }
}
