package com.example.antibully.data.ui.adapters

import android.text.format.DateUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.antibully.R
import com.example.antibully.data.models.Alert
import com.example.antibully.data.models.AlertItem
import com.example.antibully.data.models.ChildLocalData
import com.google.android.material.imageview.ShapeableImageView
import com.squareup.picasso.Picasso
import com.example.antibully.utils.humanSummary

class AlertsAdapter(
    private val childDataMap: Map<String, ChildLocalData>,
    private val onAlertClick: (Alert) -> Unit,
    private val onUnreadGroupClick: (childId: String) -> Unit
) : ListAdapter<AlertItem, RecyclerView.ViewHolder>(Diff()) {

    companion object {
        private const val TYPE_GROUP = 0
        private const val TYPE_ALERT = 1
    }

    override fun getItemViewType(position: Int): Int = when (getItem(position)) {
        is AlertItem.UnreadGroup -> TYPE_GROUP
        is AlertItem.SingleAlert -> TYPE_ALERT
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            TYPE_GROUP -> {
                val v = LayoutInflater.from(parent.context)
                    .inflate(R.layout.item_unread_group, parent, false)
                UnreadGroupVH(v)
            }
            else -> {
                val v = LayoutInflater.from(parent.context)
                    .inflate(R.layout.item_alert, parent, false)
                AlertVH(v)
            }
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (val row = getItem(position)) {
            is AlertItem.UnreadGroup ->
                (holder as UnreadGroupVH).bind(row, childDataMap, onUnreadGroupClick)
            is AlertItem.SingleAlert ->
                (holder as AlertVH).bind(row.alert, childDataMap, onAlertClick)
        }
    }

    fun getAlertForPosition(position: Int): Alert? {
        if (position !in 0 until itemCount) return null
        val item = getItem(position)
        return (item as? AlertItem.SingleAlert)?.alert
    }

    class UnreadGroupVH(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val title = itemView.findViewById<TextView>(R.id.unreadTitle)
        private val count = itemView.findViewById<TextView>(R.id.unreadCount)
        private val chevron = itemView.findViewById<ImageView>(R.id.chevron)
        private val profileImage = itemView.findViewById<ImageView>(R.id.profileImage)

        fun bind(
            group: AlertItem.UnreadGroup,
            childMap: Map<String, ChildLocalData>,
            onClick: (String) -> Unit
        ) {
            val child = childMap[group.childId]
            val name = child?.name ?: group.childId
            title.text = name
            count.text = itemView.context.getString(R.string.unread_count_fmt, group.count)
            itemView.setOnClickListener { onClick(group.childId) }
            chevron.rotation = 0f

            val url = child?.imageUrl
            if (!url.isNullOrEmpty()) {
                Picasso.get()
                    .load(url)
                    .placeholder(R.drawable.ic_default_profile)
                    .error(R.drawable.ic_default_profile)
                    .into(profileImage)
            } else {
                profileImage.setImageResource(R.drawable.ic_default_profile)
            }
        }
    }

    class AlertVH(itemView: View) : RecyclerView.ViewHolder(itemView) {
        fun bind(
            alert: Alert,
            childMap: Map<String, ChildLocalData>,
            onClick: (Alert) -> Unit
        ) {
            val title = itemView.findViewById<TextView>(R.id.alertTitle)
            val reason = itemView.findViewById<TextView>(R.id.alertReason)
            val time = itemView.findViewById<TextView>(R.id.alertTime)
            val childProfileImage = itemView.findViewById<ShapeableImageView>(R.id.childProfileImage)
            val severityView = itemView.findViewById<TextView>(R.id.alertSeverity)

            val displayTime =
                if (alert.timestamp < 1_000_000_000_000L) alert.timestamp * 1000 else alert.timestamp

            val child = childMap[alert.reporterId]
            title.text = child?.name ?: alert.reporterId
            reason.text = humanSummary(alert.reason ?: "")
            time.text = DateUtils.getRelativeTimeSpanString(
                displayTime, System.currentTimeMillis(), DateUtils.MINUTE_IN_MILLIS
            )

            val sev = alert.severity.orEmpty().uppercase()
            severityView.text = itemView.context.getString(R.string.severity_fmt, sev)

            when (sev) {
                "HIGH"   -> severityView.setTextColor(itemView.context.getColor(android.R.color.holo_red_light))
                "MEDIUM" -> severityView.setTextColor(0xFFFFA500.toInt())
                "LOW"    -> severityView.setTextColor(0xFF4CAF50.toInt())
                else     -> severityView.setTextColor(0xFF9E9E9E.toInt())
            }

            val url = child?.imageUrl
            if (!url.isNullOrEmpty()) {
                Picasso.get()
                    .load(url)
                    .placeholder(R.drawable.ic_default_profile)
                    .error(R.drawable.ic_default_profile)
                    .into(childProfileImage)
            } else {
                childProfileImage.setImageResource(R.drawable.ic_default_profile)
            }

            itemView.setOnClickListener { onClick(alert) }
        }
    }

    class Diff : DiffUtil.ItemCallback<AlertItem>() {
        override fun areItemsTheSame(oldItem: AlertItem, newItem: AlertItem): Boolean =
            when {
                oldItem is AlertItem.UnreadGroup && newItem is AlertItem.UnreadGroup ->
                    oldItem.childId == newItem.childId
                oldItem is AlertItem.SingleAlert && newItem is AlertItem.SingleAlert ->
                    oldItem.alert.postId == newItem.alert.postId
                else -> false
            }

        override fun areContentsTheSame(oldItem: AlertItem, newItem: AlertItem): Boolean =
            oldItem == newItem
    }
}
