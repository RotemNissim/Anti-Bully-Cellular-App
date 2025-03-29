package com.example.antibully.data.ui.auth

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.example.antibully.R
import com.example.antibully.data.db.AppDatabase
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class SplashFragment : Fragment() {

    private lateinit var auth: FirebaseAuth

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? = inflater.inflate(R.layout.fragment_splash, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        auth = FirebaseAuth.getInstance()

        lifecycleScope.launch(Dispatchers.IO) {
            val currentUser = auth.currentUser
            val userDao = AppDatabase.getDatabase(requireContext()).userDao()
            val userInRoom = currentUser?.uid?.let { userDao.getUserById(it) }

            delay(1000) // brief pause for splash feel

            withContext(Dispatchers.Main) {
                if (currentUser != null && userInRoom != null) {
                    findNavController().navigate(R.id.feedFragment)
                } else {
                    findNavController().navigate(R.id.loginFragment)
                }
            }
        }
    }
}