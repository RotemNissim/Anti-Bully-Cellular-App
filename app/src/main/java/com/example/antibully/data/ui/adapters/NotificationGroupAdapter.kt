package com.example.antibully.data.ui.adapters

import android.text.format.DateUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.antibully.R
import com.example.antibully.data.models.ChildLocalData
import com.example.antibully.data.models.NotificationGroup
import com.google.android.material.imageview.ShapeableImageView
import com.squareup.picasso.Picasso

class NotificationGroupAdapter(
    private val childDataMap: Map<String, ChildLocalData>,
    private val onGroupClick: (NotificationGroup) -> Unit
) : RecyclerView.Adapter<NotificationGroupAdapter.NotificationGroupViewHolder>() {

    private var notificationGroups: List<NotificationGroup> = emptyList()

    inner class NotificationGroupViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val recentUpdatesTitle: TextView = itemView.findViewById(R.id.recentUpdatesTitle)
        private val profileImagesContainer: LinearLayout = itemView.findViewById(R.id.profileImagesContainer)
        private val childProfile1: ShapeableImageView = itemView.findViewById(R.id.childProfile1)
        private val childProfile2: ShapeableImageView = itemView.findViewById(R.id.childProfile2)
        private val alertTitle: TextView = itemView.findViewById(R.id.alertTitle)
        private val alertDescription: TextView = itemView.findViewById(R.id.alertDescription)
        private val timeIndicator: TextView = itemView.findViewById(R.id.timeIndicator)
        private val moreOptionsButton: ImageButton = itemView.findViewById(R.id.moreOptionsButton)

        fun bind(group: NotificationGroup) {
            // Set the title with unread count
            recentUpdatesTitle.text = "Recent updates"

            // Get child data
            val childData = childDataMap[group.childId]

            // Set profile image
            if (!childData?.imageUrl.isNullOrEmpty()) {
                Picasso.get()
                    .load(childData.imageUrl)
                    .placeholder(R.drawable.ic_default_profile)
                    .error(R.drawable.ic_default_profile)
                    .into(childProfile1)
            } else {
                childProfile1.setImageResource(R.drawable.ic_default_profile)
            }

            // Hide second profile for now (can be extended for multiple children)
            childProfile2.visibility = View.GONE

            // Set alert content based on the most recent alert
            val latestAlert = group.alerts.maxByOrNull { it.timestamp }
            if (latestAlert != null) {
                alertTitle.text = getAlertTitle(latestAlert.reason, group.unreadCount)
                alertDescription.text = getAlertDescription(latestAlert.reason, group.unreadCount, group.childName)
            }

            // Set time
            val displayTime = if (group.latestTimestamp < 1000000000000L) {
                group.latestTimestamp * 1000
            } else {
                group.latestTimestamp
            }

            timeIndicator.text = DateUtils.getRelativeTimeSpanString(
                displayTime,
                System.currentTimeMillis(),
                DateUtils.MINUTE_IN_MILLIS
            )

            // Set click listener
            itemView.setOnClickListener {
                onGroupClick(group)
            }
        }

        private fun getAlertTitle(reason: String, count: Int): String {
            return when {
                reason.contains("hate", ignoreCase = true) ||
                        reason.contains("harmful", ignoreCase = true) -> "Problematic Content"
                reason.contains("exclusion", ignoreCase = true) -> "Potential Signs Of Social Exclusion"
                reason.contains("verbal", ignoreCase = true) -> "Verbal Bullying"
                reason.contains("no issue", ignoreCase = true) -> "No Issues Detected"
                else -> "Alert Notification"
            }
        }

        private fun getAlertDescription(reason: String, count: Int, childName: String): String {
            return when {
                reason.contains("hate", ignoreCase = true) ||
                        reason.contains("harmful", ignoreCase = true) ->
                    "$count instances of harmful language were identified"
                reason.contains("exclusion", ignoreCase = true) ->
                    "Group chats indicate possible avoidance of your child"
                reason.contains("verbal", ignoreCase = true) ->
                    "Conversation tone suggesting mockery, humiliation, or belittling"
                reason.contains("no issue", ignoreCase = true) ->
                    "No problematic behavior has been detected recently"
                else -> "$count new alerts for $childName"
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NotificationGroupViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_notification_group, parent, false)
        return NotificationGroupViewHolder(view)
    }

    override fun onBindViewHolder(holder: NotificationGroupViewHolder, position: Int) {
        holder.bind(notificationGroups[position])
    }

    override fun getItemCount() = notificationGroups.size

    fun updateGroups(newGroups: List<NotificationGroup>) {
        notificationGroups = newGroups
        notifyDataSetChanged()
    }
}