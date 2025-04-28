package com.example.antibully.data.ui.statistics

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.example.antibully.R
import com.example.antibully.data.db.AppDatabase
import com.example.antibully.data.models.Alert
import com.example.antibully.data.models.ChildLocalData
import com.example.antibully.data.repository.AlertRepository
import com.example.antibully.viewmodel.AlertViewModel
import com.example.antibully.viewmodel.AlertViewModelFactory
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class StatisticsFragment : Fragment() {

    private lateinit var alertViewModel: AlertViewModel
    private var childIds: List<String> = emptyList()
    private lateinit var children: List<ChildLocalData>
    private lateinit var allAlerts: List<Alert>

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = inflater.inflate(R.layout.fragment_statistics, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Setup ViewModel
        val alertDao = AppDatabase.getDatabase(requireContext()).alertDao()
        val repository = AlertRepository(alertDao)
        alertViewModel = ViewModelProvider(
            this,
            AlertViewModelFactory(repository)
        )[AlertViewModel::class.java]

        // Load your ID-token
        val token = FirebaseAuth.getInstance()
            .currentUser
            ?.getIdToken(false)
            ?.result
            ?.token ?: ""

        // 1) Fetch your children & their alerts
        val uid = FirebaseAuth.getInstance().currentUser?.uid.orEmpty()
        FirebaseFirestore.getInstance()
            .collection("users")
            .document(uid)
            .collection("children")
            .get()
            .addOnSuccessListener { snap ->
                children = snap.documents.mapNotNull { doc ->
                    doc.toObject(ChildLocalData::class.java)?.copy(childId = doc.id)
                }
                childIds = children.map { it.childId }

                // Kick off fetch for each child
                childIds.forEach { id ->
                    alertViewModel.fetchAlerts(token, id)
                }

                // 2) Observe local DB—filter by your childIds to drive charts
                lifecycleScope.launch(Dispatchers.IO) {
                    alertViewModel.allAlerts.collectLatest { alerts ->
                        val filtered = alerts.filter { it.reporterId in childIds }
                        withContext(Dispatchers.Main) {
                            // TODO: feed `children` and `filtered` to your PieChart/BarChart
                        }
                    }
                }
            }
            .addOnFailureListener {
                // TODO: show error
            }
    }
}
