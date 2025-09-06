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
import com.google.android.material.button.MaterialButton
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.textfield.TextInputEditText

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
    val dialogView = LayoutInflater.from(requireContext())
        .inflate(R.layout.dialog_enable_2fa, null)

    val dialog = AlertDialog.Builder(requireContext(), R.style.CustomDialogTheme)
        .setView(dialogView)
        .setCancelable(false)
        .create()

    // Set up button click listeners
    dialogView.findViewById<MaterialButton>(R.id.continueButton).setOnClickListener {
        dialog.dismiss()
        findNavController().navigate(R.id.action_securitySettingsFragment_to_twoFactorSetupFragment)
    }

    dialogView.findViewById<MaterialButton>(R.id.cancelButton).setOnClickListener {
        dialog.dismiss()
        binding.twoFactorSwitch.setOnCheckedChangeListener(null)
        binding.twoFactorSwitch.isChecked = false
        setupListeners()
    }

    dialog.show()
}


    private fun showDisableDialog() {
    val dialogView = layoutInflater.inflate(R.layout.dialog_disable_2fa, null)
    val dialog = AlertDialog.Builder(requireContext(), R.style.CustomDialogTheme)
        .setView(dialogView)
        .setCancelable(false)
        .create()

    val codeInput = dialogView.findViewById<TextInputEditText>(R.id.codeInput)
    val confirmButton = dialogView.findViewById<MaterialButton>(R.id.confirmButton)
    val cancelButton = dialogView.findViewById<MaterialButton>(R.id.cancelButton)

    confirmButton.setOnClickListener {
        val code = codeInput.text.toString().trim()
        if (code.isNotEmpty()) {
            viewModel.requestDisableTwoFactor(code)
            dialog.dismiss()
        } else {
            codeInput.error = "Please enter the authentication code"
        }
    }

    cancelButton.setOnClickListener {
        binding.twoFactorSwitch.setOnCheckedChangeListener(null)
        binding.twoFactorSwitch.isChecked = true
        setupListeners()
        dialog.dismiss()
    }

    dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
    dialog.show()
}


    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

