package com.example.antibully.data.ui.settings

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.example.antibully.R
import com.example.antibully.databinding.FragmentConnectDiscordBinding

class ConnectDiscordFragment : Fragment() {

    private var _binding: FragmentConnectDiscordBinding? = null
    private val binding get() = _binding!!
    private val botInviteUrl =
        "https://discord.com/oauth2/authorize?client_id=1371794470890115093&scope=bot%20applications.commands&permissions=8"

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentConnectDiscordBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)


        binding.btnAddBot.setOnClickListener {

            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(botInviteUrl))
            startActivity(intent)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
