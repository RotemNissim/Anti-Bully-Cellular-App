package com.example.antibully.data.ui.feed

import android.os.Bundle
import android.util.Log
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
                binding.welcomeText.visibility = View.VISIBLE
                binding.nameLoading.visibility = View.GONE

                val imageUrl = doc.getString("profileImageUrl")
                if (!imageUrl.isNullOrEmpty()) {
                    Glide.with(requireContext())
                        .load(imageUrl)
                        .circleCrop()
                        .into(binding.profileImage)
                    binding.profileImage.visibility = View.VISIBLE
                } else {
                    binding.profileImage.setImageResource(R.drawable.ic_default_profile)
                }
            }

        // 2. Setup RecyclerView and start everything
        lifecycleScope.launch {
            val childDao = AppDatabase.getDatabase(requireContext()).childDao()
            val children = childDao.getChildrenForUser(currentUserId)

            // ✅ Store child IDs immediately
            childIds = children.map { it.childId }

            Log.d("FeedFragment", "Found ${children.size} children: $childIds")

            // ✅ Create child data map for the adapter
            val childDataMap = children.associateBy { it.childId }

            adapter = AlertsAdapter(childDataMap) { alert ->
                val action = FeedFragmentDirections
                    .actionFeedFragmentToAlertDetailsFragment(alert.postId)
                findNavController().navigate(action)
            }

            binding.alertsRecyclerView.layoutManager = LinearLayoutManager(requireContext())
            binding.alertsRecyclerView.adapter = adapter

            // ✅ Setup live data collection FIRST (this stays active)
            viewModel.allAlerts.collectLatest { alerts ->
                Log.d("FeedFragment", "Received ${alerts.size} total alerts from Room")

                val filtered = alerts.filter { alert ->
                    val isForOurChild = alert.reporterId in childIds
                    if (isForOurChild) {
                        Log.d("FeedFragment", "✅ Alert ${alert.postId} is for our child ${alert.reporterId}")
                    } else {
                        Log.d("FeedFragment", "❌ Alert ${alert.postId} is NOT for our children (reporter: ${alert.reporterId})")
                    }
                    isForOurChild
                }

                Log.d("FeedFragment", "Filtered to ${filtered.size} alerts for our children")
                adapter.submitList(filtered)
            }
        }

        // 3. Start polling separately (after a small delay to ensure collection is active)
        lifecycleScope.launch {
            delay(1000) // Wait for collection to be set up

            FirebaseAuth.getInstance().currentUser
                ?.getIdToken(false)
                ?.addOnSuccessListener { result ->
                    val token = result.token ?: return@addOnSuccessListener
                    startPolling(token)
                }
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

    // ✅ Separate polling function that only handles server fetching
    private fun startPolling(token: String) {
        lifecycleScope.launch(Dispatchers.IO) {
            while (isActive) {
                if (childIds.isNotEmpty()) {
                    Log.d("FeedFragment", "Polling server for children: $childIds")
                    childIds.forEach { childId ->
                        try {
                            Log.d("FeedFragment", "Fetching alerts for child: $childId")
                            viewModel.fetchAlerts(token, childId)
                        } catch (e: Exception) {
                            Log.e("FeedFragment", "Error fetching alerts for $childId: ${e.message}")
                        }
                    }
                } else {
                    Log.w("FeedFragment", "No children found - skipping poll")
                }
                delay(TimeUnit.SECONDS.toMillis(15))
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
