package com.example.antibully.data.ui.post

import android.widget.EditText
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContentProviderCompat.requireContext
import com.example.antibully.data.firestore.FirestoreManager
import com.example.antibully.data.firestore.FirestoreManager.uploadImageToStorage
import com.example.antibully.data.models.Post
import com.google.firebase.firestore.FirebaseFirestore

private val pickImage = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
    uri?.let {
        uploadImageToStorage(it)
    }
}

fun openImagePicker() {
    pickImage.launch("image/*")
}

fun showEditDeleteOptions(post: Post) {
    AlertDialog.Builder(requireContext())
        .setTitle("Manage Post")
        .setItems(arrayOf("Edit", "Delete")) { _, which ->
            when (which) {
                0 -> editPost(post)  // Edit
                1 -> deletePost(post) // Delete
            }
        }
        .show()
}

fun editPost(post: Post) {
    val editText = EditText(requireContext()).apply { setText(post.text) }

    AlertDialog.Builder(requireContext())
        .setTitle("Edit Post")
        .setView(editText)
        .setPositiveButton("Save") { _, _ ->
            val newText = editText.text.toString()
            postViewModel.update(post.copy(text = newText)) // Update in ROOM
            FirestoreManager.updatePostInFirestore(post.id.toString(), newText, post.imageUrl, {}, {}) // Update in Firestore
        }
        .setNegativeButton("Cancel", null)
        .show()
}

fun deletePost(post: Post) {
    postViewModel.delete(post) // Delete from ROOM
    FirebaseFirestore.getInstance().collection("posts").document(post.id.toString()).delete()
}
