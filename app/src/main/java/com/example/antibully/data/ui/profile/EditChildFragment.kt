package com.example.antibully.data.ui.profile

import android.net.Uri
import android.os.Bundle
import android.view.*
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.example.antibully.R
import com.example.antibully.data.api.CloudinaryUploader
import com.example.antibully.data.db.AppDatabase
import com.example.antibully.data.repository.ChildRepository
import com.example.antibully.viewmodel.ChildViewModel
import com.example.antibully.viewmodel.ChildViewModelFactory
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.squareup.picasso.Picasso
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

class EditChildFragment : Fragment() {

    private lateinit var auth: FirebaseAuth
    private val args: EditChildFragmentArgs by navArgs()

    private var selectedImageUri: Uri? = null
    private var uploadedImageUrl: String? = null

    private lateinit var imageView: ImageView

    private val pickImageLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let {
            selectedImageUri = it
            imageView.setImageURI(it)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = inflater.inflate(R.layout.fragment_edit_child, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        auth = FirebaseAuth.getInstance()

        imageView = view.findViewById(R.id.ivEditChildImage)
        val chooseImageBtn = view.findViewById<FloatingActionButton>(R.id.btnChooseEditImage)
        val idField = view.findViewById<EditText>(R.id.etEditChildId)
        val nameField = view.findViewById<EditText>(R.id.etEditChildName)
        val saveButton = view.findViewById<Button>(R.id.btnSaveEditChild)

        val childId = args.childId
        val parentUserId = auth.currentUser?.uid ?: return

        lifecycleScope.launch(Dispatchers.IO) {
            val dao = AppDatabase.getDatabase(requireContext()).childDao()
            val children = dao.getChildrenForUser(parentUserId)
            val child = children.find { it.childId == childId }

            child?.let {
                uploadedImageUrl = it.imageUrl
                withContext(Dispatchers.Main) {
                    idField.setText(it.childId)
                    nameField.setText(it.name)
                    if (!it.imageUrl.isNullOrEmpty()) {
                        Picasso.get().load(it.imageUrl).into(imageView)
                    }
                }
            }
        }

        chooseImageBtn.setOnClickListener {
            pickImageLauncher.launch("image/*")
        }

        saveButton.setOnClickListener {
            val newName = nameField.text.toString().trim()

            if (newName.isEmpty()) {
                Toast.makeText(requireContext(), "Name is required", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (selectedImageUri != null) {
                CloudinaryUploader.uploadImage(
                    context = requireContext(),
                    imageUri = selectedImageUri!!,
                    onSuccess = { url ->
                        uploadedImageUrl = url
                        updateChild(childId, parentUserId, newName)
                    },
                    onFailure = { e ->
                        Toast.makeText(requireContext(), "Image upload failed: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
                )
            } else {
                updateChild(childId, parentUserId, newName)
            }
        }
    }

    private fun updateChild(childId: String, parentUserId: String, newName: String) {
        lifecycleScope.launch {
            val token = auth.currentUser?.getIdToken(false)?.await()?.token
            if (token != null) {
                // Initialize child repository and viewmodel
                val childDao = AppDatabase.getDatabase(requireContext()).childDao()
                val childRepository = ChildRepository(childDao)
                val factory = ChildViewModelFactory(childRepository)
                val childViewModel = ViewModelProvider(this@EditChildFragment, factory)[ChildViewModel::class.java]
                
                childViewModel.updateChild(token, parentUserId, childId, newName, uploadedImageUrl) { success ->
                    if (success) {
                        Toast.makeText(requireContext(), "Child updated successfully", Toast.LENGTH_SHORT).show()
                        findNavController().navigateUp()
                    } else {
                        Toast.makeText(requireContext(), "Failed to update child", Toast.LENGTH_SHORT).show()
                    }
                }
            } else {
                Toast.makeText(requireContext(), "Authentication required", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
