package com.example.antibully.data.ui.alert

import android.net.Uri
import android.os.Bundle
import android.view.*
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.antibully.data.firestore.FirestoreManager
import com.example.antibully.data.models.Post
import com.example.antibully.databinding.FragmentAlertDetailsBinding
import com.example.antibully.data.ui.adapters.PostAdapter
import com.example.antibully.viewmodel.PostViewModel
import com.example.antibully.viewmodel.PostViewModelFactory
import androidx.activity.result.contract.ActivityResultContracts
import com.example.antibully.data.db.AppDatabase
import com.example.antibully.data.repository.PostRepository
import com.google.firebase.firestore.FirebaseFirestore


class AlertDetailsFragment : Fragment() {

    private var _binding: FragmentAlertDetailsBinding? = null
    private val binding get() = _binding!!

    private lateinit var postViewModel: PostViewModel
    private lateinit var postAdapter: PostAdapter

    private val postDao = AppDatabase.getDatabase(requireContext()).postDao()
    private val postRepository = PostRepository(postDao,  FirebaseFirestore.getInstance())
    private val postFactory = PostViewModelFactory(postRepository)

    private val args: AlertDetailsFragmentArgs by navArgs()
    private val alertId: String by lazy { args.postId }

    private var selectedImageUrl: String? = null

    private val pickImage = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let {
            FirestoreManager.uploadImageToStorage(it,
                onSuccess = { url ->
                    selectedImageUrl = url
                    Toast.makeText(requireContext(), "Image uploaded", Toast.LENGTH_SHORT).show()
                },
                onFailure = { Toast.makeText(requireContext(), "Upload failed", Toast.LENGTH_SHORT).show() }
            )
        }
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

        postAdapter = PostAdapter()
        binding.commentsRecyclerView.layoutManager = LinearLayoutManager(requireContext())
        binding.commentsRecyclerView.adapter = postAdapter

        // Observe posts
        postViewModel.getPostsForAlert(alertId).observe(viewLifecycleOwner) { posts ->
            postAdapter.submitList(posts)
        }

        // Submit comment
        binding.sendCommentButton.setOnClickListener {
            val text = binding.commentInput.text.toString().trim()
            if (text.isNotEmpty()) {
                val post = Post(
                    alertId = alertId,
                    userId = "parentUser", // 🔁 Replace with real user if available
                    text = text,
                    imageUrl = selectedImageUrl
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
}
