package com.example.antibully.data.ui.profile

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.example.antibully.R
import com.example.antibully.data.db.AppDatabase
import com.example.antibully.data.repository.ChildRepository
import com.example.antibully.viewmodel.ChildViewModel
import com.example.antibully.viewmodel.ChildViewModelFactory
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class AddChildFragment : Fragment() {

    private lateinit var auth: FirebaseAuth
    private var selectedImageUri: Uri? = null
    private val redirectUri = "http://10.0.2.2:3000/api/oauth/discord/callback"

    private lateinit var childImageView: ImageView
    private lateinit var childViewModel: ChildViewModel
    private lateinit var progressBar: ProgressBar

    private val pickImageLauncher =
        registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
            uri?.let {
                selectedImageUri = it
                childImageView.setImageURI(it)
            }
        }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = inflater.inflate(R.layout.fragment_add_child, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        auth = FirebaseAuth.getInstance()

        childImageView = view.findViewById(R.id.ivAddChildImage)
        val chooseImageButton = view.findViewById<View>(R.id.btnChooseChildImage)
        val connectDiscordButton = view.findViewById<View>(R.id.btnConnectDiscord)
        progressBar = view.findViewById(R.id.progressBar)

        // ViewModel setup
        val childDao = AppDatabase.getDatabase(requireContext()).childDao()
        val childRepository = ChildRepository(childDao)
        val factory = ChildViewModelFactory(childRepository)
        childViewModel = ViewModelProvider(this, factory)[ChildViewModel::class.java]

        chooseImageButton.setOnClickListener {
            pickImageLauncher.launch("image/*")
        }

        connectDiscordButton.setOnClickListener {
            val oauthUrl = "https://discord.com/oauth2/authorize?client_id=1373612221166391397&response_type=code&redirect_uri=${Uri.encode(redirectUri)}&scope=identify"
            Log.d("DiscordOAuth", "Opening URL: $oauthUrl")
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(oauthUrl))
            startActivity(intent)
        }
    }

    override fun onResume() {
        super.onResume()
        val data: Uri? = requireActivity().intent?.data
        val discordId = data?.getQueryParameter("discordId")
        val name = data?.getQueryParameter("name")
        val imageUrl = data?.getQueryParameter("imageUrl")

        if (!discordId.isNullOrBlank() && !name.isNullOrBlank()) {
            requireActivity().intent?.data = null
            linkChildAfterServerHandled(discordId, name, imageUrl)
        }
    }

    private fun linkChildAfterServerHandled(discordId: String, name: String, imageUrl: String?) {
        lifecycleScope.launch {
            progressBar.visibility = View.VISIBLE
            try {
                val token = auth.currentUser?.getIdToken(false)?.await()?.token ?: return@launch
                val parentId = auth.currentUser?.uid ?: return@launch

                childViewModel.linkChild(token, parentId, discordId, name, imageUrl) { success ->
                    if (success) {
                        Toast.makeText(requireContext(), "Child linked", Toast.LENGTH_SHORT).show()
                        findNavController().navigateUp()
                    } else {
                        Toast.makeText(requireContext(), "Link failed", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                Toast.makeText(requireContext(), "Error: ${e.message}", Toast.LENGTH_LONG).show()
            } finally {
                progressBar.visibility = View.GONE
            }
        }
    }
}