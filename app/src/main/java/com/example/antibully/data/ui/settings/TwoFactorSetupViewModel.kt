package com.example.antibully.data.ui.settings

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Base64
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.antibully.data.api.AuthRetrofitClient
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class TwoFactorSetupViewModel : ViewModel() {
    private val _qrCodeBitmap = MutableLiveData<Bitmap>()
    val qrCodeBitmap: LiveData<Bitmap> = _qrCodeBitmap

    private val _setupSuccess = MutableLiveData<Boolean>()
    val setupSuccess: LiveData<Boolean> = _setupSuccess

    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?> = _error

    private val _twoFactorEnabled = MutableLiveData<Boolean>()
    val twoFactorEnabled: LiveData<Boolean> = _twoFactorEnabled

    private var tempSecret: String? = null

    init {
        checkTwoFactorStatus()
    }

    private fun checkTwoFactorStatus() {
        viewModelScope.launch {
            try {
                val token = FirebaseAuth.getInstance().currentUser?.getIdToken(false)?.await()?.token
                if (token != null) {
                    val response = AuthRetrofitClient.authService.checkTwoFactorStatus("Bearer $token")
                    _twoFactorEnabled.value = response.twoFactorEnabled
                } else {
                    _twoFactorEnabled.value = false
                    _error.postValue("לא נמצא משתמש מחובר")
                }
            } catch (e: Exception) {
                _twoFactorEnabled.value = false
                _error.postValue("שגיאה בבדיקת סטטוס: ${e.message}")
            }
        }
    }

    fun generateQrCode() {
        viewModelScope.launch {
            try {
                val token = FirebaseAuth.getInstance().currentUser?.getIdToken(false)?.await()?.token
                if (token != null) {
                    val response = AuthRetrofitClient.authService.setup2FA("Bearer $token")
                    tempSecret = response.secret

                    val base64Image = response.qrCode.substringAfter("base64,")
                    val decodedBytes = Base64.decode(base64Image, Base64.DEFAULT)
                    val bitmap = BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.size)
                    _qrCodeBitmap.value = bitmap
                } else {
                    _error.value = "לא נמצא משתמש מחובר"
                }
            } catch (e: Exception) {
                _error.value = "שגיאה בקבלת קוד QR: ${e.message}"
            }
        }
    }

    fun verifyCode(code: String) {
        viewModelScope.launch {
            try {
                val token = FirebaseAuth.getInstance().currentUser?.getIdToken(false)?.await()?.token
                if (token != null && tempSecret != null) {
                    val body = mapOf(
                        "twoFactorCode" to code,
                        "tempSecret" to tempSecret!!
                    )

                    val response = AuthRetrofitClient.authService.verify2FA("Bearer $token", body)
                    if (response.isSuccessful) {
                        val updateResponse = AuthRetrofitClient.authService.updateTwoFactorStatus(
                            "Bearer $token",
                            mapOf("enabled" to true)
                        )

                        if (updateResponse.isSuccessful) {
                            _setupSuccess.value = true
                            _twoFactorEnabled.value = true
                        } else {
                            _error.value = "האימות הצליח אך עדכון הסטטוס נכשל"
                        }
                    } else {
                        _error.value = "קוד לא תקין"
                    }
                } else {
                    _error.value = "חסרים נתונים לאימות"
                }
            } catch (e: Exception) {
                _error.value = "שגיאה באימות: ${e.message}"
            }
        }
    }

    fun isTwoFactorAlreadyEnabled(): Boolean {
        return _twoFactorEnabled.value == true
    }

    fun clearError() {
        _error.value = null
    }
}
