package io.libzy.view.connect

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.work.WorkInfo
import io.libzy.LibzyApplication
import io.libzy.R
import io.libzy.common.spotifyConnected
import io.libzy.spotify.auth.SpotifyAuthDispatcher
import io.libzy.spotify.auth.SpotifyAuthException
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlinx.android.synthetic.main.fragment_connect_spotify.connect_spotify_button as connectSpotifyButton
import kotlinx.android.synthetic.main.fragment_connect_spotify.scanning_library_screen as scanningLibraryScreen

class ConnectSpotifyFragment : Fragment() {

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
        // TODO: only show this once, instead of after each configuration change (using ViewModel) -- OR don't worry about this if yet if we shouldn't always redirect to Connect Spotify screen fro errors that can still occur once getting past this screen
        if (navArgs.networkErrorReceived) reportSpotifyError()

        connectSpotifyButton.setOnClickListener { onConnectSpotifyButtonClicked() }
        model.libraryScanWorkInfo.observe(viewLifecycleOwner, ::onLibraryScanWorkChanged)
    }

    override fun onStart() {
        super.onStart()
        val libraryScanInProgress =
            requireContext().getSharedPreferences(getString(R.string.spotify_prefs_name), Context.MODE_PRIVATE)
                .getBoolean(getString(R.string.spotify_initial_scan_in_progress_key), false)
        if (libraryScanInProgress) displayLibraryScanScreen()
        else displayConnectSpotifyScreen()
    }

    private fun displayConnectSpotifyScreen() {
        scanningLibraryScreen.visibility = View.GONE
        connectSpotifyButton.visibility = View.VISIBLE
    }

    private fun displayLibraryScanScreen() {
        connectSpotifyButton.visibility = View.GONE
        scanningLibraryScreen.visibility = View.VISIBLE
    }

    private fun onConnectSpotifyButtonClicked() {
        lifecycleScope.launch {
            try {
                spotifyAuthDispatcher.requestAuthorization()
            } catch (e: SpotifyAuthException) {
                if (navArgs.networkErrorReceived) reportSpotifyError()
                else reportSpotifyError(R.string.toast_connecting_spotify_account_failed)
                return@launch
            }

            if (spotifyConnected()) {
                // don't rescan library if user already connected their Spotify
                // (e.g. if user was only directed to this fragment due to a network error,
                // they can safely proceed to QueryFragment now that Spotify authorized w/o network error)
                navigateToQueryFragment()
            } else {
                displayLibraryScanScreen()
                model.scanLibrary()
            }
        }
    }

    private fun onLibraryScanWorkChanged(workInfos: List<WorkInfo>?) {
        val libraryScanWorkState = workInfos?.firstOrNull()?.state

        if (libraryScanWorkState == WorkInfo.State.SUCCEEDED && spotifyConnected()) {
            navigateToQueryFragment()
        } else if (libraryScanWorkState == WorkInfo.State.FAILED || libraryScanWorkState == WorkInfo.State.CANCELLED) {
            displayConnectSpotifyScreen()
            reportSpotifyError(R.string.toast_library_scan_failed) // TODO: only show this once, instead of after each configuration change (using ViewModel)
        }
    }

    private fun reportSpotifyError(errorMessageResId: Int = R.string.toast_spotify_generic_network_error) {
        Toast.makeText(requireContext(), errorMessageResId, Toast.LENGTH_LONG).show()
    }

    private fun navigateToQueryFragment() {
        findNavController().navigate(ConnectSpotifyFragmentDirections.actionConnectSpotifyFragmentToQueryFragment())
    }

}
