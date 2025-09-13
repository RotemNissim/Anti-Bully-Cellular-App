package com.example.antibully.data.api


import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object AuthRetrofitClient {
    // Use the value injected by Gradle (changes between debug/release)
    private const val BASE_PATH = "api/"  // only the relative path

    val authService: AuthApiService by lazy {
        Retrofit.Builder()
            .baseUrl("http://193.106.55.138:3000/" + BASE_PATH)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(AuthApiService::class.java)
    }
}
