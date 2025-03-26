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
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.charts.PieChart
import com.github.mikephil.charting.data.*
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import com.github.mikephil.charting.utils.ColorTemplate
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.*

class StatisticsFragment : Fragment() {

    private lateinit var pieChart: PieChart
    private lateinit var lineChart: LineChart
    private lateinit var spinner: Spinner
    private lateinit var alertViewModel: AlertViewModel

    private lateinit var auth: FirebaseAuth
    private lateinit var children: List<ChildLocalData>
    private lateinit var allAlerts: List<Alert>

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = inflater.inflate(R.layout.fragment_statistics, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        pieChart = view.findViewById(R.id.pieChart)
        lineChart = view.findViewById(R.id.lineChart)
        spinner = view.findViewById(R.id.spinnerChildren)
        auth = FirebaseAuth.getInstance()

        val alertDao = AppDatabase.getDatabase(requireContext()).alertDao()
        val dummyApi = object : com.example.antibully.data.api.MessageApiService {
            override suspend fun getAllFlaggedMessages() = throw NotImplementedError()
            override suspend fun addMessage(message: com.example.antibully.data.models.MessageRequest) = throw NotImplementedError()
            override suspend fun updateMessageFlag(id: String, updateData: Map<String, Any>) = throw NotImplementedError()
            override suspend fun deleteMessage(id: String) = throw NotImplementedError()
        }
        val repository = AlertRepository(alertDao, dummyApi)
        alertViewModel = ViewModelProvider(this, AlertViewModelFactory(repository))[AlertViewModel::class.java]

        loadChildrenAndAlerts()
    }

    private fun loadChildrenAndAlerts() {
        val uid = auth.currentUser?.uid ?: return

        lifecycleScope.launch(Dispatchers.IO) {
            val dao = AppDatabase.getDatabase(requireContext()).childDao()
            children = dao.getChildrenForUser(uid)
            alertViewModel.allAlerts.collectLatest { alerts ->
                allAlerts = alerts
                withContext(Dispatchers.Main) {
                    setupPieChart()
                    setupSpinner()
                }
            }
        }
    }

    private fun setupPieChart() {
        val childAlertCounts = children.associateWith { child ->
            allAlerts.count { it.reporterId == child.childId }
        }.filterValues { it > 0 }

        if (childAlertCounts.isEmpty()) {
            pieChart.clear()
            pieChart.centerText = "No Data"
            pieChart.invalidate()
            return
        }

        val entries = childAlertCounts.map { (child, count) ->
            PieEntry(count.toFloat(), child.name)
        }

        val dataSet = PieDataSet(entries, "Alerts per Child").apply {
            colors = ColorTemplate.MATERIAL_COLORS.toList()
            valueTextSize = 16f
        }

        pieChart.data = PieData(dataSet)
        pieChart.description.isEnabled = false
        pieChart.centerText = "Alerts per Child"
        pieChart.setEntryLabelColor(Color.BLACK)
        pieChart.invalidate()
    }

    private fun setupSpinner() {
        val names = children.map { it.name }
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, names)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinner.adapter = adapter

        spinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
                val selectedChild = children[position]
                loadLineChartForChild(selectedChild.childId)
            }

            override fun onNothingSelected(parent: AdapterView<*>) {}
        }

        if (children.isNotEmpty()) {
            loadLineChartForChild(children[0].childId)
        }
    }

    private fun loadLineChartForChild(childId: String) {
        val alerts = allAlerts.filter { it.reporterId == childId }
        setupLineChart(alerts)
    }

    private fun setupLineChart(alerts: List<Alert>) {
        val calendar = Calendar.getInstance()
        val dailyMap = mutableMapOf<String, Int>()

        for (i in 0..29) {
            calendar.timeInMillis = System.currentTimeMillis()
            calendar.add(Calendar.DAY_OF_YEAR, -i)
            val key = "${calendar.get(Calendar.MONTH) + 1}/${calendar.get(Calendar.DAY_OF_MONTH)}"
            dailyMap[key] = 0
        }

        for (alert in alerts) {
            calendar.timeInMillis = alert.timestamp
            val key = "${calendar.get(Calendar.MONTH) + 1}/${calendar.get(Calendar.DAY_OF_MONTH)}"
            if (dailyMap.containsKey(key)) {
                dailyMap[key] = dailyMap[key]!! + 1
            }
        }

        val sorted = dailyMap.toList().sortedBy { it.first }
        val entries = sorted.mapIndexed { index, entry -> Entry(index.toFloat(), entry.second.toFloat()) }
        val labels = sorted.map { it.first }

        val dataSet = LineDataSet(entries, "Harmful Alerts per Day").apply {
            color = Color.RED
            valueTextSize = 10f
            circleRadius = 4f
            setCircleColor(Color.RED)
        }

        lineChart.data = LineData(dataSet)
        lineChart.description.isEnabled = false
        lineChart.xAxis.valueFormatter = IndexAxisValueFormatter(labels)
        lineChart.xAxis.labelRotationAngle = -45f
        lineChart.xAxis.granularity = 1f
        lineChart.invalidate()
    }
}
