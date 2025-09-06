package com.example.antibully.data.ui.auth

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.example.antibully.R
import com.example.antibully.data.db.AppDatabase
import com.example.antibully.utils.SessionManager
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

            val isLoggedInSession = SessionManager.isLoggedIn(requireContext())
            val sessionUserId = SessionManager.getCurrentUserId(requireContext())

            delay(1000)

            withContext(Dispatchers.Main) {
                if (currentUser != null && userInRoom != null && isLoggedInSession && sessionUserId != null) {
                    Log.d("SplashFragment", "User is fully logged in - navigating to feed")
                    findNavController().navigate(R.id.action_splashFragment_to_feedFragment)
                } else {
                    Log.d("SplashFragment", "User not logged in - navigating to login")
                    // Clear any inconsistent session data
                    if (currentUser == null) {
                        SessionManager.logout(requireContext())
                    }
                    findNavController().navigate(R.id.action_splashFragment_to_loginFragment)
                }
            }
        }
    }
}