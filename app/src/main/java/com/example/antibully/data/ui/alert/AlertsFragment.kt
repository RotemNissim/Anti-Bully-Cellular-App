//package com.example.antibully.data.ui.alert
//
//import android.content.Context
//import android.os.Bundle
//import android.util.Log
//import android.view.LayoutInflater
//import android.view.View
//import android.view.ViewGroup
//import androidx.fragment.app.Fragment
//import androidx.lifecycle.ViewModelProvider
//import androidx.lifecycle.lifecycleScope
//import androidx.navigation.fragment.findNavController
//import androidx.recyclerview.widget.LinearLayoutManager
//import com.example.antibully.MyApp
//import com.example.antibully.data.db.AppDatabase
//import com.example.antibully.data.models.Alert
//import com.example.antibully.data.models.ChildLocalData
//import com.example.antibully.data.models.NotificationGroup
//import com.example.antibully.data.repository.AlertRepository
//import com.example.antibully.data.ui.adapters.AlertsAdapter
//import com.example.antibully.databinding.FragmentAlertsBinding
//import com.example.antibully.viewmodel.AlertViewModel
//import com.example.antibully.viewmodel.AlertViewModelFactory
//import com.google.firebase.auth.FirebaseAuth
//import kotlinx.coroutines.flow.collectLatest
//import kotlinx.coroutines.launch
//import kotlinx.coroutines.tasks.await
//
//class AlertsFragment : Fragment() {
//
//    private var _binding: FragmentAlertsBinding? = null
//    private val binding get() = _binding!!
//    private lateinit var viewModel: AlertViewModel
//
//    // Holds child details for name/image lookups
//    private val childDataMap = mutableMapOf<String, ChildLocalData>()
//
//    // ✅ Use single adapter that handles both notification groups and individual alerts
//    private lateinit var alertsAdapter: AlertsAdapter
//
//    override fun onCreateView(
//        inflater: LayoutInflater,
//        container: ViewGroup?,
//        savedInstanceState: Bundle?
//    ): View {
//        _binding = FragmentAlertsBinding.inflate(inflater, container, false)
//        return binding.root
//    }
//
//    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
//        super.onViewCreated(view, savedInstanceState)
//
//        // ✅ Mark that alerts fragment is visible
//        MyApp.isAlertsFragmentVisible = true
//
//        // 1) Setup adapter
//        setupAdapter()
//
//        // 2) ViewModel + Repository
//        val alertDao = AppDatabase.getDatabase(requireContext()).alertDao()
//        val repository = AlertRepository(alertDao)
//        val factory = AlertViewModelFactory(repository)
//        viewModel = ViewModelProvider(this, factory)[AlertViewModel::class.java]
//
//        // 3) Observe local DB updates
//        lifecycleScope.launch {
//            viewModel.allAlerts.collectLatest { alerts ->
//                Log.d("AlertsFragment", "📊 Received ${alerts.size} alerts from local DB")
//
//                // ✅ Separate unread and read alerts
//                val unreadAlerts = alerts.filter { !it.isRead }
//                val readAlerts = alerts.filter { it.isRead }
//
//                Log.d("AlertsFragment", "📈 Unread: ${unreadAlerts.size}, Read: ${readAlerts.size}")
//
//                // ✅ Create the display list
//                val displayList = mutableListOf<Any>()
//
//                // ✅ Create separate notification groups per child with unread alerts
//                if (unreadAlerts.isNotEmpty()) {
//                    val notificationGroups = createNotificationGroupsPerChild(unreadAlerts)
//                    displayList.addAll(notificationGroups)
//                    Log.d("AlertsFragment", "🔔 Added ${notificationGroups.size} notification groups")
//
//                    notificationGroups.forEachIndexed { index, group ->
//                        Log.d("AlertsFragment", "  Group $index: ${group.childName} with ${group.unreadCount} alerts")
//                    }
//                } else {
//                    Log.d("AlertsFragment", "✅ No unread alerts - no notification groups added")
//                }
//
//                // ✅ Add read alerts as individual items (these are NOT clickable)
//                displayList.addAll(readAlerts)
//                Log.d("AlertsFragment", "📝 Added ${readAlerts.size} read alerts as individual items")
//
//                // ✅ Update adapter with mixed list
//                alertsAdapter.submitMixedList(displayList)
//
//                Log.d("AlertsFragment", "🎯 Final display list size: ${displayList.size}")
//                displayList.forEachIndexed { index, item ->
//                    when (item) {
//                        is NotificationGroup -> Log.d("AlertsFragment", "  [$index] NotificationGroup: ${item.childName} (${item.unreadCount} alerts)")
//                        is Alert -> Log.d("AlertsFragment", "  [$index] Alert ${item.postId} (read: ${item.isRead})")
//                    }
//                }
//            }
//        }
//
//        // 4) Get Firebase token and fetch children data
//        lifecycleScope.launch {
//            val token = FirebaseAuth.getInstance().currentUser?.getIdToken(false)?.await()?.token
//            if (token != null) {
//                Log.d("AlertsFragment", "🔑 Got Firebase token, fetching children...")
//
//                // ✅ עדכן זמן פעילות רק פעם אחת בכניסה לפרגמנט
//                viewModel.updateUserActivity(token)
//
//                // 5) Get children from local database (already synced by ProfileFragment)
//                val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return@launch
//                val childDao = AppDatabase.getDatabase(requireContext()).childDao()
//                val children = childDao.getChildrenForUser(userId)
//
//                Log.d("AlertsFragment", "👶 Found ${children.size} children")
//
//                children.forEach { child ->
//                    Log.d("AlertsFragment", "🔄 Fetching alerts for child: ${child.childId}")
//                    childDataMap[child.childId] = child
//
//                    // Fetch alerts for this child
//                    viewModel.fetchAlerts(token, child.childId)
//                }
//
//                // Update adapter with child data
//                alertsAdapter.updateChildData(childDataMap)
//            } else {
//                Log.e("AlertsFragment", "❌ Failed to get Firebase token")
//            }
//        }
//    }
//
//    private fun setupAdapter() {
//        // ✅ Create unified adapter that handles both notification groups and individual alerts
//        alertsAdapter = AlertsAdapter(
//            childDataMap = childDataMap,
//            onAlertClick = { alert ->
//                // ✅ Individual alert clicked - only mark as read if it's unread
//                Log.d("AlertsFragment", "🔘 Individual alert clicked: ${alert.postId}, isRead: ${alert.isRead}")
//                if (!alert.isRead) {
//                    viewModel.markAlertAsRead(alert.postId)
//                }
//                // ✅ No navigation for individual alerts in alerts fragment
//            },
//            onNotificationGroupClick = { group ->
//                // ✅ Notification group clicked - navigate to child alerts
//                Log.d("AlertsFragment", "🔔 Notification group clicked: ${group.childName} with ${group.unreadCount} alerts")
//
//                // ✅ Mark all unread alerts as read when clicking the group
//                group.alerts.forEach { alert ->
//                    if (!alert.isRead) {
//                        viewModel.markAlertAsRead(alert.postId)
//                    }
//                }
//
//                // ✅ Navigate to child-specific alerts view
//                val action = AlertsFragmentDirections.actionAlertsFragmentToChildAlertsFragment(
//                    childId = group.childId,
//                    childName = group.childName
//                )
//                findNavController().navigate(action)
//            },
//            onMarkAsRead = { alert ->
//                Log.d("AlertsFragment", "✅ Mark as read callback: ${alert.postId}")
//                viewModel.markAlertAsRead(alert.postId)
//            }
//        )
//
//        binding.alertsRecyclerView.layoutManager = LinearLayoutManager(requireContext())
//        binding.alertsRecyclerView.adapter = alertsAdapter
//    }
//
//    // ✅ Create separate notification groups per child (like in mockup)
//    private fun createNotificationGroupsPerChild(unreadAlerts: List<Alert>): List<NotificationGroup> {
//        Log.d("AlertsFragment", "🏗️ Creating notification groups per child from ${unreadAlerts.size} unread alerts")
//
//        // ✅ Group unread alerts by child
//        val groupedByChild = unreadAlerts.groupBy { it.reporterId }
//
//        Log.d("AlertsFragment", "👶 Found ${groupedByChild.size} children with unread alerts:")
//        groupedByChild.forEach { (childId, alerts) ->
//            val childName = childDataMap[childId]?.name ?: childId
//            Log.d("AlertsFragment", "  - $childName ($childId): ${alerts.size} alerts")
//        }
//
//        // ✅ Create a notification group for each child
//        return groupedByChild.map { (childId, alertsForChild) ->
//            val childData = childDataMap[childId]
//            val childName = childData?.name ?: "Child $childId"
//            val latestTimestamp = alertsForChild.maxOfOrNull { it.timestamp } ?: 0L
//
//            Log.d("AlertsFragment", "🔔 Creating group for $childName with ${alertsForChild.size} alerts")
//
//            NotificationGroup(
//                childId = childId, // ✅ Use actual child ID
//                childName = childName, // ✅ Use actual child name
//                childImageUrl = childData?.imageUrl,
//                unreadCount = alertsForChild.size,
//                alerts = alertsForChild,
//                latestTimestamp = latestTimestamp
//            )
//        }.sortedByDescending { it.latestTimestamp } // ✅ Sort by most recent first
//    }
//
//    override fun onPause() {
//        super.onPause()
//        MyApp.isAlertsFragmentVisible = false
//    }
//
//    override fun onResume() {
//        super.onResume()
//        MyApp.isAlertsFragmentVisible = true
//
//        // ✅ עדכן זמן פעילות רק כשחוזרים לפרגמנט (לא כל הזמן)
//        lifecycleScope.launch {
//            val token = FirebaseAuth.getInstance().currentUser?.getIdToken(false)?.await()?.token
//            if (token != null) {
//                viewModel.updateUserActivity(token)
//            }
//        }
//    }
//
//    override fun onDestroyView() {
//        super.onDestroyView()
//        MyApp.isAlertsFragmentVisible = false
//        _binding = null
//    }
//}
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
import com.example.antibully.MyApp
import com.example.antibully.data.db.AppDatabase
import com.example.antibully.data.models.Alert
import com.example.antibully.data.models.ChildLocalData
import com.example.antibully.data.models.NotificationGroup
import com.example.antibully.data.repository.AlertRepository
import com.example.antibully.data.ui.adapters.AlertsAdapter
import com.example.antibully.databinding.FragmentAlertsBinding
import com.example.antibully.viewmodel.AlertViewModel
import com.example.antibully.viewmodel.AlertViewModelFactory
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class AlertsFragment : Fragment() {

    private var _binding: FragmentAlertsBinding? = null
    private val binding get() = _binding!!
    private lateinit var viewModel: AlertViewModel

    // Holds child details for name/image lookups
    private val childDataMap = mutableMapOf<String, ChildLocalData>()

    // ✅ Use single adapter that handles notification groups
    private lateinit var alertsAdapter: AlertsAdapter

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

        // ✅ Mark that alerts fragment is visible
        MyApp.isAlertsFragmentVisible = true

        // 1) Setup adapter
        setupAdapter()

        // 2) ViewModel + Repository
        val alertDao = AppDatabase.getDatabase(requireContext()).alertDao()
        val repository = AlertRepository(alertDao)
        val factory = AlertViewModelFactory(repository)
        viewModel = ViewModelProvider(this, factory)[AlertViewModel::class.java]

        // 3) Observe local DB updates and perform grouping
        lifecycleScope.launch {
            viewModel.allAlerts.collectLatest { alerts ->
                Log.d("AlertsFragment", "📊 Received ${alerts.size} alerts from local DB")

                // ✅ Step 1: Group alerts by child (reporterId)
                val groupedByChild = alerts.groupBy { it.reporterId }
                Log.d("AlertsFragment", "👶 Grouped into ${groupedByChild.size} children")

                // ✅ Step 2: Create one NotificationGroup per child that has unread alerts
                val notificationGroups = mutableListOf<NotificationGroup>()

                groupedByChild.forEach { (childId, alertsForChild) ->
                    val childData = childDataMap[childId]
                    val childName = childData?.name ?: "Child $childId"

                    // ✅ Count unread alerts for this child
                    val unreadAlerts = alertsForChild.filter { !it.isRead }
                    val totalAlerts = alertsForChild.size
                    val latestTimestamp = alertsForChild.maxOfOrNull { it.timestamp } ?: 0L

                    Log.d("AlertsFragment", "🔔 Child $childName: $totalAlerts total, ${unreadAlerts.size} unread")

                    // ✅ Create notification group ONLY if there are unread alerts
                    if (unreadAlerts.isNotEmpty()) {
                        val notificationGroup = NotificationGroup(
                            childId = childId,
                            childName = childName,
                            childImageUrl = childData?.imageUrl,
                            unreadCount = unreadAlerts.size, // ✅ Show exact count like in mockup
                            alerts = unreadAlerts, // ✅ Only include unread alerts in the group
                            latestTimestamp = latestTimestamp
                        )
                        notificationGroups.add(notificationGroup)
                        Log.d("AlertsFragment", "✅ Created notification group for $childName with ${unreadAlerts.size} unread alerts")
                    } else {
                        Log.d("AlertsFragment", "⏭️ Skipping $childName - no unread alerts")
                    }
                }

                // ✅ Step 3: Sort notification groups by most recent first
                val sortedGroups = notificationGroups.sortedByDescending { it.latestTimestamp }
                Log.d("AlertsFragment", "🎯 Final result: ${sortedGroups.size} notification groups")

                // ✅ Step 4: Submit notification groups to adapter
                Log.d("AlertsFragment", "📝 Submitting ${sortedGroups.size} notification groups to adapter")

                sortedGroups.forEachIndexed { index, group ->
                    Log.d("AlertsFragment", "  [$index] 🔔 ${group.childName}: ${group.unreadCount} unread alerts")
                }

                alertsAdapter.submitMixedList(sortedGroups)

                // ✅ Step 5: Show empty state if no notification groups
                if (sortedGroups.isEmpty()) {
                    Log.d("AlertsFragment", "📭 No unread alerts - showing empty state")
                    binding.emptyStateText?.visibility = View.VISIBLE
                    binding.alertsRecyclerView.visibility = View.GONE
                } else {
                    Log.d("AlertsFragment", "📋 Showing ${sortedGroups.size} notification groups")
                    binding.emptyStateText?.visibility = View.GONE
                    binding.alertsRecyclerView.visibility = View.VISIBLE
                }
            }
        }

        // 4) Get Firebase token and fetch children data
        lifecycleScope.launch {
            val token = FirebaseAuth.getInstance().currentUser?.getIdToken(false)?.await()?.token
            if (token != null) {
                Log.d("AlertsFragment", "🔑 Got Firebase token, fetching children...")

                // ✅ עדכן זמן פעילות רק פעם אחת בכניסה לפרגמנט
                viewModel.updateUserActivity(token)

                // 5) Get children from local database (already synced by ProfileFragment)
                val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return@launch
                val childDao = AppDatabase.getDatabase(requireContext()).childDao()
                val children = childDao.getChildrenForUser(userId)

                Log.d("AlertsFragment", "👶 Found ${children.size} children")

                children.forEach { child ->
                    Log.d("AlertsFragment", "🔄 Adding child data: ${child.name} (${child.childId})")
                    childDataMap[child.childId] = child

                    // Fetch alerts for this child
                    Log.d("AlertsFragment", "📡 Fetching alerts for child: ${child.childId}")
                    viewModel.fetchAlerts(token, child.childId)
                }

                // Update adapter with child data
                alertsAdapter.updateChildData(childDataMap)
                Log.d("AlertsFragment", "✅ Updated adapter with child data for ${children.size} children")
            } else {
                Log.e("AlertsFragment", "❌ Failed to get Firebase token")
            }
        }
    }

    private fun setupAdapter() {
        // ✅ Create unified adapter that handles notification groups
        alertsAdapter = AlertsAdapter(
            childDataMap = childDataMap,
            onAlertClick = { alert ->
                // ✅ This won't be called since we only show notification groups in alerts fragment
                Log.d("AlertsFragment", "🔘 Individual alert clicked: ${alert.postId}")
            },
            onNotificationGroupClick = { group ->
                // ✅ Notification group clicked - navigate to child alerts
                Log.d("AlertsFragment", "🔔 Notification group clicked: ${group.childName} with ${group.unreadCount} unread alerts")

                // ✅ Mark all unread alerts as read when clicking the group
                group.alerts.forEach { alert ->
                    if (!alert.isRead) {
                        Log.d("AlertsFragment", "✅ Marking alert as read: ${alert.postId}")
                        viewModel.markAlertAsRead(alert.postId)
                    }
                }

                // ✅ Navigate to child-specific alerts view
                val action = AlertsFragmentDirections.actionAlertsFragmentToChildAlertsFragment(
                    childId = group.childId,
                    childName = group.childName
                )
                findNavController().navigate(action)
            },
            onMarkAsRead = { alert ->
                Log.d("AlertsFragment", "✅ Mark as read callback: ${alert.postId}")
                viewModel.markAlertAsRead(alert.postId)
            }
        )

        binding.alertsRecyclerView.layoutManager = LinearLayoutManager(requireContext())
        binding.alertsRecyclerView.adapter = alertsAdapter

        Log.d("AlertsFragment", "🔧 Adapter setup complete")
    }

    override fun onPause() {
        super.onPause()
        MyApp.isAlertsFragmentVisible = false
    }

    override fun onResume() {
        super.onResume()
        MyApp.isAlertsFragmentVisible = true

        // ✅ עדכן זמן פעילות רק כשחוזרים לפרגמנט (לא כל הזמן)
        lifecycleScope.launch {
            val token = FirebaseAuth.getInstance().currentUser?.getIdToken(false)?.await()?.token
            if (token != null) {
                viewModel.updateUserActivity(token)
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        MyApp.isAlertsFragmentVisible = false
        _binding = null
    }
}
//package com.example.antibully.data.ui.alert
//
//import android.content.Context
//import android.os.Bundle
//import android.util.Log
//import android.view.LayoutInflater
//import android.view.View
//import android.view.ViewGroup
//import androidx.fragment.app.Fragment
//import androidx.lifecycle.ViewModelProvider
//import androidx.lifecycle.lifecycleScope
//import androidx.navigation.fragment.findNavController
//import androidx.recyclerview.widget.LinearLayoutManager
//import com.example.antibully.data.db.AppDatabase
//import com.example.antibully.data.models.ChildLocalData
//import com.example.antibully.data.repository.AlertRepository
//import com.example.antibully.data.ui.adapters.AlertsAdapter
//import com.example.antibully.databinding.FragmentAlertsBinding
//import com.example.antibully.viewmodel.AlertViewModel
//import com.example.antibully.viewmodel.AlertViewModelFactory
//import com.google.firebase.auth.FirebaseAuth
//import com.google.firebase.firestore.FirebaseFirestore
//import kotlinx.coroutines.flow.collectLatest
//import kotlinx.coroutines.launch
//import kotlinx.coroutines.tasks.await
//
//class AlertsFragment : Fragment() {
//
//    private var _binding: FragmentAlertsBinding? = null
//    private val binding get() = _binding!!
//    private lateinit var viewModel: AlertViewModel
//
//    // Holds child details for name/image lookups
//    private val childDataMap = mutableMapOf<String, ChildLocalData>()
//
//    override fun onCreateView(
//        inflater: LayoutInflater,
//        container: ViewGroup?,
//        savedInstanceState: Bundle?
//    ): View {
//        _binding = FragmentAlertsBinding.inflate(inflater, container, false)
//        return binding.root
//    }
//
//    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
//        super.onViewCreated(view, savedInstanceState)
//
//        // 1) RecyclerView + Adapter
//        val adapter = AlertsAdapter(childDataMap) { alert ->
//            // Navigate to alert details
//            val action = AlertsFragmentDirections.actionAlertsFragmentToAlertDetailsFragment(alert.postId) // ✅ This passes alert.postId as alertId
//            findNavController().navigate(action)
//        }
//        binding.alertsRecyclerView.layoutManager = LinearLayoutManager(requireContext())
//        binding.alertsRecyclerView.adapter = adapter
//
//        // 2) ViewModel + Repository
//        val alertDao = AppDatabase.getDatabase(requireContext()).alertDao()
//        val repository = AlertRepository(alertDao)
//        val factory = AlertViewModelFactory(repository)
//        viewModel = ViewModelProvider(this, factory)[AlertViewModel::class.java]
//
//        // 3) Observe local DB updates
//        lifecycleScope.launch {
//            viewModel.allAlerts.collectLatest { alerts ->
//                Log.d("AlertsFragment", "Received ${alerts.size} alerts from local DB") // ✅ Add logging
//                adapter.submitList(alerts)
//            }
//        }
//
//        // 4) Get Firebase token and fetch children data
//        lifecycleScope.launch {
//            val token = FirebaseAuth.getInstance().currentUser?.getIdToken(false)?.await()?.token
//            if (token != null) {
//                Log.d("AlertsFragment", "Got Firebase token, fetching children...")
//
//                // 5) Get children from local database (already synced by ProfileFragment)
//                val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return@launch
//                val childDao = AppDatabase.getDatabase(requireContext()).childDao()
//                val children = childDao.getChildrenForUser(userId)
//
//                Log.d("AlertsFragment", "Found ${children.size} children")
//
//                children.forEach { child ->
//                    Log.d("AlertsFragment", "Fetching alerts for child: ${child.childId}")
//                    childDataMap[child.childId] = child
//
//                    // Fetch alerts for this child
//                    viewModel.fetchAlerts(token, child.childId)
//                }
//
//                adapter.notifyDataSetChanged()
//            } else {
//                Log.e("AlertsFragment", "Failed to get Firebase token")
//            }
//        }
//    }
//
//    override fun onDestroyView() {
//        super.onDestroyView()
//        _binding = null
//    }
//}
