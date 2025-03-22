package com.example.antibully.data.ui.feed

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.fragment.app.Fragment
import com.example.antibully.R
import com.example.antibully.databinding.FragmentFeedBinding
import com.squareup.picasso.Picasso

class FeedFragment : Fragment() {

    private var _binding: FragmentFeedBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentFeedBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

/**
 * Utility function to load images into an ImageView using Picasso.
 */
fun ImageView.loadImage(imageUrl: String?) {
    if (!imageUrl.isNullOrEmpty()) {
        Picasso.get()
            .load(imageUrl)
            .placeholder(R.drawable.placeholder_image)
            .error(R.drawable.error_image)
            .into(this)
    } else {
        setImageResource(R.drawable.default_image)
    }
}
