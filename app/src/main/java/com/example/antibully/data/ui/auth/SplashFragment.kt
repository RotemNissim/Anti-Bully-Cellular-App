package com.example.antibully.data.ui.auth

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.navigation.NavOptions
import com.example.antibully.R
import com.example.antibully.utils.SessionManager
import com.google.firebase.auth.FirebaseAuth

class SplashFragment : Fragment() {

    private val auth by lazy { FirebaseAuth.getInstance() }
    private var authListener: FirebaseAuth.AuthStateListener? = null
    private var navigated = false

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = inflater.inflate(R.layout.fragment_splash, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val navOptions = NavOptions.Builder()
            .setPopUpTo(R.id.nav_graph, /* inclusive = */ true)
            .build()

        authListener = FirebaseAuth.AuthStateListener { fa ->
            if (navigated) return@AuthStateListener
            val user = fa.currentUser
            Log.d("SplashFragment", "AuthState change. user=${user?.uid}")

            if (user != null) {
                SessionManager.setLoggedIn(requireContext(), true)
                SessionManager.setCurrentUserId(requireContext(), user.uid)

                findNavController().navigate(R.id.feedFragment, null, navOptions)
            } else {
                // Do NOT call SessionManager.logout() here (startup race).
                findNavController().navigate(R.id.loginFragment, null, navOptions)
            }
            navigated = true
        }
    }

    override fun onStart() {
        super.onStart()
        authListener?.let { auth.addAuthStateListener(it) }
    }

    override fun onStop() {
        authListener?.let { auth.removeAuthStateListener(it) }
        super.onStop()
    }
}
