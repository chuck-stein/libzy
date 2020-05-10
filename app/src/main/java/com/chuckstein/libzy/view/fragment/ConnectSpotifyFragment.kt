package com.chuckstein.libzy.view.fragment

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.coroutineScope
import androidx.navigation.findNavController
import androidx.navigation.fragment.navArgs
import com.chuckstein.libzy.R
import com.chuckstein.libzy.network.auth.SpotifyAuthDispatcher
import com.chuckstein.libzy.network.auth.SpotifyAuthException
import kotlinx.coroutines.launch
import kotlinx.android.synthetic.main.fragment_connect_spotify.connect_spotify_button as connectSpotifyButton

class ConnectSpotifyFragment : Fragment() {

    private val navArgs: ConnectSpotifyFragmentArgs by navArgs()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_connect_spotify, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // alert the user if they were directed to this fragment because of a network error
        if (navArgs.networkErrorReceived) {
            Toast.makeText(requireContext(), getString(R.string.toast_spotify_network_error), Toast.LENGTH_LONG).show()
        }

        connectSpotifyButton.setOnClickListener { onConnectSpotifyButtonClicked() }
    }

    private fun onConnectSpotifyButtonClicked() {
        lifecycle.coroutineScope.launch {
            try {
                SpotifyAuthDispatcher.requestAuthorization()
                recordSpotifyConnected()
                requireView().findNavController()
                    .navigate(ConnectSpotifyFragmentDirections.actionConnectSpotifyFragmentToSelectGenresFragment())
            } catch (e: SpotifyAuthException) {
                val errorMessage =
                    if (navArgs.networkErrorReceived) getString(R.string.toast_spotify_network_error)
                    else getString(R.string.toast_connecting_spotify_account_failed)
                Toast.makeText(requireContext(), errorMessage, Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun recordSpotifyConnected() {
        val spotifyPrefs = requireContext().getSharedPreferences(
            getString(R.string.spotify_prefs_name),
            Context.MODE_PRIVATE
        )
        with(spotifyPrefs.edit()) {
            putBoolean(getString(R.string.spotify_connected_key), true)
            apply()
        }
    }

}
