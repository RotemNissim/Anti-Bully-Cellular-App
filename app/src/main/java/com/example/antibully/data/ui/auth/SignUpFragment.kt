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
import com.example.antibully.utils.Constants
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class SignUpFragment : Fragment() {

    private lateinit var auth: FirebaseAuth
    private lateinit var userDao: com.example.antibully.data.db.dao.UserDao
    private var selectedImageUri: Uri? = null

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
        val profileImageView = view.findViewById<ImageView>(R.id.ivProfileImage)
        val chooseImageButton = view.findViewById<Button>(R.id.btnChangeProfileImage)

        // Open gallery when "Choose Image" button is clicked
        chooseImageButton.setOnClickListener {
            val intent = Intent(Intent.ACTION_PICK).apply { type = "image/*" }
            startActivityForResult(intent, PICK_IMAGE_REQUEST)
        }

        signUpButton.setOnClickListener {
            val fullName = fullNameInput.text.toString().trim()
            val email = emailInput.text.toString().trim()
            val password = passwordInput.text.toString().trim()

            if (fullName.isEmpty() || email.isEmpty() || password.length < 6 ) {
                Toast.makeText(requireContext(), "Fill all fields & select image", Toast.LENGTH_SHORT).show()
            } else {
                registerUserLocallyAndRemotely(fullName, email, password)
            }
        }
    }

    private fun registerUserLocallyAndRemotely(fullName: String, email: String, password: String) {
        auth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val uid = auth.currentUser?.uid ?: return@addOnCompleteListener

                    val userMap = hashMapOf(
                        "fullName" to fullName,
                        "email" to email
                    )

                    FirebaseFirestore.getInstance().collection("users").document(uid).set(userMap)
                        .addOnSuccessListener {
                            val apiUser = UserApiResponse(id = uid, name = fullName, email = email)
                            val userEntity = User.fromApi(apiUser, selectedImageUri.toString())

                            lifecycleScope.launch(Dispatchers.IO) {
                                userDao.insertUser(userEntity)
                            }

                            Toast.makeText(requireContext(), "Welcome, $fullName!", Toast.LENGTH_SHORT).show()
                            Constants.NAV_AFTER_LOGIN_ACTIONS["signup"]?.let {
                                findNavController().navigate(it)
                            }
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
            view?.findViewById<ImageView>(R.id.ivProfileImage)?.setImageURI(selectedImageUri)
        }
    }
}
