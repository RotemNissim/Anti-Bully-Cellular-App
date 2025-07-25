package com.example.antibully.data.ui.auth

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.*
import android.widget.*
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.example.antibully.R
import com.example.antibully.data.db.AppDatabase
import com.example.antibully.data.models.User
import com.example.antibully.data.models.UserApiResponse
import com.example.antibully.utils.SessionManager // ✅ Add this import
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import com.example.antibully.data.api.CloudinaryUploader
import com.squareup.picasso.Picasso
import com.example.antibully.data.api.RetrofitClient

class SignUpFragment : Fragment() {

    private lateinit var auth: FirebaseAuth
    private lateinit var userDao: com.example.antibully.data.db.dao.UserDao
    private var selectedImageUri: Uri? = null
    private var uploadedImageUrl: String? = null
    private lateinit var profileImageView: ImageView
    private lateinit var spinner: ProgressBar

    private val PICK_IMAGE_REQUEST = 1

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = inflater.inflate(R.layout.fragment_sign_up, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        auth = FirebaseAuth.getInstance()
        userDao = AppDatabase.getDatabase(requireContext()).userDao()

        val fullNameInput = view.findViewById<EditText>(R.id.etFullName)
        val emailInput = view.findViewById<EditText>(R.id.etSignUpEmail)
        val passwordInput = view.findViewById<EditText>(R.id.etSignUpPassword)
        val signUpButton = view.findViewById<Button>(R.id.btnRegister)
        profileImageView = view.findViewById(R.id.ivProfileImage)
        val chooseImageButton = view.findViewById<View>(R.id.fabChangePhoto)
        spinner = view.findViewById(R.id.spinnerProfileUpload)

        chooseImageButton.setOnClickListener {
            val intent = Intent(Intent.ACTION_PICK).apply { type = "image/*" }
            startActivityForResult(intent, PICK_IMAGE_REQUEST)
        }

        signUpButton.setOnClickListener {
            val fullName = fullNameInput.text.toString().trim()
            val email = emailInput.text.toString().trim()
            val password = passwordInput.text.toString().trim()

            if (fullName.isEmpty() || email.isEmpty() || password.length < 6) {
                Toast.makeText(requireContext(), "Please fill all fields correctly", Toast.LENGTH_SHORT).show()
            } else {
                spinner.visibility = View.VISIBLE
                registerUser(fullName, email, password, uploadedImageUrl)
            }
        }
        val loginLink = view.findViewById<TextView>(R.id.tvLoginLink)

        loginLink.setOnClickListener {
            findNavController().navigate(R.id.loginFragment)
        }

    }
private fun registerUserToServer(token: String, email: String, fullName: String, profileImageUrl: String?) {
    lifecycleScope.launch {
        try {
            val body = mutableMapOf(
                "email" to email,
                "username" to fullName
            )

            profileImageUrl?.let {
                body["profileImageUrl"] = it
            }

            val response = RetrofitClient.authApiService.registerFirebaseUser("Bearer $token", body)
            if (!response.isSuccessful) {
                Toast.makeText(requireContext(), "Failed to sync user to server", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            Toast.makeText(requireContext(), "Error syncing user to server: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }
}

    private fun registerUser(fullName: String, email: String, password: String, profileImageUrl: String?) {
        auth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val uid = auth.currentUser?.uid ?: return@addOnCompleteListener

                    val userMap = hashMapOf(
                        "fullName" to fullName,
                        "email" to email
                    )
                    profileImageUrl?.let {
                        userMap["profileImageUrl"] = it
                        userMap["localProfileImagePath"] = it
                    }

                    FirebaseFirestore.getInstance().collection("users").document(uid).set(userMap)
                        .addOnSuccessListener {
                            val apiUser = UserApiResponse(id = uid, name = fullName, email = email)
                            val userEntity = User.fromApi(apiUser, profileImageUrl ?: "")

                            lifecycleScope.launch(Dispatchers.IO) {
                                userDao.insertUser(userEntity)
                            }

                            auth.currentUser?.getIdToken(false)?.addOnSuccessListener { result ->
                                val token = result.token ?: return@addOnSuccessListener
                                registerUserToServer(token, email, fullName, profileImageUrl)
                                
                                // ✅ Initialize FCM token after successful registration
                                SessionManager.login(requireContext(), uid)
                            }

                            Toast.makeText(requireContext(), "Welcome, $fullName!", Toast.LENGTH_SHORT).show()
                            findNavController().navigate(R.id.feedFragment)
                        }

                        .addOnFailureListener {
                            Toast.makeText(requireContext(), "Failed to save user data", Toast.LENGTH_SHORT).show()
                        }
                } else {
                    Toast.makeText(requireContext(), "Registration failed: ${task.exception?.message}", Toast.LENGTH_SHORT).show()
                }
            }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == PICK_IMAGE_REQUEST && resultCode == Activity.RESULT_OK && data != null && data.data != null) {
            selectedImageUri = data.data
            profileImageView.setImageURI(selectedImageUri)

            spinner.visibility = View.VISIBLE

            CloudinaryUploader.uploadImage(
                context = requireContext(),
                imageUri = selectedImageUri!!,
                onSuccess = { url ->
                    requireActivity().runOnUiThread {
                        uploadedImageUrl = url
                        spinner.visibility = View.GONE
                        Picasso.get().load(url).into(profileImageView)
                        Toast.makeText(requireContext(), "Image uploaded!", Toast.LENGTH_SHORT).show()
                    }
                },
                onFailure = { error ->
                    requireActivity().runOnUiThread {
                        spinner.visibility = View.GONE
                        Toast.makeText(requireContext(), "Upload failed: ${error.message}", Toast.LENGTH_SHORT).show()
                    }
                }
            )
        }
    }
}