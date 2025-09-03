package com.example.antibully.data.ui.auth

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.example.antibully.R
import com.example.antibully.viewmodel.TwoFactorLoginViewModel
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.doOnLayout
import kotlin.math.roundToInt

class TwoFactorLoginFragment : Fragment() {

    private val viewModel: TwoFactorLoginViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.fragment_two_factor_login, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        // שורש ה-NestedScrollView (אם לא קיים id כזה ניפול ל-root של הפרגמנט)
        val root = view.findViewById<View>(R.id.twoFactorRoot) ?: view
        val bottomNav =
            requireActivity().findViewById<BottomNavigationView>(R.id.bottom_nav_menu)

        // עוד ריווח מעבר ל-Insets (16dp)
        val extraBottomPx = (16f * resources.displayMetrics.density).roundToInt()

        // מחילים Insets לאחר שה-layout נמדד כדי לקבל גובה אמיתי של ה-BottomNav
        root.doOnLayout {
            ViewCompat.setOnApplyWindowInsetsListener(root) { v, insets ->
                val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
                val bottomNavHeight = bottomNav?.height ?: 0
                v.setPadding(
                    v.paddingLeft,
                    v.paddingTop,
                    v.paddingRight,
                    systemBars.bottom + bottomNavHeight + extraBottomPx
                )
                insets
            }
            ViewCompat.requestApplyInsets(root)
        }

        // Views
        val codeInput = view.findViewById<EditText>(R.id.codeInput)
        val verifyButton = view.findViewById<Button>(R.id.btnVerify)
        val errorText = view.findViewById<TextView>(R.id.tv2FAError)

        // Observers
        viewModel.verificationStatus.observe(viewLifecycleOwner) { success ->
            if (success) {
                Toast.makeText(requireContext(), "אימות הצליח ✅", Toast.LENGTH_SHORT).show()
                findNavController().navigate(R.id.profileFragment)
            }
        }

        viewModel.error.observe(viewLifecycleOwner) { error ->
            errorText.text = error ?: ""
        }

        // Actions
        verifyButton.setOnClickListener {
            val code = codeInput.text.toString().trim()
            if (code.isEmpty()) {
                errorText.text = "אנא הזן קוד אימות"
                return@setOnClickListener
            }

            lifecycleScope.launch {
                val token = FirebaseAuth.getInstance()
                    .currentUser?.getIdToken(true)?.await()?.token
                if (token != null) {
                    viewModel.verifyCode(token, code)
                } else {
                    errorText.text = "לא הצלחנו לקבל טוקן מהמשתמש"
                }
            }
        }
    }
}
