package com.example.antibully.data.ui.profile

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.*
import android.widget.*
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.example.antibully.R
import com.example.antibully.data.db.AppDatabase
import com.example.antibully.data.db.dao.UserDao
import com.example.antibully.data.models.User
import com.example.antibully.data.models.UserApiResponse
import com.example.antibully.data.api.CloudinaryUploader
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.squareup.picasso.Picasso
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

class EditProfileFragment : Fragment() {

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore
    private lateinit var userDao: UserDao
    private lateinit var profileImageView: ImageView
    private lateinit var spinner: ProgressBar

    private var selectedImageUri: Uri? = null
    private var existingImagePath: String = ""
    private var selectedCloudinaryUrl: String? = null

    private val PICK_IMAGE_REQUEST = 1001

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = inflater.inflate(R.layout.fragment_edit_profile, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()
        userDao = AppDatabase.getDatabase(requireContext()).userDao()

        profileImageView = view.findViewById(R.id.ivProfileImage)
        spinner = view.findViewById(R.id.spinnerProfileUpload)
        val fullNameEditText = view.findViewById<EditText>(R.id.etEditFullName)
        val passwordEditText = view.findViewById<EditText>(R.id.etEditPassword)
        val saveButton = view.findViewById<Button>(R.id.btnSaveProfile)
        val changeImageButton = view.findViewById<View>(R.id.btnChangeProfileImage) // NEW
        val uid = auth.currentUser?.uid ?: return

        spinner.visibility = View.GONE

        lifecycleScope.launch(Dispatchers.IO) {
            val localUser = userDao.getUserById(uid)
            existingImagePath = localUser?.localProfileImagePath ?: ""

            withContext(Dispatchers.Main) {
                if (existingImagePath.isNotEmpty()) {
                    profileImageView.setImageURI(Uri.parse(existingImagePath))
                }
            }
        }

        db.collection("users").document(uid).get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    fullNameEditText.setText(document.getString("fullName") ?: "")
                    val profileImageUrl = document.getString("profileImageUrl")
                    if (!profileImageUrl.isNullOrEmpty() && existingImagePath.isEmpty()) {
                        Picasso.get().load(profileImageUrl).into(profileImageView)
                    }
                }
            }

        changeImageButton.setOnClickListener {
            val intent = Intent(Intent.ACTION_PICK).apply { type = "image/*" }
            startActivityForResult(intent, PICK_IMAGE_REQUEST)
        }

        saveButton.setOnClickListener {
            val newName = fullNameEditText.text.toString().trim()
            val newPassword = passwordEditText.text.toString().trim()
            val uid = auth.currentUser?.uid ?: return@setOnClickListener

            if (newName.isEmpty()) {
                Toast.makeText(requireContext(), "Name can't be empty", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val updates = hashMapOf<String, Any>("fullName" to newName)
            selectedCloudinaryUrl?.let {
                updates["profileImageUrl"] = it
                updates["localProfileImagePath"] = it
            }

            db.collection("users").document(uid).update(updates)
                .addOnSuccessListener {
                    if (newPassword.length >= 6) {
                        auth.currentUser?.updatePassword(newPassword)
                    }

                    val finalImagePath = selectedCloudinaryUrl ?: existingImagePath

                    val apiResponse = UserApiResponse(
                        id = uid,
                        name = newName,
                        email = auth.currentUser?.email ?: "",
                        profilePictureUrl = finalImagePath
                    )

                    val userEntity = User.fromApi(apiResponse, finalImagePath)

                    lifecycleScope.launch(Dispatchers.IO) {
                        userDao.insertUser(userEntity)
                    }

                    // 🔹🔹 ADD THIS BLOCK: call your server, but don't change your UI flow 🔹🔹
                    lifecycleScope.launch {
                        try {
                            Log.d("EditProfile", "-------------------------------------!----------------------------------")
                            Log.d("EditProfile", "HELLO")
                            Log.d("EditProfile", "------------------------------------!!!---------------------------------")
                            // 1) get Firebase ID token (suspend)
                            val token = FirebaseAuth.getInstance().currentUser?.getIdToken(false)?.await()?.token
                            Log.d("EditProfile", "-------------------------------------!----------------------------------")
                            Log.d("EditProfile", "token: $token")
                            Log.d("EditProfile", "------------------------------------!!!---------------------------------")
                            if (token != null) {
                                // 2) call your Retrofit suspend function (off main thread)
                                withContext(Dispatchers.IO) {
                                    Log.d("EditProfile", "-------------------------------------!----------------------------------")
                                    Log.d("EditProfile", "uid: $uid, new name: $newName, profile image url: $finalImagePath")
                                    Log.d("EditProfile", "------------------------------------!!!---------------------------------")
                                    com.example.antibully.data.api.AuthRetrofitClient.authService.editProfileMongo(
                                        "Bearer $token",
                                        com.example.antibully.data.models.EditUserDTO(
                                            userId = uid,
                                            username = newName,
                                            profileImageUrl = finalImagePath
                                        )
                                    )
                                }
                                // Optional: you can inspect response/isSuccessful if you want,
                                // but we intentionally don't block UI or change your current flow.
                            }
                        } catch (e: Exception) {
                            // Optional: log/toast. Keeping silent to avoid changing UX.
                            // Log.e("EditProfile", "Server sync failed: ${e.message}", e)
                        }
                    }
                    // 🔹🔹 END OF ADDED BLOCK 🔹🔹

                    Toast.makeText(requireContext(), "Profile updated!", Toast.LENGTH_SHORT).show()
                    findNavController().navigateUp()
                }
                .addOnFailureListener {
                    Toast.makeText(
                        requireContext(),
                        "Failed to update Firestore",
                        Toast.LENGTH_SHORT
                    ).show()
                }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == PICK_IMAGE_REQUEST && resultCode == Activity.RESULT_OK && data != null && data.data != null) {
            val uri = data.data
            spinner.visibility = View.VISIBLE

            CloudinaryUploader.uploadImage(
                context = requireContext(),
                imageUri = uri!!,
                onSuccess = { url ->
                    activity?.runOnUiThread {
                        if (!isAdded) return@runOnUiThread
                        selectedCloudinaryUrl = url
                        spinner.visibility = View.GONE
                        Picasso.get().load(url).into(profileImageView)
                        Toast.makeText(requireContext(), "Image uploaded!", Toast.LENGTH_SHORT).show()
                    }
                },
                onFailure = { e ->
                    activity?.runOnUiThread {
                        if (!isAdded) return@runOnUiThread
                        spinner.visibility = View.GONE
                        Toast.makeText(requireContext(), "Upload failed: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
                }
            )

        }
    }
}
