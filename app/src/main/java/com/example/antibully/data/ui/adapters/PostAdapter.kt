package com.example.antibully.data.ui.adapters

import android.text.format.DateUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.PopupMenu
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.antibully.R
import com.example.antibully.data.models.Post
import com.example.antibully.data.models.User
import com.google.android.material.imageview.ShapeableImageView
import com.squareup.picasso.Picasso

class PostAdapter(
    private val userMap: Map<String, User>  = emptyMap(),
    private val currentUserId: String,
    private val onEditClick: (Post) -> Unit,
    private val onDeleteClick: (Post) -> Unit
) : ListAdapter<Post, PostAdapter.ViewHolder>(PostDiffCallback()) {

    class ViewHolder(itemView: View,
                     private val currentUserId: String,
                     private val onEditClick: (Post) -> Unit,
                     private val onDeleteClick: (Post) -> Unit)
        : RecyclerView.ViewHolder(itemView) {

        fun bind(post: Post, user: User?) {
            itemView.findViewById<TextView>(R.id.commentText).text = post.text
            itemView.findViewById<TextView>(R.id.commentTimestamp).text =
                DateUtils.getRelativeTimeSpanString(
                    post.timestamp,
                    System.currentTimeMillis(),
                    DateUtils.MINUTE_IN_MILLIS
                )
            val moreButton = itemView.findViewById<ImageView>(R.id.moreOptionsButton)

            if (post.userId == currentUserId) {
                moreButton.visibility = View.VISIBLE
                moreButton.setOnClickListener {
                    val popup = PopupMenu(itemView.context, moreButton)
                    popup.menuInflater.inflate(R.menu.comment_menu, popup.menu)
                    popup.setOnMenuItemClickListener {
                        when (it.itemId) {
                            R.id.menu_edit -> {
                                onEditClick(post)
                                true
                            }
                            R.id.menu_delete -> {
                                onDeleteClick(post)
                                true
                            }
                            else -> false
                        }
                    }
                    popup.show()
                }
            } else {
                moreButton.visibility = View.GONE
            }

            val nameView = itemView.findViewById<TextView>(R.id.commentAuthor)
            val imageView = itemView.findViewById<ShapeableImageView>(R.id.commentProfileImage)

            nameView.text = user?.name ?: "Unknown"

            if (!user?.localProfileImagePath.isNullOrEmpty()) {
                Picasso.get()
                    .load(user?.localProfileImagePath)
                    .placeholder(R.drawable.ic_default_profile)
                    .error(R.drawable.ic_default_profile)
                    .into(imageView)
            } else {
                imageView.setImageResource(R.drawable.ic_default_profile)
            }

            val commentImageView = itemView.findViewById<ImageView>(R.id.commentImage)

            if (!post.imageUrl.isNullOrEmpty()) {
                commentImageView.visibility = View.VISIBLE
                Picasso.get()
                    .load(post.imageUrl)
                    .into(commentImageView)
            } else {
                commentImageView.visibility = View.GONE
            }

        }
    }

    class PostDiffCallback : DiffUtil.ItemCallback<Post>() {
        override fun areItemsTheSame(oldItem: Post, newItem: Post) = oldItem.firebaseId == newItem.firebaseId
        override fun areContentsTheSame(oldItem: Post, newItem: Post) = oldItem == newItem
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_post, parent, false)
        return ViewHolder(view, currentUserId, onEditClick, onDeleteClick)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val post = getItem(position)
        val user = userMap[post.userId]
        holder.bind(post, user)
    }
}
