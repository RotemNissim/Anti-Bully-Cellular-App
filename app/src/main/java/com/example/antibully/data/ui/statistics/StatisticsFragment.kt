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
import com.github.mikephil.charting.charts.PieChart
import com.github.mikephil.charting.charts.BarChart
import com.github.mikephil.charting.data.*
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import com.github.mikephil.charting.formatter.ValueFormatter
import com.github.mikephil.charting.utils.ColorTemplate
import com.google.firebase.auth.FirebaseAuth
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
    private lateinit var children: List<ChildLocalData>
    private lateinit var allAlerts: List<Alert>

    // Map to store childId -> color from PieChart
    private val childColorMap = mutableMapOf<String, Int>()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = inflater.inflate(R.layout.fragment_statistics, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        pieChart = view.findViewById(R.id.pieChart)
        barChart = view.findViewById(R.id.barChart)
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

        val materialColors = ColorTemplate.MATERIAL_COLORS.toList()
        var colorIndex = 0

        val entries = childAlertCounts.map { (child, count) ->
            // Assign color to child
            val color = materialColors[colorIndex % materialColors.size]
            childColorMap[child.childId] = color
            colorIndex++
            PieEntry(count.toFloat(), child.name)
        }

        val dataSet = PieDataSet(entries, "Alerts per Child").apply {
            colors = childAlertCounts.map { (child, _) -> childColorMap[child.childId] ?: Color.GRAY }
            valueTextSize = 16f
        }

        val pieData = PieData(dataSet).apply {
            setValueFormatter(object : ValueFormatter() {
                override fun getFormattedValue(value: Float): String {
                    return value.toInt().toString()
                }
            })
        }

        pieChart.data = pieData
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
                loadBarChartForChild(selectedChild.childId)
            }

            override fun onNothingSelected(parent: AdapterView<*>) {}
        }

        if (children.isNotEmpty()) {
            loadBarChartForChild(children[0].childId)
        }
    }

    private fun loadBarChartForChild(childId: String) {
        val alerts = allAlerts.filter { it.reporterId == childId }

        val calendar = Calendar.getInstance()
        val dailyMap = mutableMapOf<String, Int>()

        for (i in 6 downTo 0) {
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

        val sorted = dailyMap.toList()

        val entries = sorted.mapIndexed { index, entry ->
            BarEntry(index.toFloat(), entry.second.toFloat())
        }

        val labels = sorted.map { it.first }

        val dataSet = BarDataSet(entries, "Alerts per Day").apply {
            color = childColorMap[childId] ?: Color.BLUE
            valueTextSize = 12f
            valueFormatter = object : ValueFormatter() {
                override fun getFormattedValue(value: Float): String {
                    return value.toInt().toString()
                }
            }
        }

        val barData = BarData(dataSet).apply {
            barWidth = 0.9f
        }

        barChart.data = barData
        barChart.description.isEnabled = false
        barChart.setFitBars(true)
        barChart.xAxis.valueFormatter = IndexAxisValueFormatter(labels)
        barChart.xAxis.labelRotationAngle = -45f
        barChart.xAxis.granularity = 1f
        barChart.axisRight.isEnabled = false
        barChart.legend.isEnabled = false
        barChart.xAxis.position = com.github.mikephil.charting.components.XAxis.XAxisPosition.BOTTOM


        barChart.axisLeft.apply {
            axisMinimum = 0f
            granularity = 1f
            valueFormatter = object : ValueFormatter() {
                override fun getFormattedValue(value: Float): String {
                    return value.toInt().toString()
                }
            }
        }

        barChart.invalidate()
    }

}
