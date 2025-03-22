package com.example.antibully.data.ui.auth

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.antibully.R
import com.example.antibully.utils.Constants
import com.google.firebase.auth.FirebaseAuth

class SignUpFragment : Fragment() {

    private lateinit var auth: FirebaseAuth

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_sign_up, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        auth = FirebaseAuth.getInstance()

        val emailInput = view.findViewById<EditText>(R.id.etSignUpEmail)
        val passwordInput = view.findViewById<EditText>(R.id.etSignUpPassword)
        val signUpButton = view.findViewById<Button>(R.id.btnRegister)

        signUpButton.setOnClickListener {
            val email = emailInput.text.toString().trim()
            val password = passwordInput.text.toString().trim()

            if (email.isNotEmpty() && password.length >= 6) {
                registerUser(email, password)
            } else {
                Toast.makeText(requireContext(), "Invalid email or password!", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun registerUser(email: String, password: String) {
        auth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    auth.signInWithEmailAndPassword(email, password)
                        .addOnCompleteListener { loginTask ->
                            if (loginTask.isSuccessful) {
                                Toast.makeText(requireContext(), "Welcome!", Toast.LENGTH_SHORT).show()
                                val actionId = Constants.NAV_AFTER_LOGIN_ACTIONS["signup"]
                                if (actionId != null) {
                                    findNavController().navigate(actionId)
                                } else {
                                    Toast.makeText(requireContext(), "Navigation error", Toast.LENGTH_SHORT).show()
                                }
                            } else {
                                Toast.makeText(requireContext(), "Auto-login failed: ${loginTask.exception?.message}", Toast.LENGTH_SHORT).show()
                            }
                        }
                } else {
                    Toast.makeText(requireContext(), "Registration failed: ${task.exception?.message}", Toast.LENGTH_SHORT).show()
                }
            }
    }
}