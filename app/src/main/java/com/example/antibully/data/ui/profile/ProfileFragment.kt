package com.example.antibully.data.ui.profile

import android.net.Uri
import android.os.Bundle
import android.view.*
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.antibully.R
import com.example.antibully.data.db.AppDatabase
import com.example.antibully.data.models.ChildLocalData
import com.example.antibully.data.models.User
import com.example.antibully.data.ui.common.VerifyTwoFactorDialogFragment
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.squareup.picasso.Picasso
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

class ProfileFragment : Fragment() {

    private lateinit var auth: FirebaseAuth
    private lateinit var userDao: com.example.antibully.data.db.dao.UserDao
    private lateinit var childDao: com.example.antibully.data.db.dao.ChildDao
    private lateinit var recyclerView: RecyclerView
    private lateinit var noChildrenText: TextView
    private lateinit var profileImageView: ImageView
    private lateinit var usernameTextView: TextView
    private var isTwoFactorEnabled: Boolean = false

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = inflater.inflate(R.layout.fragment_profile, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        recyclerView = view.findViewById(R.id.rvChildren)
        recyclerView.isNestedScrollingEnabled = false

        auth = FirebaseAuth.getInstance()
        userDao = AppDatabase.getDatabase(requireContext()).userDao()
        childDao = AppDatabase.getDatabase(requireContext()).childDao()

        profileImageView = view.findViewById(R.id.ivProfileImage)
        usernameTextView = view.findViewById(R.id.tvUsername)
        val editProfileButton = view.findViewById<FloatingActionButton>(R.id.btnEditProfile)
        val addChildButton = view.findViewById<Button>(R.id.btnAddChild)
        noChildrenText = view.findViewById(R.id.tvNoChildren)
        val settingsButton = view.findViewById<ImageButton>(R.id.btnSettings)
        settingsButton.setOnClickListener {
            findNavController().navigate(R.id.action_profileFragment_to_securitySettingsFragment)
        }


        val userId = auth.currentUser?.uid ?: return

        lifecycleScope.launch {
            syncUserFromFirestore(userId)
            loadUserDataAndChildren(userId)
        }
        lifecycleScope.launch {
            checkTwoFactorStatus()
        }

        editProfileButton.setOnClickListener {
            findNavController().navigate(R.id.action_profileFragment_to_editProfileFragment)
        }

        addChildButton.setOnClickListener {
            findNavController().navigate(R.id.action_profileFragment_to_addChildFragment)
        }
    }

    private suspend fun syncUserFromFirestore(userId: String) {
        val db = FirebaseFirestore.getInstance()

        val document = db.collection("users").document(userId).get().await()
        if (document.exists()) {
            val name = document.getString("fullName") ?: ""
            val imagePath = document.getString("localProfileImagePath") ?: ""
            val profileUrl = document.getString("profileImageUrl")

            val user = User(
                id = userId,
                name = name,
                email = auth.currentUser?.email ?: "",
                localProfileImagePath = imagePath,
                profileImageUrl = profileUrl
            )

            withContext(Dispatchers.IO) {
                userDao.insertUser(user)
            }
        }
    }
    private suspend fun checkTwoFactorStatus() {
        try {
            val token = FirebaseAuth.getInstance().currentUser?.getIdToken(false)?.await()?.token
            if (token != null) {
                val response = com.example.antibully.data.api.AuthRetrofitClient.authService.checkTwoFactorStatus("Bearer $token")
                isTwoFactorEnabled = response.twoFactorEnabled
            } else {
                isTwoFactorEnabled = false
            }
        } catch (e: Exception) {
            isTwoFactorEnabled = false
        }
    }

