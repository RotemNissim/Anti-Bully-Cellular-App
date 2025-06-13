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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

class AddChildFragment : Fragment() {

    private lateinit var auth: FirebaseAuth
    private var selectedImageUri: Uri? = null
    private var uploadedImageUrl: String? = null
    private val redirectUri = "antibully://discord-callback"

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
        val code = data?.getQueryParameter("code")

        if (code != null) {
            requireActivity().intent?.data = null
            handleDiscordCode(code)
        }
    }

    private fun handleDiscordCode(code: String) {
        lifecycleScope.launch {
            val token = auth.currentUser?.getIdToken(false)?.await()?.token ?: return@launch
            val parentId = auth.currentUser?.uid ?: return@launch
            progressBar.visibility = View.VISIBLE

            try {
                val response = withContext(Dispatchers.IO) {
                    val url = URL("http://10.0.2.2:3000/api/oauth/discord/exchange")
                    val conn = url.openConnection() as HttpURLConnection
                    conn.requestMethod = "POST"
                    conn.setRequestProperty("Content-Type", "application/json")
                    conn.doOutput = true

                    val json = """{ \"code\": \"$code\" }"""
                    conn.outputStream.use { it.write(json.toByteArray()) }

                    if (conn.responseCode == 200) {
                        val result = conn.inputStream.bufferedReader().readText()
                        JSONObject(result)
                    } else null
                }

                response?.let { json ->
                    val discordId = json.getString("discordId")
                    val name = json.getString("username")
                    val imageUrl = json.optString("avatarUrl", null)

                    childViewModel.linkChild(token, parentId, discordId, name, imageUrl) { success ->
                        if (success) {
                            Toast.makeText(requireContext(), "Child linked", Toast.LENGTH_SHORT).show()
                            findNavController().navigateUp()
                        } else {
                            Toast.makeText(requireContext(), "Link failed", Toast.LENGTH_SHORT).show()
                        }
                    }
                } ?: run {
                    Toast.makeText(requireContext(), "Failed to get user data from server", Toast.LENGTH_LONG).show()
                }

            } catch (e: Exception) {
                Log.e("DiscordOAuth", "Error handling Discord code", e)
                Toast.makeText(requireContext(), "Error: ${e.message}", Toast.LENGTH_LONG).show()
            } finally {
                progressBar.visibility = View.GONE
            }
        }
    }
}