package com.example.antibully.data.ui.common

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.drawable.Drawable
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import com.example.antibully.R
import com.example.antibully.data.models.Alert
import com.example.antibully.data.ui.adapters.AlertsAdapter

object SwipeToDelete {
    fun attach(
        recyclerView: RecyclerView,
        adapter: AlertsAdapter,
        onDelete: (Alert) -> Unit
    ) {
        val icon: Drawable? = ContextCompat.getDrawable(recyclerView.context, R.drawable.ic_delete_24)
        val paint = Paint().apply {
            color = Color.parseColor("#D32F2F")
            isAntiAlias = true
            alpha = 128 // 0..255 => 128 = ~50% שקיפות
        }

        val callback = object : ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT) {
            override fun onMove(
                rv: RecyclerView,
                vh: RecyclerView.ViewHolder,
                target: RecyclerView.ViewHolder
            ) = false

            override fun getSwipeDirs(rv: RecyclerView, vh: RecyclerView.ViewHolder): Int {
                return if (vh is AlertsAdapter.AlertVH) super.getSwipeDirs(rv, vh) else 0
            }

            override fun onSwiped(vh: RecyclerView.ViewHolder, dir: Int) {
                val pos = vh.bindingAdapterPosition
                val alert = adapter.getAlertForPosition(pos) ?: return
                onDelete(alert)
            }

            override fun onChildDraw(
                c: Canvas,
                rv: RecyclerView,
                vh: RecyclerView.ViewHolder,
                dX: Float,
                dY: Float,
                state: Int,
                isActive: Boolean
            ) {
                if (dX < 0 && vh is AlertsAdapter.AlertVH) {
                    val item = vh.itemView
                    val bg = RectF(
                        item.right + dX,
                        item.top.toFloat(),
                        item.right.toFloat(),
                        item.bottom.toFloat()
                    )
                    c.drawRoundRect(bg, 24f, 24f, paint)

                    icon?.let {
                        val m = (item.height - it.intrinsicHeight) / 2
                        val left = item.right - m - it.intrinsicWidth
                        val right = item.right - m
                        val top = item.top + (item.height - it.intrinsicHeight) / 2
                        val bottom = top + it.intrinsicHeight
                        it.setBounds(left, top, right, bottom)
                        it.draw(c)
                    }
                }
                super.onChildDraw(c, rv, vh, dX, dY, state, isActive)
            }
        }

        ItemTouchHelper(callback).attachToRecyclerView(recyclerView)
    }
}
