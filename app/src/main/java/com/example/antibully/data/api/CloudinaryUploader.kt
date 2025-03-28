package com.example.antibully.data.api


import android.content.Context
import android.net.Uri
import android.util.Log
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.IOException

object CloudinaryUploader {

    private const val CLOUD_NAME = "dddcxg6w1"
    private const val UPLOAD_PRESET = "antibully_uploads"

    fun uploadImage(
        context: Context,
        imageUri: Uri,
        onSuccess: (String) -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        try {
            val inputStream = context.contentResolver.openInputStream(imageUri)
            val imageBytes = inputStream?.readBytes() ?: throw Exception("Image is null or unreadable")
            inputStream.close()

            val requestBody = MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart(
                    "file",
                    "image.jpg",
                    imageBytes.toRequestBody("image/*".toMediaTypeOrNull())
                )
                .addFormDataPart("upload_preset", UPLOAD_PRESET)
                .build()

            val request = Request.Builder()
                .url("https://api.cloudinary.com/v1_1/$CLOUD_NAME/image/upload")
                .post(requestBody)
                .build()

            OkHttpClient().newCall(request).enqueue(object : Callback {
                override fun onFailure(call: Call, e: IOException) {
                    Log.e("CloudinaryUpload", "Failed: ${e.message}", e)
                    onFailure(e)
                }

                override fun onResponse(call: Call, response: Response) {
                    val body = response.body?.string()
                    if (response.isSuccessful && body != null) {
                        val json = JSONObject(body)
                        val secureUrl = json.getString("secure_url")
                        Log.d("CloudinaryUpload", "Success! URL: $secureUrl")
                        onSuccess(secureUrl)
                    } else {
                        val error = body ?: "Unknown error"
                        Log.e("CloudinaryUpload", "Failed with response: $error")
                        onFailure(Exception("Upload failed: $error"))
                    }
                }
            })

        } catch (e: Exception) {
            Log.e("CloudinaryUpload", "Exception: ${e.message}", e)
            onFailure(e)
        }
    }
}