package com.example.antibully.data.ui.common

import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ProgressBar
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.lifecycleScope
import com.example.antibully.R
import com.example.antibully.data.api.RetrofitClient
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class VerifyTwoFactorDialogFragment(private val onVerified: (Boolean) -> Unit) : DialogFragment() {

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val view: View = LayoutInflater.from(requireContext())
            .inflate(R.layout.fragment_verify_two_factor_dialog, null)

        val codeInput = view.findViewById<EditText>(R.id.codeInput)
        val cancelButton = view.findViewById<Button>(R.id.cancelButton)
        val verifyButton = view.findViewById<Button>(R.id.verifyButton)
        val progressBar = view.findViewById<ProgressBar>(R.id.progressBar)

        val dialog = AlertDialog.Builder(requireContext(), R.style.CustomDialogTheme)
            .setView(view)
            .create()

        cancelButton.setOnClickListener {
            onVerified(false)
            dialog.dismiss()
        }

        verifyButton.setOnClickListener {
            val code = codeInput.text.toString().trim()
            if (code.length == 6) {
                progressBar.visibility = View.VISIBLE
                verifyButton.isEnabled = false
                cancelButton.isEnabled = false
                verifyCode(code, dialog, progressBar, verifyButton, cancelButton)
            } else {
                codeInput.error = "Please enter a valid 6-digit code"
            }
        }


        return dialog
    }
    
    private fun verifyCode(
        code: String,
        dialog: Dialog,
        progressBar: ProgressBar,
        verifyButton: Button,
        cancelButton: Button
    ) {
        lifecycleScope.launch {
            try {
                val token =
                    FirebaseAuth.getInstance().currentUser?.getIdToken(false)?.await()?.token
                val success = if (token != null) {
                    val response = RetrofitClient.authApiService.verify2FA(
                        "Bearer $token",
                        mapOf("twoFactorCode" to code)
                    )
                    response.isSuccessful
                } else false

                onVerified(success)
                dialog.dismiss()
            } catch (e: Exception) {
                onVerified(false)
                dialog.dismiss()
            } finally {
                progressBar.visibility = View.GONE
                verifyButton.isEnabled = true
                cancelButton.isEnabled = true
            }
        }
    }

}
