package com.example.antibully.data.ui

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.View
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavController
import androidx.navigation.NavOptions
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.NavigationUI
import androidx.navigation.ui.setupWithNavController
import com.example.antibully.R
import com.example.antibully.data.db.AppDatabase
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.IOException

class MainActivity : AppCompatActivity() {

    private lateinit var navController: NavController
    private lateinit var bottomNav: BottomNavigationView
    private lateinit var topNav: MaterialToolbar

    private val auth by lazy { FirebaseAuth.getInstance() }
    private val authStateListener = FirebaseAuth.AuthStateListener { fa ->
        val user = fa.currentUser
        val dest = navController.currentDestination?.id
        if (user == null && dest != R.id.loginFragment && dest != R.id.splashFragment) {
            val opts = NavOptions.Builder()
                .setPopUpTo(R.id.nav_graph, /* inclusive = */ true)
                .build()
            navController.navigate(R.id.loginFragment, null, opts)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.nav_host_fragment) as NavHostFragment

        navController = navHostFragment.navController
        bottomNav = findViewById(R.id.bottom_nav_menu)
        topNav = findViewById(R.id.top_nav_bar)

        setSupportActionBar(topNav)
        NavigationUI.setupActionBarWithNavController(this, navController)
        bottomNav.setupWithNavController(navController)

        navController.addOnDestinationChangedListener { _, destination, _ ->
            val hideTopNav = setOf(
                R.id.loginFragment,
                R.id.signUpFragment,
                R.id.splashFragment
            )

            val showBackAndTitle = setOf(
                R.id.editProfileFragment,
                R.id.editChildFragment,
                R.id.twoFactorSetupFragment,
                R.id.securitySettingsFragment,
                R.id.settingsFragment,
                R.id.connectDiscordFragment,
                R.id.unreadListFragment
            )

            val hideBottomNav = setOf(
                R.id.loginFragment,
                R.id.signUpFragment,
                R.id.splashFragment,
                R.id.editProfileFragment,
                R.id.editChildFragment,
                R.id.twoFactorSetupFragment,
                R.id.settingsFragment,
                R.id.connectDiscordFragment,
                R.id.unreadListFragment
            )

            bottomNav.visibility = if (destination.id in hideBottomNav) View.GONE else View.VISIBLE
            topNav.visibility = if (destination.id in hideTopNav) View.GONE else View.VISIBLE

            if (destination.id in showBackAndTitle) {
                supportActionBar?.setDisplayHomeAsUpEnabled(true)
                supportActionBar?.setDisplayShowTitleEnabled(true)

                val title = when (destination.id) {
                    R.id.editProfileFragment -> "Edit Profile"
                    R.id.editChildFragment -> "Edit Child"
                    R.id.twoFactorSetupFragment -> "Two-Factor Setup"
                    R.id.settingsFragment -> "Settings"
                    R.id.connectDiscordFragment -> "Connect Discord"
                    R.id.unreadListFragment -> "Unread List"
                    else -> ""
                }
                supportActionBar?.title = title
                supportActionBar?.setLogo(null)
            } else {
                supportActionBar?.setDisplayHomeAsUpEnabled(false)
                supportActionBar?.setDisplayShowTitleEnabled(false)
                supportActionBar?.title = ""
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
                    val navOptions = NavOptions.Builder()
                        .setPopUpTo(R.id.nav_graph, /* inclusive = */ true)
                        .build()
                    navController.navigate(R.id.loginFragment, null, navOptions)
                    true
                }
                else -> {
                    navController.navigate(item.itemId)
                    true
                }
            }
        }

        requestNotificationPermission()
    }

    override fun onStart() {
        super.onStart()
        auth.addAuthStateListener(authStateListener)
    }

    override fun onStop() {
        super.onStop()
        auth.removeAuthStateListener(authStateListener)
    }

    private fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            when {
                ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED -> {
                    Log.d("MainActivity", "✅ Notification permission already granted")
                }
                shouldShowRequestPermissionRationale(Manifest.permission.POST_NOTIFICATIONS) -> {
                    showNotificationPermissionDialog()
                }
                else -> {
                    ActivityCompat.requestPermissions(
                        this,
                        arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                        NOTIFICATION_PERMISSION_REQUEST_CODE
                    )
                }
            }
        } else {
            Log.d("MainActivity", "✅ Notification permission not required for this Android version")
        }
    }

    private fun showNotificationPermissionDialog() {
        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("Enable Notifications")
            .setMessage("This app needs notification permission to alert you about important security events and bullying incidents.")
            .setPositiveButton("Allow") { _, _ ->
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                    NOTIFICATION_PERMISSION_REQUEST_CODE
                )
            }
            .setNegativeButton("Deny") { dialog, _ ->
                dialog.dismiss()
                Log.w("MainActivity", "User denied notification permission")
            }
            .setCancelable(false)
            .show()
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        when (requestCode) {
            NOTIFICATION_PERMISSION_REQUEST_CODE -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Log.d("MainActivity", "✅ Notification permission granted by user")
                    Toast.makeText(this, "Notifications enabled!", Toast.LENGTH_SHORT).show()
                } else {
                    Log.w("MainActivity", "❌ Notification permission denied by user")
                    Toast.makeText(this, "Notifications disabled. You won't receive alerts.", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleDiscordOAuthCode(intent)
    }

    private fun handleDiscordOAuthCode(intent: Intent?) {
        Log.d("OAuth", "handleDiscordOAuthCode triggered")

        intent?.data?.let { uri ->
            Log.d("OAuth", "Intent data URI: $uri")

            if (uri.scheme == "antibully" && uri.host == "discord-callback") {
                val code = uri.getQueryParameter("code")
                Log.d("OAuth", "Extracted code: $code")

                sendCodeToServer(code)
            } else {
                Log.w("OAuth", "URI doesn't match expected scheme/host")
            }
        }
    }

    private fun sendCodeToServer(code: String?) {
        if (code.isNullOrEmpty()) {
            Log.e("OAuth", "Code is null or empty – aborting request")
            return
        }

        Log.d("OAuth", "Sending code to backend: $code")

        val requestBody = JSONObject().apply {
            put("code", code)
        }

        val request = Request.Builder()
            .url("http://193.106.55.138:3000/api/oauth/discord/exchange")
            .post(
                requestBody.toString()
                    .toRequestBody("application/json".toMediaTypeOrNull())
            )
            .build()

        OkHttpClient().newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                Log.e("OAuth", "Network failure: ${e.message}")
            }

            override fun onResponse(call: Call, response: Response) {
                val body = response.body?.string()
                Log.d("OAuth", "Backend response status: ${response.code}")
                Log.d("OAuth", "Backend response body: $body")

                if (response.isSuccessful) {
                    val discordId = JSONObject(body ?: "{}").optString("id")
                    runOnUiThread {
                        Toast.makeText(this@MainActivity, "Connected to Discord ID: $discordId", Toast.LENGTH_LONG).show()
                    }
                } else {
                    Log.e("OAuth", "Request failed with status: ${response.code}")
                }
            }
        })
    }

    override fun onSupportNavigateUp(): Boolean {
        return navController.navigateUp() || super.onSupportNavigateUp()
    }

    companion object {
        private const val NOTIFICATION_PERMISSION_REQUEST_CODE = 1001
    }
}
