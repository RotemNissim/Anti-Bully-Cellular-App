package com.example.antibully.data.ui.alert

import android.content.Context
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
import com.example.antibully.data.db.AppDatabase
import com.example.antibully.data.models.ChildLocalData
import com.example.antibully.data.repository.AlertRepository
import com.example.antibully.data.ui.adapters.AlertsAdapter
import com.example.antibully.databinding.FragmentAlertsBinding
import com.example.antibully.viewmodel.AlertViewModel
import com.example.antibully.viewmodel.AlertViewModelFactory
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class AlertsFragment : Fragment() {

    private var _binding: FragmentAlertsBinding? = null
    private val binding get() = _binding!!
    private lateinit var viewModel: AlertViewModel

    // Holds child details for name/image lookups
    private val childDataMap = mutableMapOf<String, ChildLocalData>()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAlertsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // 1) RecyclerView + Adapter
        val adapter = AlertsAdapter(childDataMap) { alert ->
            // Navigate to alert details
            val action = AlertsFragmentDirections.actionAlertsFragmentToAlertDetailsFragment(alert.postId) // ✅ This passes alert.postId as alertId
            findNavController().navigate(action)
        }
        binding.alertsRecyclerView.layoutManager = LinearLayoutManager(requireContext())
        binding.alertsRecyclerView.adapter = adapter

        // 2) ViewModel + Repository
        val alertDao = AppDatabase.getDatabase(requireContext()).alertDao()
        val repository = AlertRepository(alertDao)
        val factory = AlertViewModelFactory(repository)
        viewModel = ViewModelProvider(this, factory)[AlertViewModel::class.java]

        // 3) Observe local DB updates
        lifecycleScope.launch {
            viewModel.allAlerts.collectLatest { alerts ->
                Log.d("AlertsFragment", "Received ${alerts.size} alerts from local DB") // ✅ Add logging
                adapter.submitList(alerts)
            }
        }

        // 4) Get Firebase token and fetch children data
        lifecycleScope.launch {
            val token = FirebaseAuth.getInstance().currentUser?.getIdToken(false)?.await()?.token
            if (token != null) {
                Log.d("AlertsFragment", "Got Firebase token, fetching children...")

                // 5) Get children from local database (already synced by ProfileFragment)
                val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return@launch
                val childDao = AppDatabase.getDatabase(requireContext()).childDao()
                val children = childDao.getChildrenForUser(userId)

                Log.d("AlertsFragment", "Found ${children.size} children")

                children.forEach { child ->
                    Log.d("AlertsFragment", "Fetching alerts for child: ${child.childId}")
                    childDataMap[child.childId] = child

                    // Fetch alerts for this child
                    viewModel.fetchAlerts(token, child.childId)
                }

                adapter.notifyDataSetChanged()
            } else {
                Log.e("AlertsFragment", "Failed to get Firebase token")
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
