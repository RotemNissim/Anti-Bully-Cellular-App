package com.example.antibully.data.ui.alert

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
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
            // handle click if you want
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
                adapter.submitList(alerts)
            }
        }

        // 4) Load saved Firebase ID-token
        val prefs = requireContext().getSharedPreferences("auth", Context.MODE_PRIVATE)
        val token = prefs.getString("auth_token", "") ?: ""

        // 5) Fetch children list, build map, and fetch each child’s alerts
        val currentUserId = FirebaseAuth.getInstance().currentUser?.uid.orEmpty()
        FirebaseFirestore.getInstance()
            .collection("users")
            .document(currentUserId)
            .collection("children")
            .get()
            .addOnSuccessListener { snapshot ->
                snapshot.documents.forEach { doc ->
                    // Convert Firestore data to your local model
                    val child = doc.toObject(ChildLocalData::class.java)
                    if (child != null) {
                        // Store for adapter lookups
                        childDataMap[child.childId] = child
                        adapter.notifyDataSetChanged()
                        // Trigger API + DB sync for this child
                        viewModel.fetchAlerts(token, child.childId)
                    }
                }
            }
            .addOnFailureListener {
                // TODO: show an error message
            }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
