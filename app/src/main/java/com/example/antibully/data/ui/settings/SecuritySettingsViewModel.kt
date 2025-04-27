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
                    _error.postValue("No connected user found")
                }
            } catch (e: Exception) {
                _twoFactorEnabled.postValue(false)
                _error.postValue("Error checking status: ${e.message}")
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
                            _success.postValue("Two-Factor Authentication disabled successfully")
                        } else {
                            _error.postValue("Error updating Two-Factor Authentication status")
                            checkTwoFactorStatus()
                        }
                    } else {
                        _error.postValue("Invalid authentication code")
                        checkTwoFactorStatus()
                    }
                } else {
                    _error.postValue("No connected user found")
                }
            } catch (e: Exception) {
                _error.postValue("Error disabling Two-Factor Authentication: ${e.message}")
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
