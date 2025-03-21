package com.example.antibully.data.api

import retrofit2.Response
import java.io.IOException

object ApiHelper {
    suspend fun <T> safeApiCall(apiCall: suspend () -> Response<T>): Result<T> {
        return try {
            val response = apiCall()
            if (response.isSuccessful) {
                response.body()?.let {
                    Result.success(it)
                } ?: Result.failure(IOException("Empty response body"))
            } else {
                Result.failure(IOException("API error: ${response.errorBody()?.string()}"))
            }
        } catch (e: Exception) {
            Result.failure(IOException("Network failure: ${e.message}"))
        }
    }
}
