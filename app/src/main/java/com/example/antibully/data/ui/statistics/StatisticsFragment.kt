package com.example.antibully.data.ui.statistics

import android.graphics.Color
import android.os.Bundle
import android.view.*
import android.widget.*
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
import com.github.mikephil.charting.charts.BarChart
import com.github.mikephil.charting.charts.PieChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.*
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import com.github.mikephil.charting.formatter.ValueFormatter
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.*

class StatisticsFragment : Fragment() {

    private lateinit var pieChart: PieChart
    private lateinit var barChart: BarChart
    private lateinit var spinner: Spinner
    private lateinit var alertViewModel: AlertViewModel
    private lateinit var auth: FirebaseAuth

    // Our data holders
    private var children: List<ChildLocalData> = emptyList()
    private var allAlerts: List<Alert> = emptyList()
    private val childColorMap = mutableMapOf<String, Int>()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = inflater.inflate(R.layout.fragment_statistics, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // 1) Wire up views
        pieChart   = view.findViewById(R.id.pieChart)
        barChart   = view.findViewById(R.id.barChart)
        spinner    = view.findViewById(R.id.spinnerChildren)
        auth       = FirebaseAuth.getInstance()

        // 2) Init ViewModel
        val dao    = AppDatabase.getDatabase(requireContext()).alertDao()
        val repo   = AlertRepository(dao)
        alertViewModel = ViewModelProvider(
            this,
            AlertViewModelFactory(repo)
        )[AlertViewModel::class.java]

        // 3) Get ID token then load everything
        auth.currentUser
            ?.getIdToken(false)
            ?.addOnSuccessListener { result ->
                val token = result.token ?: return@addOnSuccessListener
                loadChildrenAndAlerts(token)
            }
    }

    private fun loadChildrenAndAlerts(token: String) {
        val uid = auth.currentUser?.uid ?: return

        // fetch children from Firestore
        FirebaseFirestore.getInstance()
            .collection("users")
            .document(uid)
            .collection("children")
            .get()
            .addOnSuccessListener { snap ->
                // map Firestore → local models, extract IDs
                children = snap.documents.mapNotNull { doc ->
                    doc.toObject(ChildLocalData::class.java)?.copy(childId = doc.id)
                }

                // 1) Trigger server fetch for each child
                children.forEach { child ->
                    alertViewModel.fetchAlerts(token, child.childId)
                }

                // 2) Observe the Room cache, filter to only our children,
                //    then draw charts on the main thread
                viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
                    alertViewModel.allAlerts.collectLatest { alerts ->
                        // normalize timestamps and filter
                        allAlerts = alerts.map { alert ->
                            val t = if (alert.timestamp < 1_000_000_000_000L)
                                alert.timestamp * 1000L
                            else
                                alert.timestamp
                            alert.copy(timestamp = t)
                        }
                        withContext(Dispatchers.Main) {
                            setupPieChart()
                            setupSpinner()
                        }
                    }
                }
            }
    }

    private fun setupPieChart() {
        // count alerts per child
        val counts = children.associateWith { child ->
            allAlerts.count { it.reporterId == child.childId }
        }.filterValues { it > 0 }

        if (counts.isEmpty()) {
            pieChart.clear()
            pieChart.centerText = "No Data"
            return
        }

        // assign colors
        val colors = com.github.mikephil.charting.utils.ColorTemplate.MATERIAL_COLORS.toList()
        var idx = 0
        val entries = counts.map { (child, cnt) ->
            val c = colors[idx++ % colors.size]
            childColorMap[child.childId] = c
            PieEntry(cnt.toFloat(), child.name)
        }

        val ds = PieDataSet(entries, "Alerts per Child").apply {
            this.colors = entries.map { e ->
                childColorMap[ counts.keys.elementAt(entries.indexOf(e)).childId ] ?: Color.GRAY
            }
            valueTextSize = 14f
        }
        pieChart.apply {
            data = PieData(ds)
            description.isEnabled = false
            centerText = "Alerts"
            invalidate()
        }
    }

    private fun setupSpinner() {
        val names = children.map { it.name }
        val adapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_spinner_item,
            names
        ).apply {
            setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        }

        spinner.adapter = adapter
        spinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                parent: AdapterView<*>, view: View?, position: Int, id: Long
            ) {
                loadBarChartForChild(children[position].childId)
            }
            override fun onNothingSelected(parent: AdapterView<*>) {}
        }

        // preload first child’s chart
        if (children.isNotEmpty()) {
            loadBarChartForChild(children[0].childId)
        }
    }

    private fun loadBarChartForChild(childId: String) {
        // bucket alerts by day (past 7 days)
        val daily = linkedMapOf<String, Int>()
        val cal   = Calendar.getInstance()

        for (i in 6 downTo 0) {
            cal.timeInMillis = System.currentTimeMillis()
            cal.add(Calendar.DAY_OF_YEAR, -i)
            val key = "${cal.get(Calendar.MONTH)+1}/${cal.get(Calendar.DAY_OF_MONTH)}"
            daily[key] = 0
        }

        allAlerts.forEach { alert ->
            if (alert.reporterId != childId) return@forEach
            cal.timeInMillis = alert.timestamp
            val key = "${cal.get(Calendar.MONTH)+1}/${cal.get(Calendar.DAY_OF_MONTH)}"
            daily[key] = (daily[key] ?: 0) + 1
        }

        val entries = daily.entries.mapIndexed { idx, (day, cnt) ->
            BarEntry(idx.toFloat(), cnt.toFloat())
        }
        val labels = daily.keys.toList()

        val ds = BarDataSet(entries, "Alerts / Day").apply {
            color = childColorMap[childId] ?: Color.BLUE
            valueTextSize = 12f
            valueFormatter = object : ValueFormatter() {
                override fun getFormattedValue(value: Float) = value.toInt().toString()
            }
        }

        barChart.apply {
            data = BarData(ds).apply { barWidth = 0.9f }
            xAxis.apply {
                position = XAxis.XAxisPosition.BOTTOM
                granularity = 1f
                labelRotationAngle = -45f
                valueFormatter = IndexAxisValueFormatter(labels)
            }
            axisRight.isEnabled = false
            setFitBars(true)
            description.isEnabled = false
            invalidate()
        }
    }
}