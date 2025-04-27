package com.example.antibully.data.ui.settings

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.example.antibully.databinding.FragmentTwoFactorSetupBinding
import com.google.android.material.snackbar.Snackbar

class TwoFactorSetupFragment : Fragment() {
    private var _binding: FragmentTwoFactorSetupBinding? = null
    private val binding get() = _binding!!
    private val viewModel: TwoFactorSetupViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentTwoFactorSetupBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        observeViewModel()
    }

    private fun setupViews() {
        binding.verifyButton.setOnClickListener {
            val code = binding.codeInput.text.toString().trim()
            if (code.length == 6 && code.matches(Regex("\\d{6}"))) {
                binding.verifyButton.isEnabled = false
                binding.progressBar.visibility = View.VISIBLE
                viewModel.verifyCode(code)
            } else {
                Snackbar.make(binding.root, "נא להזין קוד בן 6 ספרות", Snackbar.LENGTH_SHORT).show()
            }
        }
    }

    private fun observeViewModel() {
        viewModel.twoFactorEnabled.observe(viewLifecycleOwner) { isEnabled ->
            if (isEnabled == true) {
                Snackbar.make(binding.root, "אימות דו-שלבי כבר מופעל", Snackbar.LENGTH_SHORT).show()
                requireActivity().onBackPressed()
            } else {
                viewModel.generateQrCode()
            }
        }

        viewModel.qrCodeBitmap.observe(viewLifecycleOwner) { bitmap ->
            binding.qrCodeImage.setImageBitmap(bitmap)
            binding.progressBar.visibility = View.GONE
            setupViews()
        }

        viewModel.setupSuccess.observe(viewLifecycleOwner) { success ->
            if (success) {
                Snackbar.make(binding.root, "אימות דו-שלבי הופעל בהצלחה", Snackbar.LENGTH_SHORT).show()
                requireActivity().onBackPressed()
            }
        }

        viewModel.error.observe(viewLifecycleOwner) { error ->
            error?.let {
                Snackbar.make(binding.root, it, Snackbar.LENGTH_LONG).show()
                binding.verifyButton.isEnabled = true
                binding.progressBar.visibility = View.GONE
                viewModel.clearError()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

//package com.example.antibully.data.ui.settings
//
//import android.os.Bundle
//import android.view.LayoutInflater
//import android.view.View
//import android.view.ViewGroup
//import androidx.fragment.app.Fragment
//import androidx.fragment.app.viewModels
//import com.example.antibully.databinding.FragmentTwoFactorSetupBinding
//import com.google.android.material.snackbar.Snackbar
//
//class TwoFactorSetupFragment : Fragment() {
//    private var _binding: FragmentTwoFactorSetupBinding? = null
//    private val binding get() = _binding!!
//    private val viewModel: TwoFactorSetupViewModel by viewModels()
//
//    override fun onCreateView(
//        inflater: LayoutInflater, container: ViewGroup?,
//        savedInstanceState: Bundle?
//    ): View {
//        _binding = FragmentTwoFactorSetupBinding.inflate(inflater, container, false)
//        return binding.root
//    }
//
//    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
//        super.onViewCreated(view, savedInstanceState)
//
//        observeViewModel()
//
//        // Check if 2FA is already enabled before proceeding
//        if (viewModel.isTwoFactorAlreadyEnabled()) {
//            Snackbar.make(binding.root, "אימות דו-שלבי כבר מופעל", Snackbar.LENGTH_SHORT).show()
//            requireActivity().onBackPressed()
//            return
//        }
//
//        // Generate QR code for new setup
//        viewModel.generateQrCode()
//        setupViews()
//    }
//
//    private fun setupViews() {
//        binding.verifyButton.setOnClickListener {
//            val code = binding.codeInput.text.toString().trim()
//            if (code.length == 6 && code.matches(Regex("\\d{6}"))) {
//                binding.verifyButton.isEnabled = false
//                binding.progressBar.visibility = View.VISIBLE
//                viewModel.verifyCode(code)
//            } else {
//                Snackbar.make(binding.root, "נא להזין קוד בן 6 ספרות", Snackbar.LENGTH_SHORT).show()
//            }
//        }
//    }
//
//    private fun observeViewModel() {
//        viewModel.qrCodeBitmap.observe(viewLifecycleOwner) { bitmap ->
//            binding.qrCodeImage.setImageBitmap(bitmap)
//            binding.progressBar.visibility = View.GONE
//        }
//
//        viewModel.setupSuccess.observe(viewLifecycleOwner) { success ->
//            if (success) {
//                Snackbar.make(binding.root, "אימות דו-שלבי הופעל בהצלחה", Snackbar.LENGTH_SHORT).show()
//                requireActivity().onBackPressed()
//            }
//        }
//
//        viewModel.error.observe(viewLifecycleOwner) { error ->
//            error?.let {
//                Snackbar.make(binding.root, it, Snackbar.LENGTH_LONG).show()
//                binding.verifyButton.isEnabled = true
//                binding.progressBar.visibility = View.GONE
//                viewModel.clearError()
//            }
//        }
//    }
//
//    override fun onDestroyView() {
//        super.onDestroyView()
//        _binding = null
//    }
//}