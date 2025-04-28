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

    // Only your children’s IDs:
    private var childIds: List<String> = emptyList()

    // Adapter––we reuse the same one
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

        // 1) Set up RecyclerView
        adapter = AlertsAdapter(emptyMap()) { alert ->
            val action = FeedFragmentDirections
                .actionFeedFragmentToAlertDetailsFragment(alert.postId)
            findNavController().navigate(action)
        }
        binding.alertsRecyclerView.layoutManager = LinearLayoutManager(requireContext())
        binding.alertsRecyclerView.adapter = adapter

        // 2) Grab your Firebase token
        FirebaseAuth.getInstance().currentUser
            ?.getIdToken(false)
            ?.addOnSuccessListener { result ->
                val token = result.token ?: return@addOnSuccessListener
                startLiveFeed(token)
            }
    }

    private fun startLiveFeed(token: String) {
        // 3) Fetch your children list once:
        val uid = FirebaseAuth.getInstance().currentUser?.uid.orEmpty()
        FirebaseFirestore.getInstance()
            .collection("users")
            .document(uid)
            .collection("children")
            .get()
            .addOnSuccessListener { snap ->
                childIds = snap.documents.map { it.id }

                // a) Observe Room & filter to just your kids
                viewLifecycleOwner.lifecycleScope.launch {
                    viewModel.allAlerts
                        .collectLatest { alerts ->
                            val filtered = alerts.filter { it.reporterId in childIds }
                            adapter.submitList(filtered)
                        }
                }

                // b) Kick off your polling loop
                viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
                    while (isActive) {
                        // fetch fresh from server
                        childIds.forEach { viewModel.fetchAlerts(token, it) }
                        // wait 15s
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
