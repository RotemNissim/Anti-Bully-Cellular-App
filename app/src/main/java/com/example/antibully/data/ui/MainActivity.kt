package com.example.antibully.data.ui

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController
import com.example.antibully.R
import com.example.antibully.data.db.AppDatabase
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.appbar.MaterialToolbar
import com.google.firebase.auth.FirebaseAuth
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavController
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private lateinit var navController: NavController
    private lateinit var bottomNav: BottomNavigationView
    private lateinit var topAppBar: MaterialToolbar

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        navController = navHostFragment.navController
        bottomNav = findViewById(R.id.bottom_nav_menu)
        topAppBar = findViewById(R.id.top_app_bar)

        bottomNav.setupWithNavController(navController)

        // 🔄 Handle visibility and toolbar config
        navController.addOnDestinationChangedListener { _, destination, _ ->
            when (destination.id) {
                R.id.loginFragment, R.id.signUpFragment, R.id.alertDetailsFragment, R.id.splashFragment -> {
                    bottomNav.visibility = View.GONE
                    topAppBar.visibility = View.GONE
                }

                else -> {
                    bottomNav.visibility = View.VISIBLE
                    topAppBar.visibility = View.VISIBLE
                    topAppBar.setNavigationIcon(R.drawable.ic_back_arrow)
                    topAppBar.setNavigationOnClickListener {
                        navController.navigateUp()
                    }
                    topAppBar.title = destination.label
                }
            }
        }

        // 🚪 Handle logout action
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
}
