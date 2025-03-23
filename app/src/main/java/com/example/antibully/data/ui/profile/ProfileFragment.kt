package com.example.antibully.data.ui.profile

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
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.squareup.picasso.Picasso
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ProfileFragment : Fragment() {

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore
    private lateinit var userDao: com.example.antibully.data.db.dao.UserDao

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.fragment_profile, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()
        userDao = AppDatabase.getDatabase(requireContext()).userDao()

        val profileImageView = view.findViewById<ImageView>(R.id.ivProfileImage)
        val usernameTextView = view.findViewById<TextView>(R.id.tvUsername)
        val editProfileButton = view.findViewById<Button>(R.id.btnEditProfile)

        val userId = auth.currentUser?.uid ?: return

        // שלב 1: לטעון מ-ROOM
        lifecycleScope.launch(Dispatchers.IO) {
            val localUser: User? = userDao.getUserById(userId)

            withContext(Dispatchers.Main) {
                if (localUser != null) {
                    usernameTextView.text = localUser.name
                    if (localUser.localProfileImagePath.isNotEmpty()) {
                        profileImageView.setImageURI(Uri.parse(localUser.localProfileImagePath))
                    }
                } else {
                    // שלב 2: fallback ל-Firestore אם לא קיים ב-Room
                    db.collection("users").document(userId).get()
                        .addOnSuccessListener { document ->
                            if (document.exists()) {
                                val username = document.getString("fullName") ?: "No Name"
                                val profileImageUrl = document.getString("profileImageUrl") ?: ""

                                usernameTextView.text = username

                                if (profileImageUrl.isNotEmpty()) {
                                    Picasso.get().load(profileImageUrl).into(profileImageView)
                                }
                            }
                        }
                        .addOnFailureListener {
                            usernameTextView.text = "Failed to load profile"
                        }
                }
            }
        }

        editProfileButton.setOnClickListener {
            findNavController().navigate(R.id.action_profileFragment_to_editProfileFragment)
        }
    }
}
