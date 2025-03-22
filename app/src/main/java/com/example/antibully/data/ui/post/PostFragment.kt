package com.example.antibully.data.ui.post

import android.app.AlertDialog
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ImageView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.ViewModelProvider
import com.example.antibully.data.db.AppDatabase
import com.example.antibully.data.firestore.FirestoreManager
import com.example.antibully.data.models.Post
import com.example.antibully.data.repository.PostRepository
import com.example.antibully.databinding.FragmentPostBinding
import com.example.antibully.viewmodel.PostViewModel
import com.example.antibully.viewmodel.PostViewModelFactory
import com.google.firebase.firestore.FirebaseFirestore
import com.example.antibully.data.db.dao.PostDao

class PostFragment : Fragment() {

    private var _binding: FragmentPostBinding? = null
    private val binding get() = _binding!!

    private lateinit var post: Post

    private lateinit var postViewModel: PostViewModel



    private val pickImage = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let {
            FirestoreManager.uploadImageToStorage(it,
                onSuccess = { imageUrl ->
                    // save post with imageUrl
                },
                onFailure = {
                    // handle error
                }
            )
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentPostBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val postDao = AppDatabase.getDatabase(requireContext()).postDao()
        val firestore = FirebaseFirestore.getInstance()
        val repository = PostRepository(postDao, firestore)
        val factory = PostViewModelFactory(repository)

        postViewModel = ViewModelProvider(this, factory)[PostViewModel::class.java]


    }


    private fun openImagePicker() {
        pickImage.launch("image/*")
    }

    private fun showEditDeleteOptions(post: Post) {
        AlertDialog.Builder(requireContext())
            .setTitle("Manage Post")
            .setItems(arrayOf("Edit", "Delete")) { _, which ->
                when (which) {
                    0 -> editPost(post)
                    1 -> deletePost(post)
                }
            }
            .show()
    }

    private fun editPost(post: Post) {
        val editText = EditText(requireContext()).apply {
            setText(post.text)
        }

        AlertDialog.Builder(requireContext())
            .setTitle("Edit Post")
            .setView(editText)
            .setPositiveButton("Save") { _, _ ->
                val newText = editText.text.toString()
                val updatedPost = post.copy(text = newText)
                postViewModel.update(updatedPost)

                FirestoreManager.updatePostInFirestore(
                    postId = post.id.toString(),
                    newText = newText,
                    newImageUrl = post.imageUrl,
                    onSuccess = {},
                    onFailure = {}
                )
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun deletePost(post: Post) {
        postViewModel.delete(post)
        FirebaseFirestore.getInstance().collection("posts").document(post.id.toString()).delete()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
