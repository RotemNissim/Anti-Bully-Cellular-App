package com.example.antibully.data.ui.auth

import android.os.Bundle
import android.view.*
import android.widget.*
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.example.antibully.R
import com.example.antibully.viewmodel.TwoFactorLoginViewModel
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class TwoFactorLoginFragment : Fragment() {

    private val viewModel: TwoFactorLoginViewModel by viewModels()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        return inflater.inflate(R.layout.fragment_two_factor_login, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val codeInput = view.findViewById<EditText>(R.id.et2FACode)
        val verifyButton = view.findViewById<Button>(R.id.btnVerify)
        val errorText = view.findViewById<TextView>(R.id.tv2FAError)

        viewModel.verificationStatus.observe(viewLifecycleOwner) { success ->
            if (success) {
                Toast.makeText(requireContext(), "אימות הצליח ✅", Toast.LENGTH_SHORT).show()
                findNavController().navigate(R.id.profileFragment)
            }
        }

        viewModel.error.observe(viewLifecycleOwner) { error ->
            errorText.text = error ?: ""
        }

        verifyButton.setOnClickListener {
            val code = codeInput.text.toString().trim()
            if (code.isEmpty()) {
                errorText.text = "אנא הזן קוד אימות"
                return@setOnClickListener
            }

            lifecycleScope.launch {
                val token = FirebaseAuth.getInstance().currentUser?.getIdToken(true)?.await()?.token
                if (token != null) {
                    viewModel.verifyCode(token, code)
                } else {
                    errorText.text = "לא הצלחנו לקבל טוקן מהמשתמש"
                }
            }
        }
    }
}
