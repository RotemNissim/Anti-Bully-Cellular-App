package com.example.antibully.data.ui.profile

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.*
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.example.antibully.R
import com.example.antibully.data.api.CloudinaryUploader
import com.example.antibully.data.db.AppDatabase
import com.example.antibully.data.models.ChildLocalData
import com.example.antibully.data.repository.ChildRepository
import com.example.antibully.viewmodel.ChildViewModel
import com.example.antibully.viewmodel.ChildViewModelFactory
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class AddChildFragment : Fragment() {

    private lateinit var auth: FirebaseAuth
    private var selectedImageUri: Uri? = null
    private var uploadedImageUrl: String? = null

    private lateinit var childImageView: ImageView
    private lateinit var childViewModel: ChildViewModel
    private lateinit var childRepository: ChildRepository

    private var existingChildren: List<ChildLocalData> = emptyList()
    private var parentUserId: String? = null

    private val pickImageLauncher =
        registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
            uri?.let {
                selectedImageUri = it
                childImageView.setImageURI(it)
            }
        }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = inflater.inflate(R.layout.fragment_add_child, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        auth = FirebaseAuth.getInstance()
        parentUserId = auth.currentUser?.uid

        childImageView = view.findViewById(R.id.ivAddChildImage)
        val chooseImageButton = view.findViewById<View>(R.id.btnChooseChildImage)
        val childIdInput = view.findViewById<EditText>(R.id.etChildId)
        val childNameInput = view.findViewById<EditText>(R.id.etChildName)
        val saveButton = view.findViewById<Button>(R.id.btnSaveChild)

        // Initialize child repository and viewmodel
        val childDao = AppDatabase.getDatabase(requireContext()).childDao()
        childRepository = ChildRepository(childDao)
        val factory = ChildViewModelFactory(childRepository)
        childViewModel = ViewModelProvider(this, factory)[ChildViewModel::class.java]

        // Load existing children
        parentUserId?.let { uid ->
            lifecycleScope.launch {
                existingChildren = childViewModel.getChildrenForUser(uid).first()
            }
        }

        chooseImageButton.setOnClickListener {
            pickImageLauncher.launch("image/*")
        }

        saveButton.setOnClickListener {
            val childId = childIdInput.text.toString().trim()
            val childName = childNameInput.text.toString().trim()

            if (childId.isEmpty() || childName.isEmpty()) {
                Toast.makeText(requireContext(), "Please fill all fields", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val childExists = existingChildren.any { it.childId == childId }
            if (childExists) {
                Toast.makeText(requireContext(), "Child with this ID already exists", Toast.LENGTH_LONG).show()
                return@setOnClickListener
            }

            parentUserId?.let { uid ->
                saveChildToBackend(childId, childName, uid)
            } ?: Toast.makeText(requireContext(), "User not logged in", Toast.LENGTH_SHORT).show()
        }

        val connectDiscordButton = view.findViewById<Button>(R.id.btnConnectDiscord)

        connectDiscordButton.setOnClickListener {
            val oauthUrl =
                "https://discord.com/oauth2/authorize?client_id=1373612221166391397&response_type=code&redirect_uri=http%3A%2F%2F10.100.102.35%3A3000%2Fapi%2Foauth%2Fdiscord%2Fcallback&scope=identify"

            android.util.Log.d("DiscordOAuth", "Opening URL: $oauthUrl")

            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(oauthUrl))
            startActivity(intent)
        }
    }

    private fun saveChildToBackend(childId: String, childName: String, parentUserId: String) {
        lifecycleScope.launch {
            val token = auth.currentUser?.getIdToken(false)?.await()?.token
            if (token != null) {
                childViewModel.linkChild(token, parentUserId, childId, childName, uploadedImageUrl) { success ->
                    if (success) {
                        Toast.makeText(requireContext(), "Child added successfully", Toast.LENGTH_SHORT).show()
                        findNavController().navigateUp()
                    } else {
                        Toast.makeText(requireContext(), "Failed to add child", Toast.LENGTH_SHORT).show()
                    }
                }
            } else {
                Toast.makeText(requireContext(), "Authentication required", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
