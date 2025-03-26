package com.example.antibully.data.ui

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.content.res.AppCompatResources
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController
import com.example.antibully.R
import com.example.antibully.data.db.AppDatabase
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.appbar.MaterialToolbar
import com.google.firebase.auth.FirebaseAuth
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavController
import androidx.navigation.ui.NavigationUI
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private lateinit var navController: NavController
    private lateinit var bottomNav: BottomNavigationView
    private lateinit var topNav: MaterialToolbar

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.nav_host_fragment) as NavHostFragment

        navController = navHostFragment.navController
        bottomNav = findViewById(R.id.bottom_nav_menu)
        topNav = findViewById(R.id.top_nav_bar)

        // Set the toolbar as the ActionBar
        setSupportActionBar(topNav)
        NavigationUI.setupActionBarWithNavController(this, navController)

        // Setup bottom nav
        bottomNav.setupWithNavController(navController)

        // Make top nav always visible


        navController.addOnDestinationChangedListener { _, destination, _ ->

            val hideTopNav = setOf(
                R.id.loginFragment,
                R.id.signUpFragment,
                R.id.splashFragment
            )


            // Fragments where we show BACK ARROW + TITLE
            val showBackAndTitle = setOf(
                R.id.editProfileFragment,
                R.id.alertDetailsFragment,
                R.id.addChildFragment
            )

            // Fragments where we HIDE the BOTTOM NAV
            val hideBottomNav = setOf(
                R.id.loginFragment,
                R.id.signUpFragment,
                R.id.splashFragment,
                R.id.editProfileFragment,
                R.id.alertDetailsFragment,
                R.id.addChildFragment
            )

            // Bottom nav visibility
            bottomNav.visibility = if (destination.id in hideBottomNav) View.GONE else View.VISIBLE
            topNav.visibility = if (destination.id in hideTopNav) View.GONE else View.VISIBLE

            // Top nav behavior
            if (destination.id in showBackAndTitle) {
                supportActionBar?.setDisplayHomeAsUpEnabled(true)
                supportActionBar?.setDisplayShowTitleEnabled(true)

                val title = when (destination.id) {
                    R.id.editProfileFragment -> "Edit Profile"
                    R.id.alertDetailsFragment -> "Alert Details"
                    R.id.addChildFragment -> "Add Child"
                    else -> ""
                }
                supportActionBar?.title = title

                // 🔥 Remove logo
                supportActionBar?.setLogo(null)

            } else {
                supportActionBar?.setDisplayHomeAsUpEnabled(false)
                supportActionBar?.setDisplayShowTitleEnabled(false)
                supportActionBar?.title = ""

                // 🔥 Show logo
                supportActionBar?.setLogo(R.drawable.ic_logo_text_only)
            }

        }
        bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.logout -> {
                    FirebaseAuth.getInstance().signOut()
                    lifecycleScope.launch(Dispatchers.IO) {
                        AppDatabase.getDatabase(this@MainActivity).userDao().deleteAllUsers()
                    }
                    navController.navigate(R.id.loginFragment)
                    true
                }

                else -> {
                    navController.navigate(item.itemId)
                    true
                }
            }
        }


        }
    override fun onSupportNavigateUp(): Boolean {
        return navController.navigateUp() || super.onSupportNavigateUp()
    }
}