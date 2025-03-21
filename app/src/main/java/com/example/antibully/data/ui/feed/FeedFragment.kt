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
import com.example.antibully.data.api.RetrofitClient
import com.example.antibully.data.db.AppDatabase
import com.example.antibully.data.repository.AlertRepository
import com.example.antibully.databinding.FragmentFeedBinding
import com.example.antibully.data.ui.adapters.AlertsAdapter
import com.example.antibully.viewmodel.AlertViewModel
import com.example.antibully.viewmodel.AlertViewModelFactory
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class FeedFragment : Fragment() {

    private var _binding: FragmentFeedBinding? = null
    private val binding get() = _binding!!

    private lateinit var viewModel: AlertViewModel
    private lateinit var alertAdapter: AlertsAdapter

    private val alertDao = AppDatabase.getDatabase(requireContext()).alertDao()
    private val alertRepository = AlertRepository(alertDao, RetrofitClient.apiService)
    val alertFactory = AlertViewModelFactory(alertRepository)

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentFeedBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Initialize ViewModel
        viewModel = ViewModelProvider(this,alertFactory)[AlertViewModel::class.java]

        // Set up adapter with click listener to navigate to AlertDetailsFragment
        alertAdapter = AlertsAdapter { alert ->
            val action = FeedFragmentDirections.actionFeedFragmentToAlertDetailsFragment(alert.postId)
            findNavController().navigate(action)
        }

        // Setup RecyclerView
        binding.alertsRecyclerView.layoutManager = LinearLayoutManager(requireContext())
        binding.alertsRecyclerView.adapter = alertAdapter

        // Collect alerts from ViewModel (Room)
        lifecycleScope.launch {
            viewModel.allAlerts.collectLatest { alerts ->
                alertAdapter.submitList(alerts)
            }
        }

        // Fetch fresh alerts from API
        viewModel.fetchAlerts()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
