package com.example.antibully.data.ui.settings

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.antibully.data.api.AuthRetrofitClient
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class SecuritySettingsViewModel : ViewModel() {

    private val _twoFactorEnabled = MutableLiveData<Boolean>()
    val twoFactorEnabled: LiveData<Boolean> = _twoFactorEnabled

    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?> = _error

    private val _success = MutableLiveData<String?>()
    val success: LiveData<String?> = _success

    init {
        checkTwoFactorStatus()
    }

    private fun checkTwoFactorStatus() {
        viewModelScope.launch {
            try {
                val token = FirebaseAuth.getInstance().currentUser?.getIdToken(false)?.await()?.token
                if (token != null) {
                    val response = AuthRetrofitClient.authService.checkTwoFactorStatus("Bearer $token")
                    _twoFactorEnabled.postValue(response.twoFactorEnabled)
                } else {
                    _twoFactorEnabled.postValue(false)
                    _error.postValue("לא נמצא משתמש מחובר")
                }
            } catch (e: Exception) {
                _twoFactorEnabled.postValue(false)
                _error.postValue("שגיאה בבדיקת סטטוס: ${e.message}")
            }
        }
    }

    fun refreshStatus() {
        checkTwoFactorStatus()
    }

    fun requestDisableTwoFactor(code: String) {
        viewModelScope.launch {
            try {
                val token = FirebaseAuth.getInstance().currentUser?.getIdToken(false)?.await()?.token
                if (token != null) {
                    val verifyResponse = AuthRetrofitClient.authService.verify2FA(
                        "Bearer $token",
                        mapOf("twoFactorCode" to code)
                    )

                    if (verifyResponse.isSuccessful) {
                        val updateResponse = AuthRetrofitClient.authService.updateTwoFactorStatus(
                            "Bearer $token",
                            mapOf("enabled" to false)
                        )

                        if (updateResponse.isSuccessful) {
                            _twoFactorEnabled.postValue(false)
                            _success.postValue("אימות דו-שלבי בוטל בהצלחה") // ✅ הוספתי את זה
                        } else {
                            _error.postValue("שגיאה בעדכון מצב אימות דו-שלבי")
                            checkTwoFactorStatus()
                        }
                    } else {
                        _error.postValue("קוד לא תקין")
                        checkTwoFactorStatus()
                    }
                } else {
                    _error.postValue("לא נמצא משתמש מחובר")
                }
            } catch (e: Exception) {
                _error.postValue("שגיאה בביטול אימות דו-שלבי: ${e.message}")
                checkTwoFactorStatus()
            }
        }
    }

    fun clearError() {
        _error.value = null
    }

    fun clearSuccess() {
        _success.value = null
    }
}
