package com.example.antibully.data.ui.adapters

import android.text.format.DateUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.antibully.R
import com.example.antibully.data.models.Alert
import com.example.antibully.data.models.ChildLocalData
import com.example.antibully.data.models.NotificationGroup
import com.google.android.material.imageview.ShapeableImageView
import com.squareup.picasso.Picasso

class AlertsAdapter(
    private var childDataMap: Map<String, ChildLocalData>,
    private val onAlertClick: (Alert) -> Unit,
    private val onNotificationGroupClick: (NotificationGroup) -> Unit = {},
    private val onMarkAsRead: (Alert) -> Unit = {},
    private val showOnlyReadAlerts: Boolean = false
) : ListAdapter<Any, RecyclerView.ViewHolder>(MixedDiffCallback()) {

    companion object {
        private const val TYPE_NOTIFICATION_GROUP = 0
        private const val TYPE_ALERT = 1
    }

    // ✅ Method to update child data
    fun updateChildData(newChildDataMap: Map<String, ChildLocalData>) {
        childDataMap = newChildDataMap
        notifyDataSetChanged()
    }

    // ✅ Method to submit mixed list of notification groups and alerts
    fun submitMixedList(items: List<Any>) {
        android.util.Log.d("AlertsAdapter", "📝 Submitting mixed list with ${items.size} items")
        items.forEachIndexed { index, item ->
            when (item) {
                is NotificationGroup -> android.util.Log.d("AlertsAdapter", "  [$index] 🔔 NotificationGroup: ${item.childName} (${item.unreadCount} alerts)")
                is Alert -> android.util.Log.d("AlertsAdapter", "  [$index] 📄 Alert ${item.postId} (read: ${item.isRead})")
                else -> android.util.Log.d("AlertsAdapter", "  [$index] ❓ Unknown type: ${item::class.simpleName}")
            }
        }
        submitList(items)
    }

    override fun getItemViewType(position: Int): Int {
        val item = getItem(position)
        val type = when (item) {
            is NotificationGroup -> {
                android.util.Log.d("AlertsAdapter", "🔔 Position $position is NotificationGroup: ${item.childName}")
                TYPE_NOTIFICATION_GROUP
            }
            is Alert -> {
                android.util.Log.d("AlertsAdapter", "📄 Position $position is Alert: ${item.postId}")
                TYPE_ALERT
            }
            else -> {
                android.util.Log.e("AlertsAdapter", "❌ Unknown item type at position $position: ${item?.javaClass?.simpleName}")
                TYPE_ALERT
            }
        }
        android.util.Log.d("AlertsAdapter", "🎯 getItemViewType($position) = $type")
        return type
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        android.util.Log.d("AlertsAdapter", "🏗️ Creating ViewHolder for type: $viewType")
        return when (viewType) {
            TYPE_NOTIFICATION_GROUP -> {
                val view = LayoutInflater.from(parent.context)
                    .inflate(R.layout.item_notification_group, parent, false)
                android.util.Log.d("AlertsAdapter", "✅ Created NotificationGroupViewHolder with layout: item_notification_group")
                NotificationGroupViewHolder(view)
            }
            TYPE_ALERT -> {
                val view = LayoutInflater.from(parent.context)
                    .inflate(R.layout.item_alert, parent, false)
                android.util.Log.d("AlertsAdapter", "✅ Created AlertViewHolder with layout: item_alert")
                AlertViewHolder(view)
            }
            else -> {
                android.util.Log.e("AlertsAdapter", "❌ Unknown view type: $viewType")
                throw IllegalArgumentException("Unknown view type: $viewType")
            }
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val item = getItem(position)
        android.util.Log.d("AlertsAdapter", "🔗 Binding position $position: ${item::class.simpleName}")

        when {
            holder is NotificationGroupViewHolder && item is NotificationGroup -> {
                android.util.Log.d("AlertsAdapter", "🔔 Binding NotificationGroup: ${item.childName} with ${item.unreadCount} alerts")
                holder.bind(item, childDataMap, onNotificationGroupClick)
            }
            holder is AlertViewHolder && item is Alert -> {
                android.util.Log.d("AlertsAdapter", "📄 Binding Alert ${item.postId} (read: ${item.isRead})")
                holder.bind(item, childDataMap, onAlertClick, onMarkAsRead, showOnlyReadAlerts)
            }
            else -> {
                android.util.Log.e("AlertsAdapter", "❌ Mismatch: holder=${holder::class.simpleName}, item=${item::class.simpleName}")
            }
        }
    }

    // ✅ ViewHolder for notification groups
    class NotificationGroupViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val recentUpdatesTitle: TextView = itemView.findViewById(R.id.recentUpdatesTitle)
        private val profileImagesContainer: LinearLayout = itemView.findViewById(R.id.profileImagesContainer)
        private val childProfile1: ShapeableImageView = itemView.findViewById(R.id.childProfile1)
        private val childProfile2: ShapeableImageView = itemView.findViewById(R.id.childProfile2)
        private val alertTitle: TextView = itemView.findViewById(R.id.alertTitle)
        private val alertDescription: TextView = itemView.findViewById(R.id.alertDescription)
        private val timeIndicator: TextView = itemView.findViewById(R.id.timeIndicator)
        private val moreOptionsButton: ImageButton = itemView.findViewById(R.id.moreOptionsButton)

        fun bind(
            group: NotificationGroup,
            childDataMap: Map<String, ChildLocalData>,
            onGroupClick: (NotificationGroup) -> Unit
        ) {
            android.util.Log.d("NotificationGroupViewHolder", "🔔 Starting bind for: ${group.childName} with ${group.unreadCount} alerts")

            // ✅ Set the title to show child-specific info
            recentUpdatesTitle.text = "Recent updates"

            // ✅ Get child data for this specific group
            val childData = childDataMap[group.childId]
            android.util.Log.d("NotificationGroupViewHolder", "👤 Child: ${group.childId} (${group.childName}), has image: ${!childData?.imageUrl.isNullOrEmpty()}")

            // ✅ Set profile image for this specific child
            if (!childData?.imageUrl.isNullOrEmpty()) {
                android.util.Log.d("NotificationGroupViewHolder", "🖼️ Loading image: ${childData.imageUrl}")
                Picasso.get()
                    .load(childData.imageUrl)
                    .placeholder(R.drawable.ic_default_profile)
                    .error(R.drawable.ic_default_profile)
                    .into(childProfile1)
            } else {
                android.util.Log.d("NotificationGroupViewHolder", "🖼️ Using default profile image")
                childProfile1.setImageResource(R.drawable.ic_default_profile)
            }

            // ✅ Hide second profile for now
            childProfile2.visibility = View.GONE

            // ✅ Set alert content based on the most recent alert and total count
            val latestAlert = group.alerts.maxByOrNull { it.timestamp }
            if (latestAlert != null) {
                val title = getAlertTitle(latestAlert.reason, group.unreadCount)
                val description = getAlertDescription(latestAlert.reason, group.unreadCount, group.childName)

                alertTitle.text = title
                alertDescription.text = description

                android.util.Log.d("NotificationGroupViewHolder", "📝 Set title: '$title'")
                android.util.Log.d("NotificationGroupViewHolder", "📝 Set description: '$description'")
            } else {
                alertTitle.text = "New Alerts"
                alertDescription.text = "${group.unreadCount} new alerts for ${group.childName}"
                android.util.Log.d("NotificationGroupViewHolder", "📝 Set default title and description")
            }

            // ✅ Set time based on most recent alert
            val displayTime = if (group.latestTimestamp < 1000000000000L) {
                group.latestTimestamp * 1000
            } else {
                group.latestTimestamp
            }

            val timeText = DateUtils.getRelativeTimeSpanString(
                displayTime,
                System.currentTimeMillis(),
                DateUtils.MINUTE_IN_MILLIS
            ).toString()

            timeIndicator.text = timeText
            android.util.Log.d("NotificationGroupViewHolder", "⏰ Set time: '$timeText'")

            // ✅ Set click listener
            itemView.setOnClickListener {
                android.util.Log.d("NotificationGroupViewHolder", "🔔 Notification group clicked: ${group.childName}")
                onGroupClick(group)
            }

            android.util.Log.d("NotificationGroupViewHolder", "✅ Notification group binding complete: ${group.childName}")
        }

        private fun getAlertTitle(reason: String, count: Int): String {
            return when {
                reason.contains("hate", ignoreCase = true) ||
                        reason.contains("harmful", ignoreCase = true) ||
                        reason.contains("insult", ignoreCase = true) ||
                        reason.contains("toxicity", ignoreCase = true) -> "Problematic Content"
                reason.contains("exclusion", ignoreCase = true) -> "Potential Signs Of Social Exclusion"
                reason.contains("verbal", ignoreCase = true) -> "Verbal Bullying"
                reason.contains("no issue", ignoreCase = true) -> "No Issues Detected"
                else -> "Alert Notification"
            }
        }

        private fun getAlertDescription(reason: String, count: Int, childName: String): String {
            return when {
                reason.contains("hate", ignoreCase = true) ||
                        reason.contains("harmful", ignoreCase = true) ||
                        reason.contains("insult", ignoreCase = true) ||
                        reason.contains("toxicity", ignoreCase = true) ->
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

    // ✅ ViewHolder for individual alerts
    class AlertViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val title = itemView.findViewById<TextView>(R.id.alertTitle)
        private val reason = itemView.findViewById<TextView>(R.id.alertReason)
        private val time = itemView.findViewById<TextView>(R.id.alertTime)
        private val childProfileImage = itemView.findViewById<ShapeableImageView>(R.id.childProfileImage)
        private val unreadIndicator = itemView.findViewById<View>(R.id.unreadIndicator)

        fun bind(
            alert: Alert,
            childDataMap: Map<String, ChildLocalData>,
            onClick: (Alert) -> Unit,
            onMarkAsRead: (Alert) -> Unit,
            showOnlyReadAlerts: Boolean
        ) {
            android.util.Log.d("AlertViewHolder", "📄 Binding alert ${alert.postId} (read: ${alert.isRead})")

            val displayTime = if (alert.timestamp < 1000000000000L) {
                alert.timestamp * 1000
            } else {
                alert.timestamp
            }

            val childData = childDataMap[alert.reporterId]
            val childName = childData?.name ?: alert.reporterId

            // ✅ Set core text values
            title.text = "Child: $childName"
            reason.text = alert.reason
            time.text = DateUtils.getRelativeTimeSpanString(
                displayTime,
                System.currentTimeMillis(),
                DateUtils.MINUTE_IN_MILLIS
            )

            // ✅ For read alerts in alerts fragment - make them slightly dimmed but still readable
            val isUnread = !alert.isRead

            if (isUnread) {
                // ✅ This should not happen in alerts fragment since unread alerts are in notification group
                android.util.Log.w("AlertViewHolder", "⚠️ Found unread alert as individual item: ${alert.postId}")
                unreadIndicator?.visibility = View.VISIBLE
                itemView.isSelected = true
                itemView.alpha = 1.0f

                itemView.setOnClickListener {
                    onClick(alert)
                    onMarkAsRead(alert)
                }
            } else {
                // ✅ Read alerts - show as slightly dimmed but still readable
                android.util.Log.d("AlertViewHolder", "📖 Setting up read alert: ${alert.postId}")
                unreadIndicator?.visibility = View.GONE
                itemView.isSelected = false
                itemView.alpha = 0.8f // ✅ Slightly dimmed but still readable

                // ✅ Make clickable for read alerts too (for navigation to details)
                itemView.setOnClickListener {
                    onClick(alert)
                }
                itemView.isClickable = true
                itemView.isFocusable = true
            }

            // ✅ Set profile image safely
            if (!childData?.imageUrl.isNullOrEmpty()) {
                Picasso.get()
                    .load(childData.imageUrl)
                    .placeholder(R.drawable.ic_default_profile)
                    .error(R.drawable.ic_default_profile)
                    .into(childProfileImage)
            } else {
                childProfileImage.setImageResource(R.drawable.ic_default_profile)
            }

            android.util.Log.d("AlertViewHolder", "✅ Alert binding complete: ${alert.postId}, clickable=${itemView.isClickable}")
        }
    }
}

// ✅ DiffCallback for mixed list
class MixedDiffCallback : DiffUtil.ItemCallback<Any>() {
    override fun areItemsTheSame(oldItem: Any, newItem: Any): Boolean {
        return when {
            oldItem is NotificationGroup && newItem is NotificationGroup ->
                oldItem.childId == newItem.childId
            oldItem is Alert && newItem is Alert ->
                oldItem.postId == newItem.postId
            else -> false
        }
    }

    override fun areContentsTheSame(oldItem: Any, newItem: Any): Boolean {
        return when {
            oldItem is NotificationGroup && newItem is NotificationGroup ->
                oldItem == newItem
            oldItem is Alert && newItem is Alert ->
                oldItem == newItem
            else -> false
        }
    }
}
//package com.example.antibully.data.ui.adapters
//
//import android.text.format.DateUtils
//import android.view.LayoutInflater
//import android.view.View
//import android.view.ViewGroup
//import android.widget.TextView
//import androidx.recyclerview.widget.DiffUtil
//import androidx.recyclerview.widget.ListAdapter
//import androidx.recyclerview.widget.RecyclerView
//import com.example.antibully.R
//import com.example.antibully.data.models.Alert
//import com.example.antibully.data.models.ChildLocalData
//import com.google.android.material.imageview.ShapeableImageView
//import com.squareup.picasso.Picasso
//
//class AlertsAdapter(
//    private val childDataMap: Map<String, ChildLocalData>,
//    private val onAlertClick: (Alert) -> Unit
//) : ListAdapter<Alert, AlertsAdapter.AlertViewHolder>(AlertDiffCallback()) {
//
//    class AlertViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
//        fun bind(alert: Alert, childDataMap: Map<String, ChildLocalData>, onClick: (Alert) -> Unit) {
//            val title = itemView.findViewById<TextView>(R.id.alertTitle)
//            val reason = itemView.findViewById<TextView>(R.id.alertReason)
//            val time = itemView.findViewById<TextView>(R.id.alertTime)
//            val childProfileImage = itemView.findViewById<ShapeableImageView>(R.id.childProfileImage)
//
//            val displayTime = if (alert.timestamp < 1000000000000L) {
//                alert.timestamp * 1000
//            } else {
//                alert.timestamp
//            }
//
//            val childData = childDataMap[alert.reporterId]
//            val childName = childData?.name ?: alert.reporterId
//
//            // ✅ Set core text values
//            title.text = "Child: $childName"
//            reason.text = alert.reason
//            time.text = DateUtils.getRelativeTimeSpanString(
//                displayTime,
//                System.currentTimeMillis(),
//                DateUtils.MINUTE_IN_MILLIS
//            )
//
//            // ✅ Set profile image safely
//            if (!childData?.imageUrl.isNullOrEmpty()) {
//                Picasso.get()
//                    .load(childData.imageUrl)
//                    .placeholder(R.drawable.ic_default_profile)
//                    .error(R.drawable.ic_default_profile)
//                    .into(childProfileImage)
//            } else {
//                childProfileImage.setImageResource(R.drawable.ic_default_profile)
//            }
//
//            itemView.setOnClickListener { onClick(alert) }
//        }
//
//    }
////        fun bind(alert: Alert, childDataMap: Map<String, ChildLocalData>, onClick: (Alert) -> Unit) {
////            val title = itemView.findViewById<TextView>(R.id.alertTitle)
////            val reason = itemView.findViewById<TextView>(R.id.alertReason)
////            val time = itemView.findViewById<TextView>(R.id.alertTime)
////            val childProfileImage = itemView.findViewById<ShapeableImageView>(R.id.childProfileImage)
////
////            val displayTime = if (alert.timestamp < 1000000000000L) {
////                alert.timestamp * 1000
////            } else {
////                alert.timestamp
////            }
////
////            val childData = childDataMap[alert.reporterId]
////            val childName = childData?.name ?: alert.reporterId
////
////            // ✅ Set core text values
////            title.text = "Child: $childName"
////            reason.text = alert.reason
////            time.text = DateUtils.getRelativeTimeSpanString(
////                displayTime,
////                System.currentTimeMillis(),
////                DateUtils.MINUTE_IN_MILLIS
////            )
////
////            // ✅ Set profile image (if available)
////            childData?.imageUrl?.let { imageUrl ->
////                Picasso.get()
////                    .load(imageUrl)
////                    .placeholder(R.drawable.ic_default_profile)
////                    .error(R.drawable.ic_default_profile)
////                    .into(childProfileImage)
////            } ?: run {
////                childProfileImage.setImageResource(R.drawable.ic_default_profile)
////            }
////
////            itemView.setOnClickListener { onClick(alert) }
////        }
////
////    }
//    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AlertViewHolder {
//        val view = LayoutInflater.from(parent.context)
//            .inflate(R.layout.item_alert, parent, false)
//        return AlertViewHolder(view)
//    }
//
//    override fun onBindViewHolder(holder: AlertViewHolder, position: Int) {
//        holder.bind(getItem(position), childDataMap, onAlertClick)
//    }
//}
//
//class AlertDiffCallback : DiffUtil.ItemCallback<Alert>() {
//    override fun areItemsTheSame(oldItem: Alert, newItem: Alert): Boolean {
//        return oldItem.postId == newItem.postId
//    }
//
//    override fun areContentsTheSame(oldItem: Alert, newItem: Alert): Boolean {
//        return oldItem == newItem
//    }
//}