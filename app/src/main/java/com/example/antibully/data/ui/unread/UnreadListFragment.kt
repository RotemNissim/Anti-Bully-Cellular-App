package com.example.antibully.data.ui.unread

import android.os.Bundle
import android.text.format.DateFormat
import android.util.Log
import android.view.*
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.antibully.R
import com.example.antibully.data.db.AppDatabase
import com.example.antibully.data.models.AlertItem
import com.example.antibully.data.repository.AlertRepository
import com.example.antibully.data.ui.adapters.AlertsAdapter
import com.example.antibully.databinding.FragmentUnreadListBinding
import com.example.antibully.viewmodel.AlertViewModel
import com.example.antibully.viewmodel.AlertViewModelFactory
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.util.Date

class UnreadListFragment : Fragment() {

    private var _binding: FragmentUnreadListBinding? = null
    private val binding get() = _binding!!

    private lateinit var vm: AlertViewModel
    private lateinit var adapter: AlertsAdapter

    private val childId by lazy { requireArguments().getString("childId")!! }
    private val childName by lazy { requireArguments().getString("childName") ?: "" }

    private var didAutoClose = false
    private var lastSeenInitialized = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val dao = AppDatabase.getDatabase(requireContext()).alertDao()
        vm = ViewModelProvider(
            this, AlertViewModelFactory(AlertRepository(dao))
        )[AlertViewModel::class.java]
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentUnreadListBinding.inflate(inflater, container, false)

        binding.toolbar.setNavigationOnClickListener { findNavController().popBackStack() }

        val displayName = childName.ifBlank { childId }
        binding.titleUnread.text = getString(R.string.unread_title_child, displayName)

        binding.btnMarkAllRead.setOnClickListener { markAllRead() }

        binding.unreadRecycler.layoutManager = LinearLayoutManager(requireContext())

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        Log.d("UnreadListFragment", ">>> opened UnreadList for child=$childId name=$childName")

        viewLifecycleOwner.lifecycleScope.launch {
            val currentUserId = FirebaseAuth.getInstance().currentUser?.uid ?: return@launch
            val childDao = AppDatabase.getDatabase(requireContext()).childDao()
            val children = childDao.getChildrenForUser(currentUserId)
            vm.setChildIds(children)

            if (childName.isBlank()) {
                val fromDb = children.firstOrNull { it.childId == childId }?.name
                if (!fromDb.isNullOrBlank()) {
                    binding.titleUnread.text = getString(R.string.unread_title_child, fromDb)
                }
            }

            val childDataMap = children.associateBy { it.childId }
            adapter = AlertsAdapter(
                childDataMap = childDataMap,
                onAlertClick = { alert ->
                    Log.d("UnreadListFragment", "onAlertClick postId=${alert.postId} -> navigate")
                    val args = Bundle().apply { putString("alertId", alert.postId) }
                    findNavController().navigate(R.id.alertDetailsFragment, args)
                },
                onUnreadGroupClick = { }
            )
            binding.unreadRecycler.adapter = adapter
        }

        viewLifecycleOwner.lifecycleScope.launch {
            val token = FirebaseAuth.getInstance().currentUser?.getIdToken(false)?.await()?.token
            if (token != null) {
                Log.d("UnreadListFragment", "refreshLastSeen + fetchAlerts for child=$childId")
                vm.refreshLastSeen(token)
                lastSeenInitialized = true
                vm.fetchAlerts(token, childId)
            } else {
                Log.e("UnreadListFragment", "Failed to get token; continuing with local data only")
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            vm.getUnreadForChild(childId)
                .combine(vm.lastSeenMillis) { alerts, lastSeen ->
                    Pair(alerts, lastSeen)
                }
                .collectLatest { (alerts, lastSeen) ->
                    Log.d("UnreadListFragment", "unread for child=$childId -> ${alerts.size} items")

                    if (alerts.isEmpty() && lastSeenInitialized && !didAutoClose) {
                        didAutoClose = true
                        findNavController().popBackStack()
                        return@collectLatest
                    }

                    val rows = alerts.map { AlertItem.SingleAlert(it) }
                    adapter.submitList(rows)

                    val sinceText = lastSeen?.let { formatTime(it) } ?: "--:--"
                    binding.subtitleSince.text = getString(
                        R.string.unread_since_template,
                        alerts.size, sinceText
                    )
                }
        }
    }

    private fun formatTime(millis: Long): String {
        val df = DateFormat.getTimeFormat(requireContext())
        return df.format(Date(millis))
    }

    private fun markAllRead() = viewLifecycleOwner.lifecycleScope.launch {
        val token = FirebaseAuth.getInstance().currentUser?.getIdToken(false)?.await()?.token
        if (token != null) {
            Log.d("UnreadListFragment", "markAllRead()")
            vm.markAllRead(token)
            Snackbar.make(
                binding.root,
                getString(R.string.marked_all_read),
                Snackbar.LENGTH_SHORT
            ).show()
            findNavController().previousBackStackEntry
                ?.savedStateHandle
                ?.set("refresh_feed", true)
            findNavController().popBackStack()
        } else {
            Snackbar.make(
                binding.root,
                getString(R.string.auth_error),
                Snackbar.LENGTH_SHORT
            ).show()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
