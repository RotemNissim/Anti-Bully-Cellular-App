package com.example.antibully.data.ui.common

import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.EditText
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.lifecycleScope
import com.example.antibully.R
import com.example.antibully.data.api.AuthRetrofitClient
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class VerifyTwoFactorDialogFragment(private val onVerified: (Boolean) -> Unit) : DialogFragment() {

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val view = LayoutInflater.from(context).inflate(R.layout.fragment_verify_two_factor_dialog, null)
        val codeInput = view.findViewById<EditText>(R.id.etVerificationCode)

        return AlertDialog.Builder(requireContext())
            .setTitle("Two-Factor Authentication")
            .setMessage("Please enter your authentication code.")
            .setView(view)
            .setPositiveButton("Verify") { _, _ ->
                val code = codeInput.text.toString().trim()
                if (code.length == 6) {
                    verifyCode(code)
                } else {
                    onVerified(false)
                }
            }
            .setNegativeButton("Cancel") { _, _ ->
                onVerified(false)
            }
            .create()
    }

    private fun verifyCode(code: String) {
        lifecycleScope.launch {
            try {
                val token = FirebaseAuth.getInstance().currentUser?.getIdToken(false)?.await()?.token
                if (token != null) {
                    val response = AuthRetrofitClient.authService.verify2FA(
                        "Bearer $token",
                        mapOf("twoFactorCode" to code)
                    )
                    onVerified(response.isSuccessful)
                } else {
                    onVerified(false)
                }
            } catch (e: Exception) {
                onVerified(false)
            }
        }
    }
}
