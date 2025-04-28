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
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

class StatisticsFragment : Fragment() {

    private lateinit var viewModel: AlertViewModel
    private lateinit var children: List<ChildLocalData>

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = inflater.inflate(R.layout.fragment_statistics, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // 1) ViewModel setup
        val alertDao = AppDatabase.getDatabase(requireContext()).alertDao()
        val repository = AlertRepository(alertDao)
        viewModel = ViewModelProvider(
            this,
            AlertViewModelFactory(repository)
        )[AlertViewModel::class.java]

        // 2) Load children and fetch alerts in coroutine
        lifecycleScope.launch {
            // a) Fetch list of your children
            val uid = FirebaseAuth.getInstance().currentUser?.uid.orEmpty()
            val snap = FirebaseFirestore.getInstance()
                .collection("users")
                .document(uid)
                .collection("children")
                .get()
                .await()
            children = snap.documents.mapNotNull { doc ->
                doc.toObject(ChildLocalData::class.java)?.copy(childId = doc.id)
            }
            val childIds = children.map { it.childId }

            // b) Fetch token
            val firebaseUser = FirebaseAuth.getInstance().currentUser ?: return@launch
            val tokenResult = firebaseUser.getIdToken(true).await()
            val token = tokenResult.token ?: return@launch

            // c) Kick off API fetch for each child
            childIds.forEach { viewModel.fetchAlerts(token, it) }

            // d) Collect from local DB, filter by your childIds, then update UI
            viewModel.allAlerts.collectLatest { alerts ->
                val filtered = alerts.filter { it.reporterId in childIds }
                withContext(Dispatchers.Main) {
                    // TODO: feed `children` and `filtered` into your charts
                }
            }
        }
    }
}
