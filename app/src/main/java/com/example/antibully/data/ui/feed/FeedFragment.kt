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
import com.example.antibully.data.api.RetrofitClient
import com.example.antibully.data.db.AppDatabase
import com.example.antibully.data.models.ChildLocalData
import com.example.antibully.data.repository.AlertRepository
import com.example.antibully.data.ui.adapters.AlertsAdapter
import com.example.antibully.databinding.FragmentFeedBinding
import com.example.antibully.viewmodel.AlertViewModel
import com.example.antibully.viewmodel.AlertViewModelFactory
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class FeedFragment : Fragment() {

    private var _binding: FragmentFeedBinding? = null
    private val binding get() = _binding!!

    private lateinit var viewModel: AlertViewModel
    private lateinit var alertAdapter: AlertsAdapter
    private lateinit var alertFactory: AlertViewModelFactory

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val alertDao = AppDatabase.getDatabase(requireContext()).alertDao()
        val alertRepository = AlertRepository(alertDao, RetrofitClient.apiService)
        alertFactory = AlertViewModelFactory(alertRepository)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentFeedBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewModel = ViewModelProvider(this, alertFactory)[AlertViewModel::class.java]
        val toggleGroup = binding.reasonToggleGroup
        val currentUserId = FirebaseAuth.getInstance().currentUser?.uid.orEmpty()
        val localChildDao = AppDatabase.getDatabase(requireContext()).childDao()

        FirebaseFirestore.getInstance()
            .collection("users")
            .document(currentUserId)
            .collection("children")
            .get()
            .addOnSuccessListener { result ->
                val firebaseChildren = result.documents.map { doc ->
                    val id = doc.id
                    val name = doc.getString("name") ?: id
                    id to name
                }.toMap()

                lifecycleScope.launch {
                    val localChildren = localChildDao.getChildrenForUser(currentUserId)
                    val roomMap = localChildren.associateBy { it.childId }

                    val mergedMap = firebaseChildren.mapValues { (childId, name) ->
                        val local = roomMap[childId]
                        ChildLocalData(
                            childId = childId,
                            parentUserId = currentUserId,
                            name = name,
                            imageUrl = local?.imageUrl
                        )
                    }

                    alertAdapter = AlertsAdapter(mergedMap) { alert ->
                        val action = FeedFragmentDirections.actionFeedFragmentToAlertDetailsFragment(alert.postId)
                        findNavController().navigate(action)
                    }

                    binding.alertsRecyclerView.layoutManager = LinearLayoutManager(requireContext())
                    binding.alertsRecyclerView.adapter = alertAdapter

                    toggleGroup.addOnButtonCheckedListener { _, checkedId, isChecked ->
                        if (isChecked) {
                            val reason = when (checkedId) {
                                R.id.btnHarassment -> "Harassment"
                                R.id.btnExclusion -> "Social Exclusion"
                                R.id.btnHateSpeech -> "Hate Speech"
                                R.id.btnCursing -> "Cursing"
                                else -> null
                            }

                            lifecycleScope.launch {
                                if (reason == null) {
                                    viewModel.allAlerts.collectLatest { alerts ->
                                        alertAdapter.submitList(alerts)
                                    }
                                } else {
                                    viewModel.getFilteredAlerts(reason).collectLatest { alerts ->
                                        alertAdapter.submitList(alerts)
                                    }
                                }
                            }
                        }
                    }

                    toggleGroup.check(R.id.btnAll)
                    viewModel.fetchAlerts()
                }
            }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
