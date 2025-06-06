package com.example.antibully.data.ui.statistics

import android.content.Context
import android.graphics.Color
import android.os.Bundle
import android.util.Log
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
    private lateinit var progressBar: ProgressBar

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
        progressBar = view.findViewById(R.id.progressBar)

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
        progressBar.visibility = View.VISIBLE
        val uid = auth.currentUser?.uid ?: return

        // ✅ Get children from local database instead of Firestore
        lifecycleScope.launch {
            val childDao = AppDatabase.getDatabase(requireContext()).childDao()
            children = childDao.getChildrenForUser(uid)
            
            Log.d("StatisticsFragment", "Found ${children.size} children for statistics")
            
            if (children.isEmpty()) {
                Log.w("StatisticsFragment", "No children found for user $uid")
                withContext(Dispatchers.Main) {
                    progressBar.visibility = View.GONE
                    // Show "no children" message
                    pieChart.centerText = "No Children Added"
                    pieChart.setCenterTextColor(Color.WHITE)
                }
                return@launch
            }

            // Fetch alerts for each child
            children.forEach { child ->
                Log.d("StatisticsFragment", "Fetching alerts for child: ${child.childId}")
                alertViewModel.fetchAlerts(token, child.childId)
            }

            // Observe alerts and update charts
            alertViewModel.allAlerts.collectLatest { alerts ->
                Log.d("StatisticsFragment", "Received ${alerts.size} total alerts")
                
                // Filter alerts for our children
                allAlerts = alerts.filter { alert ->
                    children.any { child -> child.childId == alert.reporterId }
                }.map { alert ->
                    val t = if (alert.timestamp < 1_000_000_000_000L)
                        alert.timestamp * 1000L
                    else
                        alert.timestamp
                    alert.copy(timestamp = t)
                }
                
                Log.d("StatisticsFragment", "Filtered to ${allAlerts.size} alerts for our children")
                allAlerts.forEach { alert ->
                    Log.d("StatisticsFragment", "Alert: reporterId=${alert.reporterId}, reason=${alert.reason}")
                }

                withContext(Dispatchers.Main) {
                    setupPieChart()
                    setupSpinner()
                    progressBar.visibility = View.GONE
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
            pieChart.apply {
                clear()
                centerText = "No Data"
                setEntryLabelColor(Color.WHITE) // Set label color to white
                setCenterTextColor(Color.WHITE) // Set center text color to white
                setHoleColor(Color.TRANSPARENT) // Make the hole transparent
                legend.textColor = Color.WHITE // Set legend text color to white
            }
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
            valueTextColor = Color.WHITE // Set value text color to white
            valueLineColor = Color.WHITE // Set value line color to white
        }

        pieChart.apply {
            data = PieData(ds)
            description.isEnabled = false
            centerText = "Alerts"
            setEntryLabelColor(Color.WHITE) // Set entry label color to white
            setCenterTextColor(Color.WHITE) // Set center text color to white
            setHoleColor(Color.TRANSPARENT) // Make the hole transparent
            legend.textColor = Color.WHITE // Set legend text color to white
            invalidate()
        }
    }
    private class CustomSpinnerAdapter(
        context: Context,
        private val items: List<String>
    ) : ArrayAdapter<String>(context, R.layout.spinner_item, items) {

        override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
            val view = super.getView(position, convertView, parent)
            val textView = view.findViewById<TextView>(android.R.id.text1)
            textView.setTextColor(Color.WHITE)
            textView.textSize = 16f
            return view
        }

        override fun getDropDownView(position: Int, convertView: View?, parent: ViewGroup): View {
            val view = super.getDropDownView(position, convertView, parent)
            val textView = view.findViewById<TextView>(android.R.id.text1)
            textView.setTextColor(Color.WHITE)
            textView.textSize = 16f
            textView.setPadding(32, 16, 32, 16)
            return view
        }
    }

    private fun setupSpinner() {
        val names = children.map { it.name }
        val adapter = CustomSpinnerAdapter(requireContext(), names)

        // Set the dropdown layout style
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)

        spinner.adapter = adapter
        spinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                parent: AdapterView<*>, view: View?, position: Int, id: Long
            ) {
                loadBarChartForChild(children[position].childId)
            }
            override fun onNothingSelected(parent: AdapterView<*>) {}
        }

        // preload first child's chart
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
            valueTextColor = Color.WHITE // Set value text color to white
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
                textColor = Color.WHITE // Set X-axis text color to white
                gridColor = Color.GRAY // Set grid lines to gray
            }
            axisLeft.apply {
                textColor = Color.WHITE // Set Y-axis text color to white
                gridColor = Color.GRAY // Set grid lines to gray
            }
            axisRight.isEnabled = false
            legend.textColor = Color.WHITE // Set legend text color to white
            description.textColor = Color.WHITE // Set description text color to white
            setFitBars(true)
            description.isEnabled = false
            invalidate()
        }
    }
}
