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
import com.example.antibully.data.models.ChildLocalData
import com.google.android.material.imageview.ShapeableImageView
import com.squareup.picasso.Picasso

class AlertsAdapter(
    private val childDataMap: Map<String, ChildLocalData>,
    private val onAlertClick: (Alert) -> Unit
) : ListAdapter<Alert, AlertsAdapter.AlertViewHolder>(AlertDiffCallback()) {

    class AlertViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        fun bind(alert: Alert, childDataMap: Map<String, ChildLocalData>, onClick: (Alert) -> Unit) {
            val title = itemView.findViewById<TextView>(R.id.alertTitle)
            val reason = itemView.findViewById<TextView>(R.id.alertReason)
            val time = itemView.findViewById<TextView>(R.id.alertTime)
            val childProfileImage = itemView.findViewById<ShapeableImageView>(R.id.childProfileImage)
            val severityView = itemView.findViewById<TextView>(R.id.alertSeverity)

            val displayTime = if (alert.timestamp < 1000000000000L) {
                alert.timestamp * 1000
            } else {
                alert.timestamp
            }

            val childData = childDataMap[alert.reporterId]
            val childName = childData?.name ?: alert.reporterId

            // ✅ Set core text values
            title.text = "$childName"
            reason.text = alert.reason
            time.text = DateUtils.getRelativeTimeSpanString(
                displayTime,
                System.currentTimeMillis(),
                DateUtils.MINUTE_IN_MILLIS
            )

            val severity = alert.severity.uppercase()
            severityView.text = "$severity SEVERITY"

            when (severity) {
                "HIGH" -> severityView.setTextColor(itemView.context.getColor(android.R.color.holo_red_light))
                "MEDIUM" -> severityView.setTextColor(android.graphics.Color.parseColor("#FFA500")) // Orange
                "LOW" -> severityView.setTextColor(android.graphics.Color.GREEN)
                else -> severityView.setTextColor(android.graphics.Color.GRAY)
            }

            if (!childData?.imageUrl.isNullOrEmpty()) {
                Picasso.get()
                    .load(childData.imageUrl)
                    .placeholder(R.drawable.ic_default_profile)
                    .error(R.drawable.ic_default_profile)
                    .into(childProfileImage)
            } else {
                childProfileImage.setImageResource(R.drawable.ic_default_profile)
            }

            itemView.setOnClickListener { onClick(alert) }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AlertViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_alert, parent, false)
        return AlertViewHolder(view)
    }

    override fun onBindViewHolder(holder: AlertViewHolder, position: Int) {
        holder.bind(getItem(position), childDataMap, onAlertClick)
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