package com.example.antibully.data.ui

import android.content.Intent
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
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import android.widget.Toast
import java.io.IOException

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
                R.id.addChildFragment,
                R.id.editChildFragment
            )

            // Fragments where we HIDE the BOTTOM NAV
            val hideBottomNav = setOf(
                R.id.loginFragment,
                R.id.signUpFragment,
                R.id.splashFragment,
                R.id.editProfileFragment,
                R.id.alertDetailsFragment,
                R.id.addChildFragment,
                R.id.editChildFragment
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
                    R.id.editChildFragment -> "Edit Child"
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
                supportActionBar?.setLogo(R.drawable.untitled)
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
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleDiscordOAuthCode(intent)
    }

    private fun handleDiscordOAuthCode(intent: Intent?) {
        android.util.Log.d("OAuth", "handleDiscordOAuthCode triggered")

        intent?.data?.let { uri ->
            android.util.Log.d("OAuth", "Intent data URI: $uri")

            if (uri.scheme == "antibully" && uri.host == "discord-callback") {
                val code = uri.getQueryParameter("code")
                android.util.Log.d("OAuth", "Extracted code: $code")

                sendCodeToServer(code)
            } else {
                android.util.Log.w("OAuth", "URI doesn't match expected scheme/host")
            }
        }
    }

//
//    private fun sendCodeToServer(code: String?) {
//        if (code.isNullOrEmpty()) return
//
//        val requestBody = JSONObject()
//        requestBody.put("code", code)
//
//        val request = Request.Builder()
//            .url("http://10.0.2.2:3000/api/oauth/discord/exchange") // אמולטור
//            .post(
//                requestBody.toString()
//                    .toRequestBody("application/json".toMediaTypeOrNull())
//            )
//            .build()
//
//        OkHttpClient().newCall(request).enqueue(object : Callback {
//            override fun onFailure(call: Call, e: IOException) {
//                android.util.Log.e("OAuth", "Request failed: ${e.message}")
//            }
//
//            override fun onResponse(call: Call, response: Response) {
//                val body = response.body?.string()
//                android.util.Log.d("OAuth", "Server response: $body")
//
//                if (response.isSuccessful) {
//                    val discordId = JSONObject(body).optString("id")
//                    runOnUiThread {
//                        Toast.makeText(this@MainActivity, "Connected to Discord ID: $discordId", Toast.LENGTH_LONG).show()
//                        // כאן תוכלי לשמור את ה־discordId או להעביר ל־ViewModel
//                    }
//                }
//            }
//        })
//    }
private fun sendCodeToServer(code: String?) {
    if (code.isNullOrEmpty()) {
        android.util.Log.e("OAuth", "Code is null or empty – aborting request")
        return
    }

    android.util.Log.d("OAuth", "Sending code to backend: $code")

    val requestBody = JSONObject().apply {
        put("code", code)
    }

    val request = Request.Builder()
        .url("http://10.0.2.2:3000/api/oauth/discord/exchange")
        .post(
            requestBody.toString()
                .toRequestBody("application/json".toMediaTypeOrNull())
        )
        .build()

    OkHttpClient().newCall(request).enqueue(object : Callback {
        override fun onFailure(call: Call, e: IOException) {
            android.util.Log.e("OAuth", "Network failure: ${e.message}")
        }

        override fun onResponse(call: Call, response: Response) {
            val body = response.body?.string()
            android.util.Log.d("OAuth", "Backend response status: ${response.code}")
            android.util.Log.d("OAuth", "Backend response body: $body")

            if (response.isSuccessful) {
                val discordId = JSONObject(body ?: "{}").optString("id")
                runOnUiThread {
                    Toast.makeText(this@MainActivity, "Connected to Discord ID: $discordId", Toast.LENGTH_LONG).show()
                }
            } else {
                android.util.Log.e("OAuth", "Request failed with status: ${response.code}")
            }
        }
    })
}


    override fun onSupportNavigateUp(): Boolean {
        return navController.navigateUp() || super.onSupportNavigateUp()
    }
}