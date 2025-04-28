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
import com.google.android.material.button.MaterialButtonToggleGroup
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class FeedFragment : Fragment() {

    private var _binding: FragmentFeedBinding? = null
    private val binding get() = _binding!!
    private lateinit var viewModel: AlertViewModel

    // Hold just your children’s IDs
    private var childIds: List<String> = emptyList()

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

        // 1) Recycler setup
        val adapter = AlertsAdapter(emptyMap()) { alert ->
            val action = FeedFragmentDirections
                .actionFeedFragmentToAlertDetailsFragment(alert.postId)
            findNavController().navigate(action)
        }
        binding.alertsRecyclerView.layoutManager = LinearLayoutManager(requireContext())
        binding.alertsRecyclerView.adapter = adapter

        // 2) Load your Firebase ID-token
        val token = FirebaseAuth.getInstance()
            .currentUser
            ?.getIdToken(false)
            ?.result
            ?.token ?: ""

        // 3) Fetch *only* your children’s IDs from Firestore
        val uid = FirebaseAuth.getInstance().currentUser?.uid.orEmpty()
        FirebaseFirestore.getInstance()
            .collection("users")
            .document(uid)
            .collection("children")
            .get()
            .addOnSuccessListener { snap ->
                childIds = snap.documents.map { it.id }

                // a) Observe allAlerts and filter by those childIds
                lifecycleScope.launch {
                    viewModel.allAlerts.collectLatest { alerts ->
                        val filtered = alerts.filter { alert ->
                            alert.reporterId in childIds
                        }
                        adapter.submitList(filtered)
                    }
                }

                // b) Kick off API fetch for each child
                childIds.forEach { id ->
                    viewModel.fetchAlerts(token, id)
                }
            }
            .addOnFailureListener {
                // TODO: show an error
            }

        // 4) Toggle‐group still filters on *already filtered* local list
        binding.reasonToggleGroup.addOnButtonCheckedListener { _: MaterialButtonToggleGroup, checkedId, isChecked ->
            if (!isChecked) return@addOnButtonCheckedListener

            lifecycleScope.launch {
                when (checkedId) {
                    R.id.btnAll -> {
                        // re‐show everything for your children
                        viewModel.allAlerts.collectLatest { alerts ->
                            adapter.submitList(alerts.filter { it.reporterId in childIds })
                        }
                    }
                    R.id.btnHarassment -> {
                        viewModel.getAlertsByReason("Harassment").collectLatest { list ->
                            adapter.submitList(list.filter { it.reporterId in childIds })
                        }
                    }
                    R.id.btnExclusion -> {
                        viewModel.getAlertsByReason("Social Exclusion").collectLatest { list ->
                            adapter.submitList(list.filter { it.reporterId in childIds })
                        }
                    }
                    R.id.btnHateSpeech -> {
                        viewModel.getAlertsByReason("Hate Speech").collectLatest { list ->
                            adapter.submitList(list.filter { it.reporterId in childIds })
                        }
                    }
                    R.id.btnCursing -> {
                        viewModel.getAlertsByReason("Cursing").collectLatest { list ->
                            adapter.submitList(list.filter { it.reporterId in childIds })
                        }
                    }
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
