package com.example.antibully.data.ui.settings

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.browser.customtabs.CustomTabsIntent
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.antibully.R
import com.example.antibully.databinding.FragmentSettingsBinding
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.squareup.picasso.Picasso
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class SettingsFragment : Fragment() {

    private var _binding: FragmentSettingsBinding? = null
    private val binding get() = _binding!!

    // Toggle this if you decide to hide Add Child from the Settings screen
    private val showAddChildRow = true

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSettingsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.settingsLoading.visibility = View.VISIBLE
        binding.settingsContentContainer.visibility = View.GONE

        // Load header (name/email/avatar)
        MainScope().launch {
            bindUserHeader()
            // After loading is done, show content and hide loader
            binding.settingsLoading.visibility = View.GONE
            binding.settingsContentContainer.visibility = View.VISIBLE
        }

        // ===== Edit Profile =====
        binding.rowEditProfile.icon.setImageResource(R.drawable.ic_edit)
        binding.rowEditProfile.title.text = getString(R.string.settings_edit_profile)
        binding.rowEditProfile.root.setOnClickListener {
            findNavController().navigate(R.id.action_settingsFragment_to_editProfileFragment)
        }

        // ===== Discord (OAuth + Bot Invite) =====
        // Use an icon that exists in your project:
        binding.rowDiscord.icon.setImageResource(R.drawable.ic_discord_hollow)
        binding.rowDiscord.title.text = getString(R.string.settings_connect_discord)
        binding.rowDiscord.root.setOnClickListener { findNavController().navigate(R.id.connectDiscordFragment) }

        // ===== Add Child (optional) =====
        binding.rowAddChild.root.isVisible = showAddChildRow
        if (showAddChildRow) {
            binding.rowAddChild.icon.setImageResource(R.drawable.ic_add_24dp)
            binding.rowAddChild.title.text = getString(R.string.settings_add_child)
            binding.rowAddChild.root.setOnClickListener {
                openDiscordOAuthViaBackend()
            }
        }

        // ===== Security (2FA) =====
        binding.rowSecurity.icon.setImageResource(R.drawable.ic_security)
        binding.rowSecurity.title.text = getString(R.string.settings_security)
        binding.rowSecurity.root.setOnClickListener {
            findNavController().navigate(R.id.action_settingsFragment_to_securitySettingsFragment)
        }

        // ===== Privacy =====
        binding.rowPrivacy.icon.setImageResource(R.drawable.ic_privacy)
        binding.rowPrivacy.title.text = getString(R.string.settings_privacy)
        binding.rowPrivacy.root.setOnClickListener {
            // findNavController().navigate(R.id.action_settingsFragment_to_privacyFragment)
        }

        // ===== Help & Support =====
        binding.rowHelp.icon.setImageResource(R.drawable.ic_help)
        binding.rowHelp.title.text = getString(R.string.settings_help_support)
        binding.rowHelp.root.setOnClickListener {
            // findNavController().navigate(R.id.action_settingsFragment_to_helpFragment)
        }
    }

    private suspend fun bindUserHeader() {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
        val doc = FirebaseFirestore.getInstance().collection("users").document(uid).get().await()
        val name = doc.getString("fullName") ?: ""
        val email = FirebaseAuth.getInstance().currentUser?.email ?: ""
        val profileUrl = doc.getString("profileImageUrl")

        binding.tvName.text = name
        binding.tvEmail.text = email
        if (!profileUrl.isNullOrEmpty()) {
            Picasso.get().load(profileUrl).into(binding.imgAvatar)
        }
    }

    private fun showDiscordActions() {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(getString(R.string.settings_connect_discord))
            .setItems(
                arrayOf(
                    getString(R.string.settings_connect_discord_account),
                    getString(R.string.settings_add_bot_to_server)
                )
            ) { _, which ->
                when (which) {
                    0 -> openDiscordOAuthViaBackend()
                    1 -> openBotInvite()
                }
            }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }

    private fun openDiscordOAuthViaBackend() {
            val oauthUrl = "https://discord.com/oauth2/authorize?client_id=1373612221166391397&response_type=code&redirect_uri=http%3A%2F%2F10.0.2.2%3A3000%2Fapi%2Foauth%2Fdiscord%2Fcallback&scope=identify"
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(oauthUrl))
            startActivity(intent)
    }

    private fun openBotInvite() {
        val invite =
            "https://discord.com/api/oauth2/authorize?client_id=1373612221166391397&permissions=0&scope=bot%20applications.commands"
        startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(invite)))
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
