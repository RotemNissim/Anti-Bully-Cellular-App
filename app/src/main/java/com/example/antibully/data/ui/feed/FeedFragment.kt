package com.example.antibully.data.ui.feed

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
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
import kotlinx.coroutines.tasks.await
import java.util.concurrent.TimeUnit
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.activityViewModels
import com.example.antibully.data.ui.common.SwipeToDelete

class FeedFragment : Fragment() {

    private var _binding: FragmentFeedBinding? = null
    private val binding get() = _binding!!
    private val viewModel: AlertViewModel by activityViewModels {
        val db = AppDatabase.getDatabase(requireContext())
        val currentUserId = FirebaseAuth.getInstance().currentUser?.uid.orEmpty()
        AlertViewModelFactory(
            AlertRepository(
                db.alertDao(),
                db.dismissedAlertDao(),
                currentUserId
            )
        )
    }

    private var childIds: List<String> = emptyList()
    private lateinit var adapter: AlertsAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
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

        val currentUserId = FirebaseAuth.getInstance().currentUser?.uid.orEmpty()
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val doc = FirebaseFirestore.getInstance()
                    .collection("users")
                    .document(currentUserId)
                    .get()
                    .await()

                val b = _binding ?: return@launch
                val userName = doc.getString("fullName") ?: "User"
                b.welcomeText.text = getString(R.string.welcome_user, userName)
                b.welcomeText.visibility = View.VISIBLE
                b.nameLoading.visibility = View.GONE

                val imageUrl = doc.getString("profileImageUrl")
                if (!imageUrl.isNullOrEmpty()) {
                    Glide.with(b.root)
                        .load(imageUrl)
                        .centerCrop()
                        .circleCrop()
                        .into(b.profileImage)
                    b.profileImage.visibility = View.VISIBLE
                } else {
                    b.profileImage.setImageResource(R.drawable.ic_default_profile)
                }
            } catch (e: Exception) {
                _binding?.nameLoading?.visibility = View.GONE
                Log.e("FeedFragment", "Failed to load user doc: ${e.message}")
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            val childDao = AppDatabase.getDatabase(requireContext()).childDao()
            val children = childDao.getChildrenForUser(currentUserId)

            childIds = children.map { it.childId }
            viewModel.setChildIds(children)
            val childDataMap = children.associateBy { it.childId }

            adapter = AlertsAdapter(
                childDataMap = childDataMap,
                onAlertClick = { alert ->
                    val action = FeedFragmentDirections
                        .actionFeedFragmentToAlertDetailsFragment(alert.postId)
                    findNavController().navigate(action)
                },
                onUnreadGroupClick = { childId ->
                    val childName = childDataMap[childId]?.name.orEmpty()
                    val action = FeedFragmentDirections
                        .actionFeedFragmentToUnreadListFragment(
                            childId = childId,
                            childName = childName
                        )
                    findNavController().navigate(action)
                }
            )

            binding.alertsRecyclerView.layoutManager = LinearLayoutManager(requireContext())
            binding.alertsRecyclerView.adapter = adapter

            SwipeToDelete.attach(binding.alertsRecyclerView, adapter) { alert ->
                viewModel.delete(alert)
            }

            val backStackEntry = findNavController().getBackStackEntry(R.id.feedFragment)
            backStackEntry.savedStateHandle
                .getLiveData<Boolean>("refresh_feed")
                .observe(viewLifecycleOwner) { shouldRefresh ->
                    if (shouldRefresh == true) {
                        backStackEntry.savedStateHandle.set("refresh_feed", false)
                    }
                }

            FirebaseAuth.getInstance().currentUser?.getIdToken(false)
                ?.await()?.token?.let { token ->
                viewModel.refreshLastSeen(token)
                viewModel.loadLastSeenForChildren(token)
                children.forEach { child -> viewModel.fetchAlerts(token, child.childId) }
            } ?: Log.e("FeedFragment", "Failed to get Firebase token")

            viewLifecycleOwner.lifecycleScope.launch {
                viewModel.rows.collectLatest { items ->
                    adapter.submitList(items)
                    if (items.isEmpty()) {
                        binding.alertsRecyclerView.visibility = View.GONE
                        binding.emptyStateText.visibility = View.VISIBLE
                    } else {
                        binding.alertsRecyclerView.visibility = View.VISIBLE
                        binding.emptyStateText.visibility = View.GONE
                    }
                }
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            delay(1000)
            FirebaseAuth.getInstance().currentUser?.getIdToken(false)?.await()?.token?.let {
                startPolling(it)
            }
        }

        binding.searchInput.setOnEditorActionListener { tv, _, _ ->
            viewModel.setSearchQuery(tv.text.toString().trim())
            true
        }
        binding.searchInput.doAfterTextChanged { text ->
            viewModel.setSearchQuery(text?.toString().orEmpty())
        }
        binding.reasonToggleGroup.setOnCheckedChangeListener { _, checkedId ->
            val category: String? = when (checkedId) {
                R.id.btnHarassment -> "Harassment"
                R.id.btnExclusion -> "Social Exclusion"
                R.id.btnHateSpeech -> "Hate Speech"
                R.id.btnCursing -> "Cursing"
                R.id.btnAll -> null
                else -> null
            }
            viewModel.setCategory(category)
        }
    }

    private fun startPolling(token: String) {
        viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
            while (isActive) {
                if (childIds.isNotEmpty()) {
                    Log.d("FeedFragment", "Polling server for children: $childIds")
                    childIds.forEach { id ->
                        try {
                            viewModel.fetchAlerts(token, id)
                        } catch (e: Exception) {
                            Log.e("FeedFragment", "Error fetching $id: ${e.message}")
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
