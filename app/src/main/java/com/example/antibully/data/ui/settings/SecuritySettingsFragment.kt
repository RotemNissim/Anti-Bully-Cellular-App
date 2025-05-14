package com.example.antibully.data.ui.settings

import android.os.Bundle
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.example.antibully.R
import com.example.antibully.databinding.FragmentSecuritySettingsBinding
import com.google.android.material.snackbar.Snackbar

class SecuritySettingsFragment : Fragment() {

    private var _binding: FragmentSecuritySettingsBinding? = null
    private val binding get() = _binding!!
    private val viewModel: SecuritySettingsViewModel by viewModels()

    private var userInitiatedChange = false

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSecuritySettingsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        observeViewModel()
        setupListeners()
    }

    override fun onResume() {
        super.onResume()
        viewModel.refreshStatus()
    }

    private fun observeViewModel() {
        viewModel.twoFactorEnabled.observe(viewLifecycleOwner) { enabled ->
            binding.twoFactorSwitch.setOnCheckedChangeListener(null)
            binding.twoFactorSwitch.isChecked = enabled
            setupListeners()

            binding.twoFactorContainer.visibility = if (enabled) View.VISIBLE else View.GONE
            binding.configureButton.visibility = if (enabled) View.GONE else View.VISIBLE
        }

        viewModel.error.observe(viewLifecycleOwner) { errorMsg ->
            errorMsg?.let {
                Snackbar.make(binding.root, it, Snackbar.LENGTH_LONG).show()
                viewModel.clearError()
            }
        }

        viewModel.success.observe(viewLifecycleOwner) { successMsg ->
            successMsg?.let {
                Snackbar.make(binding.root, it, Snackbar.LENGTH_LONG).show()
                viewModel.clearSuccess()
            }
        }
    }


    private fun setupListeners() {
        // Detect user-initiated touch
        binding.twoFactorSwitch.setOnTouchListener { _, event ->
            if (event.action == MotionEvent.ACTION_DOWN) {
                userInitiatedChange = true
            }
            false
        }

        binding.twoFactorSwitch.setOnCheckedChangeListener { _, isChecked ->
            if (userInitiatedChange) {
                userInitiatedChange = false
                if (isChecked != viewModel.twoFactorEnabled.value) {
                    if (isChecked) {
                        showEnableDialog()
                    } else {
                        showDisableDialog()
                    }
                }
            }
        }

        binding.configureButton.setOnClickListener {
            findNavController().navigate(R.id.action_securitySettingsFragment_to_twoFactorSetupFragment)
        }
    }

    private fun showEnableDialog() {
        AlertDialog.Builder(requireContext())
            .setTitle("Enable Two-Factor Authentication")
            .setMessage("Do you want to enable two-factor authentication and add an extra layer of security to your account?")
            .setPositiveButton("Continue") { _, _ ->
                findNavController().navigate(R.id.action_securitySettingsFragment_to_twoFactorSetupFragment)
            }
            .setNegativeButton("Cancel") { _, _ ->
                binding.twoFactorSwitch.setOnCheckedChangeListener(null)
                binding.twoFactorSwitch.isChecked = false
                setupListeners()
            }
            .setCancelable(false)
            .show()
    }


    private fun showDisableDialog() {
        val input = EditText(requireContext())
        input.hint = "Enter authentication code"

        AlertDialog.Builder(requireContext())
            .setTitle("Disable Two-Factor Authentication")
            .setMessage("To disable authentication, please enter the code from your authenticator app.")
            .setView(input)
            .setPositiveButton("Confirm") { _, _ ->
                val code = input.text.toString().trim()
                if (code.isNotEmpty()) {
                    viewModel.requestDisableTwoFactor(code)
                } else {
                    Snackbar.make(binding.root, "Please enter the authentication code", Snackbar.LENGTH_SHORT).show()
                    binding.twoFactorSwitch.setOnCheckedChangeListener(null)
                    binding.twoFactorSwitch.isChecked = true
                    setupListeners()
                }
            }
            .setNegativeButton("Cancel") { _, _ ->
                binding.twoFactorSwitch.setOnCheckedChangeListener(null)
                binding.twoFactorSwitch.isChecked = true
                setupListeners()
            }
            .setCancelable(false)
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

