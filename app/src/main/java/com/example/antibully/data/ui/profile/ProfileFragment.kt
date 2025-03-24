package com.example.antibully.data.ui.profile

import android.net.Uri
import android.os.Bundle
import android.view.*
import android.widget.*
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.antibully.R
import com.example.antibully.data.db.AppDatabase
import com.example.antibully.data.models.ChildLocalData
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ProfileFragment : Fragment() {

    private lateinit var auth: FirebaseAuth
    private lateinit var userDao: com.example.antibully.data.db.dao.UserDao
    private lateinit var childDao: com.example.antibully.data.db.dao.ChildDao

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = inflater.inflate(R.layout.fragment_profile, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        auth = FirebaseAuth.getInstance()
        userDao = AppDatabase.getDatabase(requireContext()).userDao()
        childDao = AppDatabase.getDatabase(requireContext()).childDao()

        val profileImageView = view.findViewById<ImageView>(R.id.ivProfileImage)
        val usernameTextView = view.findViewById<TextView>(R.id.tvUsername)
        val editProfileButton = view.findViewById<Button>(R.id.btnEditProfile)
        val addChildButton = view.findViewById<Button>(R.id.btnAddChild)
        val recyclerView = view.findViewById<RecyclerView>(R.id.rvChildren)
        val noChildrenText = view.findViewById<TextView>(R.id.tvNoChildren) // נדרש ב-XML

        val userId = auth.currentUser?.uid ?: return

        lifecycleScope.launch(Dispatchers.IO) {
            val localUser = userDao.getUserById(userId)
            val children = childDao.getChildrenForUser(userId)

            withContext(Dispatchers.Main) {
                if (localUser != null) {
                    usernameTextView.text = localUser.name
                    if (localUser.localProfileImagePath.isNotEmpty()) {
                        profileImageView.setImageURI(Uri.parse(localUser.localProfileImagePath))
                    }
                }

                recyclerView.layoutManager = LinearLayoutManager(requireContext())
                recyclerView.adapter = ChildrenAdapter(children)

                // הצגת טקסט אם אין ילדים
                noChildrenText.visibility = if (children.isEmpty()) View.VISIBLE else View.GONE
            }
        }

        editProfileButton.setOnClickListener {
            findNavController().navigate(R.id.action_profileFragment_to_editProfileFragment)
        }

        addChildButton.setOnClickListener {
            findNavController().navigate(R.id.action_profileFragment_to_addChildFragment)
        }
    }

    private inner class ChildrenAdapter(private val children: List<ChildLocalData>) :
        RecyclerView.Adapter<ChildrenAdapter.ChildViewHolder>() {

        inner class ChildViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            val childImage: ImageView = itemView.findViewById(R.id.ivChildImage)
            val childIdText: TextView = itemView.findViewById(R.id.tvChildId)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ChildViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_child_card, parent, false)
            return ChildViewHolder(view)
        }

        override fun onBindViewHolder(holder: ChildViewHolder, position: Int) {
            val child = children[position]
            holder.childIdText.text = child.childId
            holder.childImage.setImageURI(Uri.parse(child.localImagePath))
        }

        override fun getItemCount() = children.size
    }
}
