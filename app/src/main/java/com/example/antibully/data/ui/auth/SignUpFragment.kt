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
import com.google.firebase.firestore.FirebaseFirestore

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

        val fullNameInput = view.findViewById<EditText>(R.id.etFullName)
        val emailInput = view.findViewById<EditText>(R.id.etSignUpEmail)
        val passwordInput = view.findViewById<EditText>(R.id.etSignUpPassword)
        val signUpButton = view.findViewById<Button>(R.id.btnRegister)

        signUpButton.setOnClickListener {
            val fullName = fullNameInput.text.toString().trim()
            val email = emailInput.text.toString().trim()
            val password = passwordInput.text.toString().trim()

            if (fullName.isEmpty() || email.isEmpty() || password.length < 6) {
                Toast.makeText(requireContext(), "Please fill all fields properly", Toast.LENGTH_SHORT).show()
            } else {
                registerUser(fullName, email, password)
            }
        }
    }

    private fun registerUser(fullName: String, email: String, password: String) {
        auth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val uid = auth.currentUser?.uid ?: return@addOnCompleteListener

                    val userMap = hashMapOf(
                        "fullName" to fullName,
                        "email" to email,
                        "profileImageUrl" to "" // בעתיד תוכל לאפשר העלאת תמונה
                    )

                    FirebaseFirestore.getInstance()
                        .collection("users")
                        .document(uid)
                        .set(userMap)
                        .addOnSuccessListener {
                            Toast.makeText(requireContext(), "Welcome, $fullName!", Toast.LENGTH_SHORT).show()
                            val actionId = Constants.NAV_AFTER_LOGIN_ACTIONS["signup"]
                            if (actionId != null) {
                                findNavController().navigate(actionId)
                            } else {
                                Toast.makeText(requireContext(), "Navigation error", Toast.LENGTH_SHORT).show()
                            }
                        }
                        .addOnFailureListener { e ->
                            Toast.makeText(requireContext(), "Failed to save user data: ${e.message}", Toast.LENGTH_SHORT).show()
                        }
                } else {
                    Toast.makeText(requireContext(), "Registration failed: ${task.exception?.message}", Toast.LENGTH_SHORT).show()
                }
            }
    }
}
