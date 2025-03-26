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
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ProfileFragment : Fragment() {

    private lateinit var auth: FirebaseAuth
    private lateinit var userDao: com.example.antibully.data.db.dao.UserDao
    private lateinit var childDao: com.example.antibully.data.db.dao.ChildDao
    private lateinit var recyclerView: RecyclerView
    private lateinit var noChildrenText: TextView

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
        val editProfileButton = view.findViewById<FloatingActionButton>(R.id.btnEditProfile)
        val addChildButton = view.findViewById<Button>(R.id.btnAddChild)
        recyclerView = view.findViewById(R.id.rvChildren)
        noChildrenText = view.findViewById(R.id.tvNoChildren)

        val userId = auth.currentUser?.uid ?: return

        loadChildren(userId, usernameTextView, profileImageView)

        editProfileButton.setOnClickListener {
            findNavController().navigate(R.id.action_profileFragment_to_editProfileFragment)
        }

        addChildButton.setOnClickListener {
            findNavController().navigate(R.id.action_profileFragment_to_addChildFragment)
        }
    }

    private fun loadChildren(userId: String, usernameView: TextView, imageView: ImageView) {
        lifecycleScope.launch(Dispatchers.IO) {
            val localUser = userDao.getUserById(userId)
            val children = childDao.getChildrenForUser(userId)

            withContext(Dispatchers.Main) {
                localUser?.let {
                    usernameView.text = it.name
                    if (it.localProfileImagePath.isNotEmpty()) {
                        imageView.setImageURI(Uri.parse(it.localProfileImagePath))
                    }
                }

                recyclerView.layoutManager = LinearLayoutManager(requireContext())
                recyclerView.adapter = ChildrenAdapter(children)

                noChildrenText.visibility = if (children.isEmpty()) View.VISIBLE else View.GONE
            }
        }
    }

    private inner class ChildrenAdapter(private var children: List<ChildLocalData>) :
        RecyclerView.Adapter<ChildrenAdapter.ChildViewHolder>() {

        inner class ChildViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            val childImage: ImageView = itemView.findViewById(R.id.ivChildImage)
            val childIdText: TextView = itemView.findViewById(R.id.tvChildId)
            val childNameText: TextView = itemView.findViewById(R.id.tvChildName)
            val editButton: ImageButton = itemView.findViewById(R.id.btnEditChild)
            val deleteButton: ImageButton = itemView.findViewById(R.id.btnDeleteChild)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ChildViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_child_card, parent, false)
            return ChildViewHolder(view)
        }

        override fun onBindViewHolder(holder: ChildViewHolder, position: Int) {
            val child = children[position]
            holder.childIdText.text = "ID: ${child.childId}"
            holder.childNameText.text = child.name
            holder.childImage.setImageURI(Uri.parse(child.localImagePath))

            holder.editButton.setOnClickListener {
                val action = ProfileFragmentDirections.actionProfileFragmentToEditChildFragment(child.childId)
                findNavController().navigate(action)
            }

            holder.deleteButton.setOnClickListener {
                lifecycleScope.launch(Dispatchers.IO) {
                    childDao.deleteChild(child.childId, child.parentUserId)
                    val updated = childDao.getChildrenForUser(child.parentUserId)
                    withContext(Dispatchers.Main) {
                        updateData(updated)
                        noChildrenText.visibility = if (updated.isEmpty()) View.VISIBLE else View.GONE
                        Toast.makeText(requireContext(), "Child deleted", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }

        override fun getItemCount() = children.size

        fun updateData(newChildren: List<ChildLocalData>) {
            children = newChildren
            notifyDataSetChanged()
        }
    }

    private fun refreshChildrenList() {
        val userId = auth.currentUser?.uid ?: return
        lifecycleScope.launch(Dispatchers.IO) {
            val children = childDao.getChildrenForUser(userId)
            withContext(Dispatchers.Main) {
                recyclerView.adapter = ChildrenAdapter(children)
                noChildrenText.visibility = if (children.isEmpty()) View.VISIBLE else View.GONE
            }
        }
    }

}
