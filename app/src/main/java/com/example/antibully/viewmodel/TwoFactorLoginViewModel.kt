package com.example.antibully.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.antibully.data.repository.TwoFactorAuthRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class TwoFactorLoginViewModel : ViewModel() {

    private val repository = TwoFactorAuthRepository()

    private val _verificationStatus = MutableLiveData<Boolean>()
    val verificationStatus: LiveData<Boolean> get() = _verificationStatus

    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?> get() = _error

    fun verifyCode(token: String, code: String) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val response = repository.verify2FA(token, code)
                if (response.isSuccessful) {
                    _verificationStatus.postValue(true)
                    _error.postValue(null)
                } else {
                    _verificationStatus.postValue(false)
                    _error.postValue("אימות נכשל")
                }
            } catch (e: Exception) {
                _verificationStatus.postValue(false)
                _error.postValue("שגיאה: ${e.localizedMessage}")
            }
        }
    }
}
