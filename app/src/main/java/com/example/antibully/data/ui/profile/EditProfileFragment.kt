package com.example.antibully.data.ui.profile

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.*
import android.widget.*
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.antibully.R
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import com.squareup.picasso.Picasso
import java.util.*

class EditProfileFragment : Fragment() {

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore
    private var selectedImageUri: Uri? = null

    private val PICK_IMAGE_REQUEST = 1001

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = inflater.inflate(R.layout.fragment_edit_profile, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        val profileImageView = view.findViewById<ImageView>(R.id.ivEditProfileImage)
        val fullNameEditText = view.findViewById<EditText>(R.id.etEditFullName)
        val passwordEditText = view.findViewById<EditText>(R.id.etEditPassword)
        val saveButton = view.findViewById<Button>(R.id.btnSaveProfile)

        // Load existing user data
        val uid = auth.currentUser?.uid
        if (uid != null) {
            db.collection("users").document(uid).get()
                .addOnSuccessListener { document ->
                    if (document.exists()) {
                        fullNameEditText.setText(document.getString("fullName"))
                        val profileImageUrl = document.getString("profileImageUrl")
                        if (!profileImageUrl.isNullOrEmpty()) {
                            Picasso.get().load(profileImageUrl).into(profileImageView)
                        }
                    }
                }
        }

        // Select new profile image
        profileImageView.setOnClickListener {
            val intent = Intent(Intent.ACTION_PICK)
            intent.type = "image/*"
            startActivityForResult(intent, PICK_IMAGE_REQUEST)
        }

        // Save changes
        saveButton.setOnClickListener {
            val newFullName = fullNameEditText.text.toString().trim()
            val newPassword = passwordEditText.text.toString().trim()

            if (newFullName.isEmpty()) {
                Toast.makeText(requireContext(), "Name can't be empty", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (selectedImageUri != null) {
                uploadImageAndSave(uid!!, newFullName, newPassword)
            } else {
                updateUserData(uid!!, newFullName, newPassword, null)
            }
        }
    }

    private fun uploadImageAndSave(uid: String, name: String, password: String, ) {
        Log.d("UPLOAD_DEBUG", "Selected image URI: $selectedImageUri")
        val filename = UUID.randomUUID().toString()
        val ref = FirebaseStorage.getInstance().getReference("/profile_images/$filename")
        selectedImageUri?.let { uri ->
            ref.putFile(uri)
                .addOnSuccessListener {
                    ref.downloadUrl.addOnSuccessListener { url ->
                        updateUserData(uid, name, password, url.toString())
                    }
                }
                .addOnFailureListener {
                    Toast.makeText(requireContext(), "Image upload failed", Toast.LENGTH_SHORT).show()
                }
        }
    }

    private fun updateUserData(uid: String, name: String, password: String, imageUrl: String?) {
        val updates = hashMapOf<String, Any>("fullName" to name)
        if (!imageUrl.isNullOrEmpty()) {
            updates["profileImageUrl"] = imageUrl
        }

        db.collection("users").document(uid).update(updates)
            .addOnSuccessListener {
                if (password.length >= 6) {
                    auth.currentUser?.updatePassword(password)
                        ?.addOnSuccessListener {
                            Toast.makeText(requireContext(), "Profile & password updated", Toast.LENGTH_SHORT).show()
                        }
                        ?.addOnFailureListener {
                            Toast.makeText(requireContext(), "Profile updated, but password failed", Toast.LENGTH_SHORT).show()
                        }
                } else {
                    Toast.makeText(requireContext(), "Profile updated!", Toast.LENGTH_SHORT).show()
                }

                findNavController().navigateUp()
            }
            .addOnFailureListener {
                Toast.makeText(requireContext(), "Failed to update profile", Toast.LENGTH_SHORT).show()
            }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == PICK_IMAGE_REQUEST && resultCode == Activity.RESULT_OK && data != null && data.data != null) {
            selectedImageUri = data.data
            view?.findViewById<ImageView>(R.id.ivEditProfileImage)?.setImageURI(selectedImageUri)
        }
    }
}
