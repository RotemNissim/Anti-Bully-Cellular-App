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
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class FeedFragment : Fragment() {

    private var _binding: FragmentFeedBinding? = null
    private val binding get() = _binding!!
    private lateinit var viewModel: AlertViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val alertDao = AppDatabase.getDatabase(requireContext()).alertDao()
        val repository = AlertRepository(alertDao)
        val factory = AlertViewModelFactory(repository)
        viewModel = ViewModelProvider(this, factory)[AlertViewModel::class.java]
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

        // 1) Setup RecyclerView + Adapter
        val adapter = AlertsAdapter(emptyMap()) { alert ->
            val action = FeedFragmentDirections
                .actionFeedFragmentToAlertDetailsFragment(alert.postId)
            findNavController().navigate(action)
        }
        binding.alertsRecyclerView.layoutManager = LinearLayoutManager(requireContext())
        binding.alertsRecyclerView.adapter = adapter

        // 2) Coroutine scope for loading children, token, and alerts
        lifecycleScope.launch {
            // a) Fetch your child IDs from Firestore
            val uid = FirebaseAuth.getInstance().currentUser?.uid.orEmpty()
            val snap = FirebaseFirestore.getInstance()
                .collection("users")
                .document(uid)
                .collection("children")
                .get()
                .await()
            val childIds = snap.documents.map { it.id }

            // b) Fetch a fresh Firebase ID token
            val firebaseUser = FirebaseAuth.getInstance().currentUser ?: return@launch
            val tokenResult = firebaseUser.getIdToken(true).await()
            val token = tokenResult.token ?: return@launch

            // c) Kick off API fetch for each child
            childIds.forEach { childId ->
                viewModel.fetchAlerts(token, childId)
            }

            // d) Observe local DB, filter by your childIds, and submit to adapter
            viewModel.allAlerts.collectLatest { alerts ->
                adapter.submitList(alerts.filter { it.reporterId in childIds })
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
