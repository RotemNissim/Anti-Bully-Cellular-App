package com.example.antibully.data.ui.profile

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.example.antibully.R
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
    private val PICK_IMAGE_REQUEST = 1002

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = inflater.inflate(R.layout.fragment_add_child, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        auth = FirebaseAuth.getInstance()

        val childImage = view.findViewById<ImageView>(R.id.ivAddChildImage)
        val chooseImageButton = view.findViewById<View>(R.id.btnChooseChildImage)
        val childIdInput = view.findViewById<EditText>(R.id.etChildId)
        val childNameInput = view.findViewById<EditText>(R.id.etChildName)
        val saveButton = view.findViewById<Button>(R.id.btnSaveChild)

        chooseImageButton.setOnClickListener {
            val intent = Intent(Intent.ACTION_PICK).apply { type = "image/*" }
            startActivityForResult(intent, PICK_IMAGE_REQUEST)
        }

        saveButton.setOnClickListener {
            val childId = childIdInput.text.toString().trim()
            val childName = childNameInput.text.toString().trim()
            val parentUserId = auth.currentUser?.uid ?: return@setOnClickListener

            if (childId.isEmpty() || childName.isEmpty()) {
                Toast.makeText(requireContext(), "Please fill all fields", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val imagePath = selectedImageUri?.toString() ?: ""
            val child = ChildLocalData(
                childId = childId,
                parentUserId = parentUserId,
                name = childName,
                localImagePath = imagePath
            )

            lifecycleScope.launch(Dispatchers.IO) {
                val dao = AppDatabase.getDatabase(requireContext()).childDao()
                dao.insertChild(child)

                // ✅ Save to Firestore
                val firestore = FirebaseFirestore.getInstance()
                val childMap = mapOf("name" to childName)

                firestore.collection("users")
                    .document(parentUserId)
                    .collection("children")
                    .document(childId)
                    .set(childMap)

                withContext(Dispatchers.Main) {
                    Toast.makeText(requireContext(), "Child added successfully", Toast.LENGTH_SHORT).show()
                    findNavController().navigateUp()
                }
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == PICK_IMAGE_REQUEST && resultCode == Activity.RESULT_OK && data != null && data.data != null) {
            selectedImageUri = data.data
            requireContext().contentResolver.takePersistableUriPermission(
                selectedImageUri!!,
                Intent.FLAG_GRANT_READ_URI_PERMISSION
            )
            view?.findViewById<ImageView>(R.id.ivAddChildImage)?.setImageURI(selectedImageUri)
        }
    }
}
