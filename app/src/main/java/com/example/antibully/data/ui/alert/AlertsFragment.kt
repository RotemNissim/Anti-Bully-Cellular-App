package com.example.antibully.data.ui.alert

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.antibully.data.db.AppDatabase
import com.example.antibully.data.repository.AlertRepository
import com.example.antibully.data.api.RetrofitClient
import com.example.antibully.databinding.FragmentAlertsBinding
import com.example.antibully.data.ui.adapters.AlertsAdapter
import com.example.antibully.viewmodel.AlertViewModel
import com.example.antibully.viewmodel.AlertViewModelFactory
import kotlinx.coroutines.flow.collectLatest
import com.example.antibully.data.ui.feed.FeedFragmentDirections
import kotlinx.coroutines.launch

class AlertsFragment : Fragment() {

    private var _binding: FragmentAlertsBinding? = null
    private val binding get() = _binding!!

    private lateinit var viewModel: AlertViewModel
    private lateinit var adapter: AlertsAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAlertsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val alertDao = AppDatabase.getDatabase(requireContext()).alertDao()
        val repository = AlertRepository(alertDao, RetrofitClient.apiService)
        val factory = AlertViewModelFactory(repository)
        viewModel = ViewModelProvider(this, factory)[AlertViewModel::class.java]

        adapter = AlertsAdapter { alert ->
            // Navigate to AlertDetailsFragment (you'll set this up in nav_graph)
            val action = FeedFragmentDirections.actionFeedFragmentToAlertDetailsFragment(alert.postId)
            findNavController().navigate(action)
        }

        binding.alertsRecyclerView.layoutManager = LinearLayoutManager(requireContext())
        binding.alertsRecyclerView.adapter = adapter

        lifecycleScope.launch {
            viewModel.allAlerts.collectLatest { alerts ->
                adapter.submitList(alerts)
            }
        }

        viewModel.fetchAlerts()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
