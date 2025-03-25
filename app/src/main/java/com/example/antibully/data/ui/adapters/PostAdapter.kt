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
import com.example.antibully.data.models.Post
import com.example.antibully.data.models.User
import com.google.android.material.imageview.ShapeableImageView
import com.squareup.picasso.Picasso

class PostAdapter(
    private val userMap: Map<String, User>  = emptyMap() // user.firebaseId -> User(name, profilePicUrl)
) : ListAdapter<Post, PostAdapter.ViewHolder>(PostDiffCallback()) {

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        fun bind(post: Post, user: User?) {
            itemView.findViewById<TextView>(R.id.commentText).text = post.text
            itemView.findViewById<TextView>(R.id.commentTimestamp).text =
                DateUtils.getRelativeTimeSpanString(
                    post.timestamp,
                    System.currentTimeMillis(),
                    DateUtils.MINUTE_IN_MILLIS
                )

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
        }
    }

    class PostDiffCallback : DiffUtil.ItemCallback<Post>() {
        override fun areItemsTheSame(oldItem: Post, newItem: Post) = oldItem.firebaseId == newItem.firebaseId
        override fun areContentsTheSame(oldItem: Post, newItem: Post) = oldItem == newItem
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_post, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val post = getItem(position)
        val user = userMap[post.userId]  // Make sure Post has this field!
        holder.bind(post, user)
    }
}
