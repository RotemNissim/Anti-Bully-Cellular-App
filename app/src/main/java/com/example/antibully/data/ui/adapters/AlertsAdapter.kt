package com.example.antibully.data.ui.adapters

import android.text.format.DateUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.antibully.R
import com.example.antibully.data.models.Alert

class AlertsAdapter(
    private val onAlertClick: (Alert) -> Unit
) : ListAdapter<Alert, AlertsAdapter.AlertViewHolder>(AlertDiffCallback()) {

    class AlertViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        fun bind(alert: Alert, onClick: (Alert) -> Unit) {
            val title = itemView.findViewById<TextView>(R.id.alertTitle)
            val reason = itemView.findViewById<TextView>(R.id.alertReason)
            val time = itemView.findViewById<TextView>(R.id.alertTime)

            title.text = "Child's ID ${alert.reporterId}"
            reason.text = alert.reason
            time.text = DateUtils.getRelativeTimeSpanString(
                alert.timestamp,
                System.currentTimeMillis(),
                DateUtils.MINUTE_IN_MILLIS
            )

            itemView.setOnClickListener { onClick(alert) }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AlertViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_alert, parent, false)
        return AlertViewHolder(view)
    }

    override fun onBindViewHolder(holder: AlertViewHolder, position: Int) {
        holder.bind(getItem(position), onAlertClick)
    }
}

class AlertDiffCallback : DiffUtil.ItemCallback<Alert>() {
    override fun areItemsTheSame(oldItem: Alert, newItem: Alert): Boolean {
        return oldItem.postId == newItem.postId
    }

    override fun areContentsTheSame(oldItem: Alert, newItem: Alert): Boolean {
        return oldItem == newItem
    }
}
