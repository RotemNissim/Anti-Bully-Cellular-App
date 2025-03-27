package com.example.antibully.data.ui.alert

import android.net.Uri
import android.os.Bundle
import android.view.*
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.antibully.R
import com.example.antibully.data.db.AppDatabase
import com.example.antibully.data.firestore.FirestoreManager
import com.example.antibully.data.firestore.FirestoreManager.fetchAllUsers
import com.example.antibully.data.models.Post
import com.example.antibully.data.repository.PostRepository
import com.example.antibully.data.repository.AlertRepository
import com.example.antibully.databinding.FragmentAlertsBinding
import com.example.antibully.databinding.FragmentAlertDetailsBinding
import com.example.antibully.data.ui.adapters.PostAdapter
import com.example.antibully.viewmodel.PostViewModel
import com.example.antibully.viewmodel.PostViewModelFactory
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.squareup.picasso.Picasso
import java.util.Date

class AlertDetailsFragment : Fragment() {

    private var _binding: FragmentAlertDetailsBinding? = null
    private val binding get() = _binding!!

    private lateinit var postViewModel: PostViewModel
    private lateinit var postAdapter: PostAdapter
    private lateinit var postFactory: PostViewModelFactory

    private val args: AlertDetailsFragmentArgs by navArgs()
    private val alertId: String by lazy { args.postId }

    private var selectedImageUrl: String? = null

    private val pickImage = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let {
            FirestoreManager.uploadImageToStorage(
                imageUri = it,
                onSuccess = { url ->
                    selectedImageUrl = url
                    Toast.makeText(requireContext(), "Image uploaded", Toast.LENGTH_SHORT).show()
                },
                onFailure = {
                    Toast.makeText(requireContext(), "Upload failed", Toast.LENGTH_SHORT).show()
                }
            )
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // ✅ Safe to access context here
        val postDao = AppDatabase.getDatabase(requireContext()).postDao()
        val postRepository = PostRepository(postDao, FirebaseFirestore.getInstance())
        postFactory = PostViewModelFactory(postRepository)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAlertDetailsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {

        postViewModel = ViewModelProvider(this, postFactory)[PostViewModel::class.java]
        postViewModel.syncPostsFromFirestore(alertId)
        val currentUserId = FirebaseAuth.getInstance().currentUser?.uid.orEmpty()

        postAdapter = PostAdapter(
            currentUserId = currentUserId,
            onEditClick = { post -> showEditDialog(post) },
            onDeleteClick = { post -> postViewModel.delete(post) }
        )
        binding.commentsRecyclerView.layoutManager = LinearLayoutManager(requireContext())
        binding.commentsRecyclerView.adapter = postAdapter

        val alertDao = AppDatabase.getDatabase(requireContext()).alertDao()

        alertDao.getAlertByPostId(alertId).observe(viewLifecycleOwner) { alert ->
            alert?.let {
                binding.alertText.text = it.text
                binding.alertReason.text = it.reason
                binding.alertReporterId.text = it.reporterId
                binding.alertTimestamp.text = Date(it.timestamp).toString()
                it.imageUrl?.let { url ->
                    Picasso.get().load(url).into(binding.alertImage)
                }
            }
        }

        // Observe posts
        postViewModel.getPostsForAlert(alertId).observe(viewLifecycleOwner) { posts ->
            fetchAllUsers { userMap ->

                postAdapter = PostAdapter(
                    userMap = userMap,
                    currentUserId = currentUserId,
                    onEditClick = { post -> showEditDialog(post) },
                    onDeleteClick = { post -> postViewModel.delete(post) }
                )
                binding.commentsRecyclerView.adapter = postAdapter
                postAdapter.submitList(posts) // ← this is your post list
            }
        }

        // Submit comment
        binding.sendCommentButton.setOnClickListener {
            val generatedFirebaseId = FirebaseFirestore.getInstance().collection("posts").document().id
            val text = binding.commentInput.text.toString().trim()
            if (text.isNotEmpty()) {
                val post = Post(
                    firebaseId = generatedFirebaseId,
                    alertId = alertId,
                    userId = FirebaseAuth.getInstance().currentUser?.uid ?: "unknownUser", // 🛠️ Replace with actual user logic later
                    text = text,
                    imageUrl = selectedImageUrl,
                    timestamp = System.currentTimeMillis()

                )
                postViewModel.insert(post)
                binding.commentInput.setText("")
                selectedImageUrl = null
            }
        }

        // Pick image
        binding.commentImagePicker.setOnClickListener {
            pickImage.launch("image/*")
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
    private fun showEditDialog(post: Post) {
        val editText = EditText(requireContext()).apply {
            setText(post.text)
        }

        AlertDialog.Builder(requireContext())
            .setTitle("Edit Comment")
            .setView(editText)
            .setPositiveButton("Save") { _, _ ->
                val newText = editText.text.toString()
                val updatedPost = post.copy(text = newText)
                postViewModel.update(updatedPost)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

}
