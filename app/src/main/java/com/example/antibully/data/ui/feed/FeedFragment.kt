package com.example.antibully.data.ui.feed

import com.squareup.picasso.Picasso
import android.widget.ImageView
import com.example.antibully.R

// Inside onCreateView or RecyclerView Adapter
fun loadImage(imageUrl: String?, imageView: ImageView) {
    if (!imageUrl.isNullOrEmpty()) {
        Picasso.get()
            .load(imageUrl)
            .placeholder(R.drawable.placeholder_image) // Show while loading
            .error(R.drawable.error_image) // Show if fails
            .into(imageView)
    } else {
        imageView.setImageResource(R.drawable.default_image)
    }
}
