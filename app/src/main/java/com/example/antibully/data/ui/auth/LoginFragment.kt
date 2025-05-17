package com.example.antibully.data.ui.auth

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
import com.example.antibully.data.models.UserApiResponse
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class LoginFragment : Fragment() {

    private lateinit var auth: FirebaseAuth
    private lateinit var googleSignInClient: GoogleSignInClient

    private val googleSignInLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
        try {
            val account = task.getResult(Exception::class.java)!!
            firebaseAuthWithGoogle(account.idToken!!)
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(requireContext(), "Google sign-in failed: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = inflater.inflate(R.layout.fragment_login, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        auth = FirebaseAuth.getInstance()

        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id))
            .requestEmail()
            .build()

        googleSignInClient = GoogleSignIn.getClient(requireActivity(), gso)

        val emailInput = view.findViewById<EditText>(R.id.etEmail)
        val passwordInput = view.findViewById<EditText>(R.id.etPassword)
        val loginButton = view.findViewById<Button>(R.id.btnLogin)
        val signUpButton = view.findViewById<Button>(R.id.btnSignUp)
        val googleSignInButton = view.findViewById<View>(R.id.btnGoogleLogin)

        loginButton.setOnClickListener {
            val email = emailInput.text.toString().trim()
            val password = passwordInput.text.toString().trim()

            if (email.isNotEmpty() && password.isNotEmpty()) {
                loginUser(email, password)
            } else {
                Toast.makeText(requireContext(), "Please enter email and password", Toast.LENGTH_SHORT).show()
            }
        }

        signUpButton.setOnClickListener {
            findNavController().navigate(R.id.action_loginFragment_to_signUpFragment)
        }

        googleSignInButton.setOnClickListener {
            signInWithGoogle()
        }
    }
    private fun registerUserToServer(token: String, email: String) {
        lifecycleScope.launch {
            try {
                val body = mapOf("email" to email)
                val response = com.example.antibully.data.api.AuthRetrofitClient.authService.registerFirebaseUser(
                    "Bearer $token", body
                )
                if (!response.isSuccessful) {
                    Toast.makeText(requireContext(), "Failed to register user to server", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(requireContext(), "Error registering user to server: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun signInWithGoogle() {
        val signInIntent = googleSignInClient.signInIntent
        googleSignInLauncher.launch(signInIntent)
    }

    private fun firebaseAuthWithGoogle(idToken: String) {
        val credential = GoogleAuthProvider.getCredential(idToken, null)
        auth.signInWithCredential(credential)
            .addOnCompleteListener(requireActivity()) { task ->
                if (task.isSuccessful) {
                    val firebaseUser = auth.currentUser
                    val uid = firebaseUser?.uid ?: return@addOnCompleteListener
                    val name = firebaseUser.displayName ?: "No Name"
                    val email = firebaseUser.email ?: ""
                    val profileImageUrl = firebaseUser.photoUrl?.toString() ?: ""

                    val db = FirebaseFirestore.getInstance()
                    val userDocRef = db.collection("users").document(uid)

                    userDocRef.get().addOnSuccessListener { document ->
                        if (!document.exists()) {
                            val newUser = hashMapOf(
                                "fullName" to name,
                                "email" to email,
                                "profileImageUrl" to profileImageUrl
                            )
                            userDocRef.set(newUser)
                        }

                        val apiUser = UserApiResponse(uid, name, email, profileImageUrl)
                        val userEntity = User.fromApi(apiUser, localImagePath = "")
                        val userDao = AppDatabase.getDatabase(requireContext()).userDao()
                        lifecycleScope.launch(Dispatchers.IO) {
                            userDao.insertUser(userEntity)
                        }

                        Toast.makeText(requireContext(), "Google Sign-In Successful!", Toast.LENGTH_SHORT).show()
                        firebaseUser.getIdToken(false).addOnSuccessListener { result ->
                            val token = result.token ?: return@addOnSuccessListener
                            registerUserToServer(token, email)
                        }

                        findNavController().navigate(R.id.feedFragment)
                    }.addOnFailureListener { e ->
                        Toast.makeText(requireContext(), "Failed to check/create Firestore user: ${e.message}", Toast.LENGTH_SHORT).show()
                    }

                } else {
                    Toast.makeText(requireContext(), "Google Sign-In Failed!", Toast.LENGTH_SHORT).show()
                }
            }
    }

    private fun loginUser(email: String, password: String) {
        auth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val firebaseUser = auth.currentUser
                    firebaseUser?.getIdToken(false)?.addOnSuccessListener { result ->
                        val token = result.token ?: return@addOnSuccessListener
                        val emailFromUser = firebaseUser.email ?: ""
                        registerUserToServer(token, emailFromUser)
                    }

                    Toast.makeText(requireContext(), "Login successful!", Toast.LENGTH_SHORT).show()
                    findNavController().navigate(R.id.feedFragment)
                } else {
                    Toast.makeText(requireContext(), "Login failed: ${task.exception?.message}", Toast.LENGTH_SHORT).show()
                }
            }
    }
}