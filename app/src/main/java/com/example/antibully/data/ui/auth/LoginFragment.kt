package com.example.antibully.data.ui.auth

import android.os.Bundle
import android.util.Log
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
import com.example.antibully.utils.SessionManager
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class LoginFragment : Fragment() {

    private lateinit var auth: FirebaseAuth
    private lateinit var googleSignInClient: GoogleSignInClient

    private val googleSignInLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            val data = result.data
            if (data == null) {
                Toast.makeText(requireContext(), "Google sign-in cancelled.", Toast.LENGTH_SHORT).show()
                return@registerForActivityResult
            }
            val task = GoogleSignIn.getSignedInAccountFromIntent(data)
            try {
                val account = task.getResult(ApiException::class.java)!!
                val idToken = account.idToken
                if (idToken.isNullOrBlank()) {
                    Toast.makeText(requireContext(), "Missing ID token from Google.", Toast.LENGTH_LONG).show()
                    return@registerForActivityResult
                }
                firebaseAuthWithGoogle(idToken)
            } catch (e: ApiException) {
                // This is where you'll see statusCode = 10 (DEVELOPER_ERROR) if config is wrong
                Log.e("LoginFragment", "Google sign-in failed. code=${e.statusCode}, msg=${e.message}", e)
                Toast.makeText(
                    requireContext(),
                    "Google sign-in failed (code ${e.statusCode}). ${e.message ?: ""}",
                    Toast.LENGTH_LONG
                ).show()
            } catch (e: Exception) {
                Log.e("LoginFragment", "Google sign-in failed (unknown).", e)
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

        // Verify we loaded the correct google-services.json at runtime
        val webClientId = getString(R.string.default_web_client_id)
        Log.d("LoginFragment", "default_web_client_id: $webClientId")

        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(webClientId)
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

    private fun registerUserToServer(token: String, email: String, name: String, imageUrl: String?) {
        lifecycleScope.launch {
            try {
                val body = mutableMapOf(
                    "email" to email,
                    "username" to name
                )
                imageUrl?.takeIf { it.isNotBlank() }?.let { body["profileImageUrl"] = it }

                val resp = com.example.antibully.data.api.AuthRetrofitClient.authService
                    .registerFirebaseUser("Bearer $token", body)

                if (!resp.isSuccessful) {
                    val code = resp.code()
                    val err = resp.errorBody()?.string()
                    Log.e("Register", "Failed: HTTP $code, error=$err")
                    Toast.makeText(requireContext(), "Register failed: $code", Toast.LENGTH_SHORT).show()
                    return@launch
                }

                Toast.makeText(requireContext(), "Registered on server!", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                android.util.Log.e("Register", "Exception", e)
                Toast.makeText(requireContext(), "Register error: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }


    private fun signInWithGoogle() {
        // Optional: clear any cached Google session to avoid stale account/token issues
        googleSignInClient.signOut().addOnCompleteListener {
            val signInIntent = googleSignInClient.signInIntent
            googleSignInLauncher.launch(signInIntent)
        }
    }

    private fun firebaseAuthWithGoogle(idToken: String) {
        val credential = GoogleAuthProvider.getCredential(idToken, null)
        auth.signInWithCredential(credential)
            .addOnCompleteListener(requireActivity()) { task ->
                if (!task.isSuccessful) {
                    Toast.makeText(requireContext(), "Google Sign-In Failed!", Toast.LENGTH_SHORT).show()
                    Log.e("LoginFragment", "Firebase signInWithCredential failed", task.exception)
                    return@addOnCompleteListener
                }

                val firebaseUser = auth.currentUser ?: return@addOnCompleteListener
                val uid = firebaseUser.uid
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

                    firebaseUser.getIdToken(false).addOnSuccessListener { result ->
                        val token = result.token ?: return@addOnSuccessListener
                        // Hit your backend
                        loginWithFirebaseToServer(token)
                        registerUserToServer(token, email, name, profileImageUrl)
                        // Persist session
                        SessionManager.login(requireContext(), uid)
                        // Navigate only after session is set & calls fired
                        Toast.makeText(requireContext(), "Google Sign-In Successful!", Toast.LENGTH_SHORT).show()
                        findNavController().navigate(R.id.feedFragment)
                    }.addOnFailureListener { e ->
                        Toast.makeText(requireContext(), "Failed to get Firebase token: ${e.message}", Toast.LENGTH_SHORT).show()
                    }

                }.addOnFailureListener { e ->
                    Toast.makeText(requireContext(), "Failed to check/create Firestore user: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
    }

    private fun loginUser(email: String, password: String) {
        auth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val firebaseUser = auth.currentUser ?: return@addOnCompleteListener
                    val uid = firebaseUser.uid

                    firebaseUser.getIdToken(false).addOnSuccessListener { result ->
                        val token = result.token ?: return@addOnSuccessListener
                        val emailFromUser = firebaseUser.email ?: ""
                        val name = firebaseUser.displayName ?: emailFromUser.substringBefore("@")
                        val profileImageUrl = firebaseUser.photoUrl?.toString() ?: ""

                        registerUserToServer(token, emailFromUser, name, profileImageUrl)
                        SessionManager.login(requireContext(), uid)

                        Toast.makeText(requireContext(), "Login successful!", Toast.LENGTH_SHORT).show()
                        findNavController().navigate(R.id.feedFragment)
                    }.addOnFailureListener {
                        Toast.makeText(requireContext(), "Failed to get Firebase token.", Toast.LENGTH_SHORT).show()
                    }

                } else {
                    Toast.makeText(requireContext(), "Login failed: ${task.exception?.message}", Toast.LENGTH_SHORT).show()
                }
            }
    }

    private fun loginWithFirebaseToServer(token: String) {
        lifecycleScope.launch {
            try {
                val response = com.example.antibully.data.api.AuthRetrofitClient.authService
                    .loginWithFirebase("Bearer $token")
                if (!response.isSuccessful) {
                    Toast.makeText(requireContext(), "Server login failed", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(requireContext(), "Login error: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
