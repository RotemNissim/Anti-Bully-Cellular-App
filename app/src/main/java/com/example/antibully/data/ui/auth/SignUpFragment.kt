package com.example.antibully.data.ui.auth

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.*
import android.widget.*
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.antibully.R
import com.example.antibully.utils.Constants
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import java.util.*

class SignUpFragment : Fragment() {

    private lateinit var auth: FirebaseAuth
    private var selectedImageUri: Uri? = null

    private val PICK_IMAGE_REQUEST = 1

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = inflater.inflate(R.layout.fragment_sign_up, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        auth = FirebaseAuth.getInstance()

        val fullNameInput = view.findViewById<EditText>(R.id.etFullName)
        val emailInput = view.findViewById<EditText>(R.id.etSignUpEmail)
        val passwordInput = view.findViewById<EditText>(R.id.etSignUpPassword)
        val signUpButton = view.findViewById<Button>(R.id.btnRegister)
        val profileImageView = view.findViewById<ImageView>(R.id.ivProfileImage)

        profileImageView.setOnClickListener {
            val intent = Intent(Intent.ACTION_PICK)
            intent.type = "image/*"
            startActivityForResult(intent, PICK_IMAGE_REQUEST)
        }

        signUpButton.setOnClickListener {
            val fullName = fullNameInput.text.toString().trim()
            val email = emailInput.text.toString().trim()
            val password = passwordInput.text.toString().trim()

            if (fullName.isEmpty() || email.isEmpty() || password.length < 6 || selectedImageUri == null) {
                Toast.makeText(requireContext(), "Fill all fields & select image", Toast.LENGTH_SHORT).show()
            } else {
                uploadProfileImageAndRegister(fullName, email, password)
            }
        }
    }

    private fun uploadProfileImageAndRegister(fullName: String, email: String, password: String) {
        val filename = UUID.randomUUID().toString()
        val storageRef = FirebaseStorage.getInstance().getReference("/profile_images/$filename")

        selectedImageUri?.let { uri ->
            storageRef.putFile(uri)
                .addOnSuccessListener {
                    storageRef.downloadUrl.addOnSuccessListener { imageUrl ->
                        registerUser(fullName, email, password, imageUrl.toString())
                    }
                }
                .addOnFailureListener {
                    Toast.makeText(requireContext(), "Image upload failed: ${it.message}", Toast.LENGTH_SHORT).show()
                }
        }
    }

    private fun registerUser(fullName: String, email: String, password: String, profileImageUrl: String) {
        auth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val uid = auth.currentUser?.uid ?: return@addOnCompleteListener
                    val userMap = hashMapOf(
                        "fullName" to fullName,
                        "email" to email,
                        "profileImageUrl" to profileImageUrl
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
                        .addOnFailureListener {
                            Toast.makeText(requireContext(), "Failed to save user data: ${it.message}", Toast.LENGTH_SHORT).show()
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
