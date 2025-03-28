package com.example.antibully.data.ui.profile

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.*
import android.widget.*
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.example.antibully.R
import com.example.antibully.data.db.AppDatabase
import com.example.antibully.data.models.ChildLocalData
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class EditChildFragment : Fragment() {

    private lateinit var auth: FirebaseAuth
    private val args: EditChildFragmentArgs by navArgs()

    private var selectedImageUri: Uri? = null
    private val PICK_IMAGE_REQUEST = 2001

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = inflater.inflate(R.layout.fragment_edit_child, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        auth = FirebaseAuth.getInstance()

        val imageView = view.findViewById<ImageView>(R.id.ivEditChildImage)
        val chooseImageBtn = view.findViewById<FloatingActionButton>(R.id.btnChooseEditImage)
        val idField = view.findViewById<EditText>(R.id.etEditChildId)
        val nameField = view.findViewById<EditText>(R.id.etEditChildName)
        val saveButton = view.findViewById<Button>(R.id.btnSaveEditChild)

        val childId = args.childId
        val parentUserId = auth.currentUser?.uid ?: return

        // Load existing child data
        lifecycleScope.launch(Dispatchers.IO) {
            val dao = AppDatabase.getDatabase(requireContext()).childDao()
            val children = dao.getChildrenForUser(parentUserId)
            val child = children.find { it.childId == childId }

            child?.let {
                withContext(Dispatchers.Main) {
                    idField.setText(it.childId)
                    nameField.setText(it.name)
                    if (it.localImagePath.isNotEmpty()) {
                        selectedImageUri = Uri.parse(it.localImagePath)
                        imageView.setImageURI(selectedImageUri)
                    }
                }
            }
        }

        chooseImageBtn.setOnClickListener {
            val intent = Intent(Intent.ACTION_PICK).apply { type = "image/*" }
            startActivityForResult(intent, PICK_IMAGE_REQUEST)
        }

        saveButton.setOnClickListener {
            val newName = nameField.text.toString().trim()
            val imagePath = selectedImageUri?.toString() ?: ""

            if (newName.isEmpty()) {
                Toast.makeText(requireContext(), "Name is required", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            lifecycleScope.launch(Dispatchers.IO) {
                val dao = AppDatabase.getDatabase(requireContext()).childDao()
                dao.updateChild(childId, parentUserId, newName, imagePath)

                // 🔥 Update name in Firestore
                val updateMap = mapOf("name" to newName)
                FirebaseFirestore.getInstance()
                    .collection("users")
                    .document(parentUserId)
                    .collection("children")
                    .document(childId)
                    .update(updateMap)

                withContext(Dispatchers.Main) {
                    Toast.makeText(requireContext(), "Child updated", Toast.LENGTH_SHORT).show()
                    findNavController().navigateUp()
                }
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == PICK_IMAGE_REQUEST && resultCode == Activity.RESULT_OK && data?.data != null) {
            selectedImageUri = data.data
            requireContext().contentResolver.takePersistableUriPermission(
                selectedImageUri!!,
                Intent.FLAG_GRANT_READ_URI_PERMISSION
            )
            view?.findViewById<ImageView>(R.id.ivEditChildImage)?.setImageURI(selectedImageUri)
        }
    }
}