    private suspend fun loadUserDataAndChildren(userId: String) {
        withContext(Dispatchers.IO) {
            val localUser = userDao.getUserById(userId)
            val children = childDao.getChildrenForUser(userId)

            withContext(Dispatchers.Main) {
                localUser?.let {
                    usernameTextView.text = it.name
                    if (!it.profileImageUrl.isNullOrEmpty()) {
                        Picasso.get().load(it.profileImageUrl).into(profileImageView)
                    } else if (it.localProfileImagePath.isNotEmpty()) {
                        profileImageView.setImageURI(Uri.parse(it.localProfileImagePath))
                    }
                }

                recyclerView.layoutManager = LinearLayoutManager(requireContext())
                recyclerView.adapter = ChildrenAdapter(children)
                noChildrenText.visibility = if (children.isEmpty()) View.VISIBLE else View.GONE
            }
        }
    }

    private inner class ChildrenAdapter(private var children: List<ChildLocalData>) :
        RecyclerView.Adapter<ChildrenAdapter.ChildViewHolder>() {

        inner class ChildViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            val childImage: ImageView = itemView.findViewById(R.id.ivChildImage)
            val childIdText: TextView = itemView.findViewById(R.id.tvChildId)
            val childNameText: TextView = itemView.findViewById(R.id.tvChildName)
            val editButton: ImageButton = itemView.findViewById(R.id.btnEditChild)
            val deleteButton: ImageButton = itemView.findViewById(R.id.btnDeleteChild)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ChildViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_child_card, parent, false)
            return ChildViewHolder(view)
        }

        override fun onBindViewHolder(holder: ChildViewHolder, position: Int) {
            val child = children[position]
            holder.childIdText.text = "ID: ${child.childId}"
            holder.childNameText.text = child.name

            if (!child.imageUrl.isNullOrEmpty()) {
                Picasso.get().load(child.imageUrl).into(holder.childImage)
            } else {
                holder.childImage.setImageResource(R.drawable.ic_default_profile)
            }

            holder.editButton.setOnClickListener {
                val action =
                    ProfileFragmentDirections.actionProfileFragmentToEditChildFragment(child.childId)
                findNavController().navigate(action)
            }

            holder.deleteButton.setOnClickListener {
                val dialogView = LayoutInflater.from(requireContext())
                    .inflate(R.layout.dialog_delete_child, null)

                val dialog = AlertDialog.Builder(requireContext())
                    .setView(dialogView)
                    .create()

                // כפתורי הדיאלוג
                val cancelButton = dialogView.findViewById<Button>(R.id.cancelButton)
                val deleteButton = dialogView.findViewById<Button>(R.id.deleteButton)

                cancelButton.setOnClickListener {
                    dialog.dismiss()
                }

                deleteButton.setOnClickListener {
                    dialog.dismiss()

                    if (isTwoFactorEnabled) {
                        // פתיחת דיאלוג אימות דו שלבי
                        val verifyDialog = VerifyTwoFactorDialogFragment { verified ->
                            if (verified) {
                                deleteChild(child)
                            } else {
                                Toast.makeText(
                                    requireContext(),
                                    "Authentication failed",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        }
                        verifyDialog.show(parentFragmentManager, "VerifyTwoFactor")
                    } else {
                        // אם אין אימות – מחיקה ישירה
                        deleteChild(child)
                    }
                }

                dialog.show()
            }
        }
            private fun deleteChild(child: ChildLocalData) {
            lifecycleScope.launch(Dispatchers.IO) {
                childDao.deleteChild(child.childId, child.parentUserId)
                try {
                    val db = FirebaseFirestore.getInstance()
                    db.collection("users")
                        .document(child.parentUserId)
                        .collection("children")
                        .document(child.childId)
                        .delete()
                        .await()
                } catch (e: Exception) {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(requireContext(), "Failed to delete from Firestore", Toast.LENGTH_SHORT).show()
                    }
                }

                val updated = childDao.getChildrenForUser(child.parentUserId)
                withContext(Dispatchers.Main) {
                    updateData(updated)
                    noChildrenText.visibility = if (updated.isEmpty()) View.VISIBLE else View.GONE
                    Toast.makeText(requireContext(), "Child deleted", Toast.LENGTH_SHORT).show()
                }
            }
        }


        override fun getItemCount() = children.size

        fun updateData(newChildren: List<ChildLocalData>) {
            children = newChildren
            notifyDataSetChanged()
        }
    }
}