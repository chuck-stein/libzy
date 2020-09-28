package com.chuckstein.libzy.view.connect

import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.content.edit
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.adamratzman.spotify.SpotifyException
import com.chuckstein.libzy.R
import com.chuckstein.libzy.common.LibzyApplication
import com.chuckstein.libzy.common.spotifyConnected
import com.chuckstein.libzy.spotify.auth.SpotifyAuthDispatcher
import com.chuckstein.libzy.spotify.auth.SpotifyAuthException
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlinx.android.synthetic.main.fragment_connect_spotify.connect_spotify_button as connectSpotifyButton
import kotlinx.android.synthetic.main.fragment_connect_spotify.scanning_library_screen as scanningLibraryScreen

class ConnectSpotifyFragment : Fragment() {

    companion object {
        private val TAG = ConnectSpotifyFragment::class.java.simpleName
    }

    @Inject
    lateinit var viewModelFactory: ViewModelProvider.Factory
    private val model by viewModels<ConnectSpotifyViewModel> { viewModelFactory }

    @Inject
    lateinit var spotifyAuthDispatcher: SpotifyAuthDispatcher

    private val navArgs: ConnectSpotifyFragmentArgs by navArgs()

    override fun onAttach(context: Context) {
        super.onAttach(context)
        (requireActivity().application as LibzyApplication).appComponent.inject(this)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_connect_spotify, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // alert the user if they were directed to this fragment because of a network error
        // TODO: only show this once, instead of after each configuration change (so a ViewModel is probably necessary) -- OR don't worry about this if yet if we shouldn't always redirect to Connect Spotify screen fro errors that can still occur once getting past this screen
        if (navArgs.networkErrorReceived) reportNetworkError()

        connectSpotifyButton.setOnClickListener { onConnectSpotifyButtonClicked() }
    }

    private fun onConnectSpotifyButtonClicked() {
        lifecycleScope.launch {
            try {
                spotifyAuthDispatcher.requestAuthorization()
            } catch (e: SpotifyAuthException) {
                if (navArgs.networkErrorReceived) reportNetworkError()
                else Toast.makeText(
                    requireContext(),
                    R.string.toast_connecting_spotify_account_failed,
                    Toast.LENGTH_LONG
                ).show()
                return@launch
            }
            if (!spotifyConnected()) {
                connectSpotifyButton.visibility = View.GONE
                scanningLibraryScreen.visibility = View.VISIBLE
                try {
                    // TODO: test what happens when you close the app while scanning
                    // TODO: verify that this operation can run (AND COMPLETE) while the app is in the background
                    //  (after verifying, change subheading text back to "This may take a few minutes, so feel free to come back later")
                    model.scanLibrary()
                } catch (e: Exception) {
                    if (e is SpotifyException || e is SpotifyAuthException) { // TODO: abstract this exception predicate
                        Log.e(TAG, "Received a Spotify network error", e) // TODO: abstract this log message
                        reportNetworkError()
                        connectSpotifyButton.visibility = View.VISIBLE
                        scanningLibraryScreen.visibility = View.GONE
                        return@launch
                    } else throw e
                }
                // TODO: send a notification that the library scan is complete
                recordSpotifyConnected()
            }
            findNavController().navigate(ConnectSpotifyFragmentDirections.actionConnectSpotifyFragmentToQueryFragment())
        }
    }

    private fun reportNetworkError() {
        Toast.makeText(requireContext(), R.string.toast_spotify_network_error, Toast.LENGTH_LONG).show()
    }

    private fun recordSpotifyConnected() {
        requireContext().getSharedPreferences(getString(R.string.spotify_prefs_name), Context.MODE_PRIVATE).edit {
            putBoolean(getString(R.string.spotify_connected_key), true)
        }
    }

}
