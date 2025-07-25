package com.example.antibully.data.ui.alert

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.antibully.data.db.AppDatabase
import com.example.antibully.data.models.ChildLocalData
import com.example.antibully.data.repository.AlertRepository
import com.example.antibully.data.ui.adapters.AlertsAdapter
import com.example.antibully.databinding.FragmentChildAlertsBinding
import com.example.antibully.viewmodel.AlertViewModel
import com.example.antibully.viewmodel.AlertViewModelFactory
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class ChildAlertsFragment : Fragment() {

    private var _binding: FragmentChildAlertsBinding? = null
    private val binding get() = _binding!!
    private lateinit var viewModel: AlertViewModel

    private val args: ChildAlertsFragmentArgs by navArgs()
    private val childId: String by lazy { args.childId }
    private val childName: String by lazy { args.childName }

    private val childDataMap = mutableMapOf<String, ChildLocalData>()
    private lateinit var alertsAdapter: AlertsAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentChildAlertsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Set title
        binding.headerTitle.text = "Alerts for $childName"

        // ✅ Setup back button
        binding.btnBack.setOnClickListener {
            findNavController().navigateUp()
        }

        // Setup ViewModel
        val alertDao = AppDatabase.getDatabase(requireContext()).alertDao()
        val repository = AlertRepository(alertDao)
        val factory = AlertViewModelFactory(repository)
        viewModel = ViewModelProvider(this, factory)[AlertViewModel::class.java]

        // Load child data
        lifecycleScope.launch {
            val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return@launch
            val childDao = AppDatabase.getDatabase(requireContext()).childDao()
            val children = childDao.getChildrenForUser(userId)

            children.forEach { child ->
                childDataMap[child.childId] = child
            }
        }

        // ✅ Setup adapter - all alerts in this view are clickable and can be marked as read
        alertsAdapter = AlertsAdapter(
            childDataMap = childDataMap,
            onAlertClick = { alert ->
                // Mark as read when clicked
                if (!alert.isRead) {
                    Log.d("ChildAlertsFragment", "Marking alert as read: ${alert.postId}")
                    viewModel.markAlertAsRead(alert.postId)
                }
            },
            onMarkAsRead = { alert ->
                Log.d("ChildAlertsFragment", "Marking alert as read via callback: ${alert.postId}")
                viewModel.markAlertAsRead(alert.postId)
            },
            showOnlyReadAlerts = false // ✅ All alerts are interactive here
        )

        binding.alertsRecyclerView.layoutManager = LinearLayoutManager(requireContext())
        binding.alertsRecyclerView.adapter = alertsAdapter

        // ✅ Observe alerts for this specific child
        lifecycleScope.launch {
            viewModel.getAlertsForChild(childId).collectLatest { alerts ->
                Log.d("ChildAlertsFragment", "Received ${alerts.size} alerts for child $childId")

                // Sort by timestamp (newest first)
                val sortedAlerts = alerts.sortedByDescending { it.timestamp }
                alertsAdapter.submitList(sortedAlerts)

                // Update empty state
                if (sortedAlerts.isEmpty()) {
                    binding.emptyStateText.visibility = View.VISIBLE
                    binding.alertsRecyclerView.visibility = View.GONE
                } else {
                    binding.emptyStateText.visibility = View.GONE
                    binding.alertsRecyclerView.visibility = View.VISIBLE
                }
            }
        }

        // ✅ Mark all alerts for this child as read when entering this screen
        Log.d("ChildAlertsFragment", "Marking all alerts as read for child: $childId")
        viewModel.markAllAlertsAsReadForChild(childId)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}