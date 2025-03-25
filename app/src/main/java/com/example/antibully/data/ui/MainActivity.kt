package com.example.antibully.data.ui

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController
import com.example.antibully.R
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.appbar.MaterialToolbar


class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        val navController = navHostFragment.navController
        val bottomNav = findViewById<BottomNavigationView>(R.id.bottom_nav_menu)
        val topAppBar = findViewById<MaterialToolbar>(R.id.top_app_bar)


        bottomNav.setupWithNavController(navController)

        navController.addOnDestinationChangedListener { _, destination, _ ->
            when (destination.id) {
                R.id.loginFragment, R.id.signUpFragment, R.id.alertDetailsFragment -> {
                    bottomNav.visibility = View.GONE
                    topAppBar.visibility = View.GONE

                }
                else ->{
                    bottomNav.visibility = View.VISIBLE
                    topAppBar.visibility = View.VISIBLE
                    topAppBar.setNavigationIcon(R.drawable.ic_back_arrow) // Replace with your icon
                    topAppBar.setNavigationOnClickListener {
                        navController.navigateUp()
                    }
                    topAppBar.title = destination.label
                }
            }
        }


    }
}