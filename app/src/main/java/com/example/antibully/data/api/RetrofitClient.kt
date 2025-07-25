package com.example.antibully.data.api

import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object RetrofitClient {
    // === Message API ===
    private const val MESSAGE_BASE_URL =
        "https://67cd7757dd7651e464ee70df.mockapi.io/api/v1/"

    // === Main API (Alerts, Auth, Children) ===
    private const val API_BASE_URL = "http://10.0.2.2:3000/"

    // Shared logging interceptor for debugging
    private val logging = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY
    }

    private val httpClient = OkHttpClient.Builder()
        .addInterceptor(logging)
        .build()

    // Retrofit for messages (unchanged)
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

    // ✅ Retrofit for main API (alerts, auth, children)
    private val mainApiRetrofit: Retrofit by lazy {
        Retrofit.Builder()
            .baseUrl(API_BASE_URL)
            .client(httpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    // ✅ All API services using the same base URL
    val alertApiService: AlertApiService by lazy {
        mainApiRetrofit.create(AlertApiService::class.java)
    }

    val authApiService: AuthApiService by lazy {
        mainApiRetrofit.create(AuthApiService::class.java)
    }

    val childApiService: ChildApiService by lazy {
        mainApiRetrofit.create(ChildApiService::class.java)
    }
}
//package com.example.antibully.data.api
//
//import okhttp3.OkHttpClient
//import okhttp3.logging.HttpLoggingInterceptor
//import retrofit2.Retrofit
//import retrofit2.converter.gson.GsonConverterFactory
//
//object RetrofitClient {
//    // === Message API ===
//    private const val MESSAGE_BASE_URL =
//        "https://67cd7757dd7651e464ee70df.mockapi.io/api/v1/"
//
//    // === Alerts API ===
//    private const val ALERT_BASE_URL = "http://10.0.2.2:3000/"
//
//    // Shared logging interceptor for debugging
//    private val logging = HttpLoggingInterceptor().apply {
//        level = HttpLoggingInterceptor.Level.BODY
//    }
//
//    private val httpClient = OkHttpClient.Builder()
//        .addInterceptor(logging)
//        .build()
//
//    // Retrofit for messages (unchanged)
//    private val messageRetrofit: Retrofit by lazy {
//        Retrofit.Builder()
//            .baseUrl(MESSAGE_BASE_URL)
//            .client(httpClient)
//            .addConverterFactory(GsonConverterFactory.create())
//            .build()
//    }
//
//    val apiService: MessageApiService by lazy {
//        messageRetrofit.create(MessageApiService::class.java)
//    }
//
//    // Retrofit for alerts
//    private val alertRetrofit: Retrofit by lazy {
//        Retrofit.Builder()
//            .baseUrl(ALERT_BASE_URL)
//            .client(httpClient)
//            .addConverterFactory(GsonConverterFactory.create())
//            .build()
//    }
//
//    val alertApiService: AlertApiService by lazy {
//        alertRetrofit.create(AlertApiService::class.java)
//    }
//
//    // Retrofit for children (newly added)
//    private val childRetrofit: Retrofit by lazy {
//        Retrofit.Builder()
//            .baseUrl(ALERT_BASE_URL) // Assuming the base URL is the same as MESSAGE_BASE_URL
//            .client(httpClient)
//            .addConverterFactory(GsonConverterFactory.create())
//            .build()
//    }
//
//    val childApiService: ChildApiService by lazy {
//        childRetrofit.create(ChildApiService::class.java)
//    }
//}
