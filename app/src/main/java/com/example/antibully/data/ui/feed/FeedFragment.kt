package com.example.antibully.data.ui.feed

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import com.example.antibully.R
import com.example.antibully.data.db.AppDatabase
import com.example.antibully.data.repository.AlertRepository
import com.example.antibully.data.ui.adapters.AlertsAdapter
import com.example.antibully.databinding.FragmentFeedBinding
import com.example.antibully.viewmodel.AlertViewModel
import com.example.antibully.viewmodel.AlertViewModelFactory
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit

class FeedFragment : Fragment() {

    private var _binding: FragmentFeedBinding? = null
    private val binding get() = _binding!!
    private lateinit var viewModel: AlertViewModel

    private var childIds: List<String> = emptyList()
    private lateinit var adapter: AlertsAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val dao = AppDatabase.getDatabase(requireContext()).alertDao()
        val repo = AlertRepository(dao)
        viewModel = ViewModelProvider(
            this,
            AlertViewModelFactory(repo)
        )[AlertViewModel::class.java]
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentFeedBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // 1. Load profile info (image + name)
        val currentUserId = FirebaseAuth.getInstance().currentUser?.uid.orEmpty()
        FirebaseFirestore.getInstance()
            .collection("users")
            .document(currentUserId)
            .get()
            .addOnSuccessListener { doc ->
                val userName = doc.getString("fullName") ?: "User"
                binding.welcomeText.text = getString(R.string.welcome_user, userName)

                val imageUrl = doc.getString("profileImageUrl")
                if (!imageUrl.isNullOrEmpty()) {
                    Glide.with(requireContext())
                        .load(imageUrl)
                        .circleCrop()
                        .into(binding.profileImage)
                } else {
                    binding.profileImage.setImageResource(R.drawable.ic_default_profile)
                }
            }

        // 2. RecyclerView setup
        adapter = AlertsAdapter(emptyMap()) { alert ->
            val action = FeedFragmentDirections
                .actionFeedFragmentToAlertDetailsFragment(alert.postId)
            findNavController().navigate(action)
        }
        binding.alertsRecyclerView.layoutManager = LinearLayoutManager(requireContext())
        binding.alertsRecyclerView.adapter = adapter

        // 3. Token to fetch alerts
        FirebaseAuth.getInstance().currentUser
            ?.getIdToken(false)
            ?.addOnSuccessListener { result ->
                val token = result.token ?: return@addOnSuccessListener
                startLiveFeed(token)
            }

        // 4. Search input (placeholder)
        binding.searchInput.setOnEditorActionListener { textView, _, _ ->
            val query = textView.text.toString().trim()
            // TODO: implement search logic
            true
        }

        // 5. Filter chips (logic optional - not wired yet)
        binding.reasonToggleGroup.setOnCheckedChangeListener { _, checkedId ->
            val reason = when (checkedId) {
                R.id.btnHarassment -> "Harassment"
                R.id.btnExclusion -> "Social Exclusion"
                R.id.btnHateSpeech -> "Hate Speech"
                R.id.btnCursing -> "Cursing"
                else -> null
            }
            // Future: filter alerts list by reason
        }
    }

    private fun startLiveFeed(token: String) {
        val uid = FirebaseAuth.getInstance().currentUser?.uid.orEmpty()
        FirebaseFirestore.getInstance()
            .collection("users")
            .document(uid)
            .collection("children")
            .get()
            .addOnSuccessListener { snap ->
                childIds = snap.documents.map { it.id }

                // Live updates from Room
                viewLifecycleOwner.lifecycleScope.launch {
                    viewModel.allAlerts.collectLatest { alerts ->
                        val filtered = alerts.filter { it.reporterId in childIds }
                        adapter.submitList(filtered)
                    }
                }

                // Polling from server
                viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
                    while (isActive) {
                        childIds.forEach { viewModel.fetchAlerts(token, it) }
                        delay(TimeUnit.SECONDS.toMillis(15))
                    }
                }
            }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
