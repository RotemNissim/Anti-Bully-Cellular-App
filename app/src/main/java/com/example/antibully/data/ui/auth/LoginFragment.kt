package com.example.antibully.data.ui.auth

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.example.antibully.R
import com.example.antibully.data.db.AppDatabase
import com.example.antibully.data.models.User
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch

class LoginFragment : Fragment() {

    private lateinit var emailInput: EditText
    private lateinit var passwordInput: EditText
    private lateinit var loginButton: Button

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_login, container, false)
        emailInput = view.findViewById(R.id.etEmail)
        passwordInput = view.findViewById(R.id.etPassword)
        loginButton = view.findViewById(R.id.btnLogin)
        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        loginButton.setOnClickListener {
            val email = emailInput.text.toString().trim()
            val pass  = passwordInput.text.toString().trim()

            FirebaseAuth.getInstance()
                .signInWithEmailAndPassword(email, pass)
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        val firebaseUser = FirebaseAuth.getInstance().currentUser
                        firebaseUser?.getIdToken(false)
                            ?.addOnSuccessListener { result ->
                                val token = result.token ?: return@addOnSuccessListener

                                // ← NEW: save token for all future API calls
                                requireContext()
                                    .getSharedPreferences("auth", Context.MODE_PRIVATE)
                                    .edit()
                                    .putString("auth_token", token)
                                    .apply()

                                // now register this user with your backend
                                registerUserToServer(token, firebaseUser.email ?: "")
                            }

                        Toast.makeText(requireContext(),
                            "Login successful!", Toast.LENGTH_SHORT).show()
                        findNavController().navigate(R.id.feedFragment)

                    } else {
                        Toast.makeText(requireContext(),
                            "Login failed: ${task.exception?.message}",
                            Toast.LENGTH_SHORT).show()
                    }
                }
        }
    }

    private fun registerUserToServer(token: String, email: String) {
        lifecycleScope.launch {
            try {
                val body = mapOf("email" to email)
                val response = com.example.antibully.data.api.AuthRetrofitClient
                    .authService
                    .registerFirebaseUser("Bearer $token", body)

                if (!response.isSuccessful) {
                    Toast.makeText(requireContext(),
                        "Failed to register user to server",
                        Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(requireContext(),
                    "Server error: ${e.localizedMessage}",
                    Toast.LENGTH_SHORT).show()
            }
        }
    }
}
