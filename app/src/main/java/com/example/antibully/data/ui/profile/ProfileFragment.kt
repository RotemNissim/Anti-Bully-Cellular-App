package com.example.antibully.data.ui.profile

import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.*
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.antibully.R
import com.example.antibully.data.api.CloudinaryUploader
import com.example.antibully.data.db.AppDatabase
import com.example.antibully.data.models.ChildLocalData
import com.example.antibully.data.models.User
import com.example.antibully.data.repository.ChildRepository
import com.example.antibully.viewmodel.ChildViewModel
import com.example.antibully.viewmodel.ChildViewModelFactory
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.squareup.picasso.Picasso
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collectLatest
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
    private lateinit var childViewModel: ChildViewModel
    private lateinit var childRepository: ChildRepository

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
        val emailTextView = view.findViewById<TextView>(R.id.tvUserEmail)
        val editProfileButton = view.findViewById<FloatingActionButton>(R.id.btnEditProfile)
        val addChildButton = view.findViewById<Button>(R.id.btnAddChild)
        noChildrenText = view.findViewById(R.id.tvNoChildren)
        val settingsButton = view.findViewById<ImageButton>(R.id.btnSettings)
        
        settingsButton.setOnClickListener {
            findNavController().navigate(R.id.action_profileFragment_to_securitySettingsFragment)
        }

        // Initialize child repository and viewmodel
        childRepository = ChildRepository(childDao)
        val factory = ChildViewModelFactory(childRepository)
        childViewModel = ViewModelProvider(this, factory)[ChildViewModel::class.java]

        val userId = auth.currentUser?.uid ?: return
        Log.d("ProfileFragment", "Current user ID: $userId")

        lifecycleScope.launch {
            try {
                // Load user data
                syncUserFromFirestore(userId)
                val user = withContext(Dispatchers.IO) {
                    userDao.getUserById(userId)
                }

                user?.let {
                    usernameTextView.text = it.name
                    usernameTextView.visibility = View.VISIBLE
                    emailTextView.text = it.email
                    emailTextView.visibility = View.VISIBLE
                    profileImageView.visibility = View.VISIBLE
                    editProfileButton?.visibility = View.VISIBLE
                    view.findViewById<ProgressBar>(R.id.profileLoading)?.visibility = View.GONE

                    if (!it.profileImageUrl.isNullOrEmpty()) {
                        Picasso.get().load(it.profileImageUrl).into(profileImageView)
                    } else if (it.localProfileImagePath.isNotEmpty()) {
                        profileImageView.setImageURI(Uri.parse(it.localProfileImagePath))
                    }
                }

                // Get Firebase token and fetch children from backend
                val token = auth.currentUser?.getIdToken(false)?.await()?.token
                Log.d("ProfileFragment", "Firebase token obtained: ${token != null}")
                
                if (token != null) {
                    Log.d("ProfileFragment", "Fetching children from API for user: $userId")
                    childViewModel.fetchChildrenFromApi(token, userId)
                    
                    // ✅ Add a small delay and check again
//                    kotlinx.coroutines.delay(2000)
//
//                    // ✅ Also try to get children from local DB directly
//                    val localChildren = AppDatabase.getDatabase(requireContext()).childDao().getChildrenForUser(userId)
//                    Log.d("ProfileFragment", "Local children count: ${localChildren.size}")
                } else {
                    Log.e("ProfileFragment", "Failed to get Firebase token")
                }

                viewLifecycleOwner.lifecycleScope.launch {
                    viewLifecycleOwner.lifecycle.repeatOnLifecycle(androidx.lifecycle.Lifecycle.State.STARTED) {
                        childViewModel.getChildrenForUser(userId).collectLatest { children ->
                            Log.d("ProfileFragment", "Received ${children.size} children from local DB")
                            recyclerView.layoutManager = LinearLayoutManager(requireContext())
                            recyclerView.adapter = ChildrenAdapter(children)
                            noChildrenText.visibility = if (children.isEmpty()) View.VISIBLE else View.GONE
                        }
                    }
                }

            } catch (e: Exception) {
                Log.e("ProfileFragment", "Error in onViewCreated: ${e.message}", e)
            }
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
        
        // ✅ FOR DEBUGGING - uncomment this line
        debugLocalData()
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

                val cancelButton = dialogView.findViewById<Button>(R.id.cancelButton)
                val deleteButton = dialogView.findViewById<Button>(R.id.deleteButton)

                cancelButton.setOnClickListener { dialog.dismiss() }

                deleteButton.setOnClickListener {
                    dialog.dismiss()
                    deleteChildFromBackend(child)
                }

                dialog.show()
            }
        }

        override fun getItemCount() = children.size
    }

    private fun deleteChildFromBackend(child: ChildLocalData) {
        lifecycleScope.launch {
            val token = auth.currentUser?.getIdToken(false)?.await()?.token
            if (token != null) {
                childViewModel.unlinkChild(token, child.parentUserId, child.childId) { success ->
                    if (success) {
                        Toast.makeText(requireContext(), "Child removed successfully", Toast.LENGTH_SHORT).show()
                        // Children list will be automatically updated through Flow
                    } else {
                        Toast.makeText(requireContext(), "Failed to remove child", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }

    // ✅ Add this debugging method
    private fun debugLocalData() {
        lifecycleScope.launch {
            val userId = auth.currentUser?.uid ?: return@launch
            
            // Check children
            val children = AppDatabase.getDatabase(requireContext()).childDao().getChildrenForUser(userId)
            Log.d("ProfileFragment", "=== LOCAL DATABASE DEBUG ===")
            Log.d("ProfileFragment", "Children count: ${children.size}")
            children.forEach { child ->
                Log.d("ProfileFragment", "Child: ID=${child.childId}, Name=${child.name}")
            }
            
            // Check alerts
            val alerts = AppDatabase.getDatabase(requireContext()).alertDao().getAllAlertsSync()
            Log.d("ProfileFragment", "Total alerts: ${alerts.size}")
            alerts.forEach { alert ->
                Log.d("ProfileFragment", "Alert: postId=${alert.postId}, reporterId=${alert.reporterId}, reason=${alert.reason}")
            }
        }
    }
}