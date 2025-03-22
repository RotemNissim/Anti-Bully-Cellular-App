package com.example.antibully.data.ui.profile

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.antibully.R
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.squareup.picasso.Picasso

class ProfileFragment : Fragment() {

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore

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

        val profileImageView = view.findViewById<ImageView>(R.id.ivProfileImage)
        val usernameTextView = view.findViewById<TextView>(R.id.tvUsername)
        val editProfileButton = view.findViewById<Button>(R.id.btnEditProfile)

        val userId = auth.currentUser?.uid
        if (userId != null) {
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

        editProfileButton.setOnClickListener {
            findNavController().navigate(R.id.action_profileFragment_to_editProfileFragment)
        }
    }
}
