package com.example.antibully.data.ui.profile

import android.net.Uri
import android.os.Bundle
import android.view.*
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.example.antibully.R
import com.example.antibully.data.api.CloudinaryUploader
import com.example.antibully.data.db.AppDatabase
import com.example.antibully.data.models.ChildLocalData
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class AddChildFragment : Fragment() {

    private lateinit var auth: FirebaseAuth
    private var selectedImageUri: Uri? = null
    private var uploadedImageUrl: String? = null

    private lateinit var childImageView: ImageView

    private val pickImageLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
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

        childImageView = view.findViewById(R.id.ivAddChildImage)
        val chooseImageButton = view.findViewById<View>(R.id.btnChooseChildImage)
        val childIdInput = view.findViewById<EditText>(R.id.etChildId)
        val childNameInput = view.findViewById<EditText>(R.id.etChildName)
        val saveButton = view.findViewById<Button>(R.id.btnSaveChild)

        chooseImageButton.setOnClickListener {
            pickImageLauncher.launch("image/*")
        }

        saveButton.setOnClickListener {
            val childId = childIdInput.text.toString().trim()
            val childName = childNameInput.text.toString().trim()
            val parentUserId = auth.currentUser?.uid ?: return@setOnClickListener

            if (childId.isEmpty() || childName.isEmpty()) {
                Toast.makeText(requireContext(), "Please fill all fields", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (selectedImageUri != null && uploadedImageUrl == null) {
                CloudinaryUploader.uploadImage(
                    context = requireContext(),
                    imageUri = selectedImageUri!!,
                    onSuccess = { url ->
                        uploadedImageUrl = url
                        saveChild(childId, childName, parentUserId)
                    },
                    onFailure = { e ->
                        Toast.makeText(requireContext(), "Image upload failed: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
                )
            } else {
                saveChild(childId, childName, parentUserId)
            }
        }
    }

    private fun saveChild(childId: String, childName: String, parentUserId: String) {
        lifecycleScope.launch(Dispatchers.IO) {
            val dao = AppDatabase.getDatabase(requireContext()).childDao()
            val child = ChildLocalData(childId, parentUserId, childName, uploadedImageUrl ?: "")
            dao.insertChild(child)

            FirebaseFirestore.getInstance()
                .collection("users")
                .document(parentUserId)
                .collection("children")
                .document(childId)
                .set(mapOf("name" to childName))

            withContext(Dispatchers.Main) {
                Toast.makeText(requireContext(), "Child added successfully", Toast.LENGTH_SHORT).show()
                findNavController().navigateUp()
            }
        }
    }
}
