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
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.antibully.data.db.AppDatabase
import com.example.antibully.data.models.ChildLocalData
import com.example.antibully.data.repository.AlertRepository
import com.example.antibully.data.ui.adapters.AlertsAdapter
import com.example.antibully.databinding.FragmentAlertsBinding
import com.example.antibully.viewmodel.AlertViewModel
import com.example.antibully.viewmodel.AlertViewModelFactory
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import  com.example.antibully.R

class AlertsFragment : Fragment() {

    private var _binding: FragmentAlertsBinding? = null
    private val binding get() = _binding!!
    private lateinit var viewModel: AlertViewModel

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

        val adapter = AlertsAdapter(
            childDataMap = childDataMap,
            onAlertClick = { alert ->
                val args = Bundle().apply { putString("alertId", alert.postId) }
                findNavController().navigate(R.id.alertDetailsFragment, args)
            },
            onUnreadGroupClick = { childId ->
                val childName = childDataMap[childId]?.name ?: ""
                Log.d("AlertsFragment", ">>> navigate to UnreadList for child=$childId name=$childName")
                val args = Bundle().apply {
                    putString("childId", childId)
                    putString("childName", childName)
                }
                findNavController().navigate(R.id.action_alertsFragment_to_unreadListFragment, args)
            }
        )

        findNavController().currentBackStackEntry?.savedStateHandle
            ?.getLiveData<Boolean>("refresh_feed")
            ?.observe(viewLifecycleOwner) { shouldRefresh ->
                if (shouldRefresh == true) {
                    viewLifecycleOwner.lifecycleScope.launch {
                        val token = FirebaseAuth.getInstance().currentUser?.getIdToken(false)?.await()?.token
                        if (token != null) {
                            viewModel.refreshLastSeen(token)
                        }
                    }
                    findNavController().currentBackStackEntry?.savedStateHandle?.set("refresh_feed", false)
                }
            }

        binding.alertsRecyclerView.layoutManager = LinearLayoutManager(requireContext())
        binding.alertsRecyclerView.adapter = adapter

        val alertDao = AppDatabase.getDatabase(requireContext()).alertDao()
        val repository = AlertRepository(alertDao)
        val factory = AlertViewModelFactory(repository)
        viewModel = ViewModelProvider(this, factory)[AlertViewModel::class.java]

        lifecycleScope.launchWhenStarted {
            viewModel.rows.collectLatest { items ->
                adapter.submitList(items)
            }
        }

        lifecycleScope.launch {
            val token = FirebaseAuth.getInstance().currentUser
                ?.getIdToken(false)?.await()?.token ?: run {
                Log.e("AlertsFragment", "Failed to get Firebase token")
                return@launch
            }
            viewModel.refreshLastSeen(token)
            val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return@launch
            val childDao = AppDatabase.getDatabase(requireContext()).childDao()
            val children = childDao.getChildrenForUser(userId)
            viewModel.setChildIds(children)
            children.forEach { child ->
                childDataMap[child.childId] = child
            }
            children.forEach { child ->
                viewModel.fetchAlerts(token, child.childId)
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
