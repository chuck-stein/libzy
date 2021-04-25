package io.libzy.ui.connect

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
import io.libzy.analytics.AnalyticsDispatcher
import io.libzy.spotify.auth.SpotifyAuthDispatcher
import io.libzy.spotify.auth.SpotifyAuthException
import io.libzy.util.extensions.spotifyConnected
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject
import kotlinx.android.synthetic.main.fragment_connect_spotify.connect_spotify_button as connectSpotifyButton
import kotlinx.android.synthetic.main.fragment_connect_spotify.scanning_library_screen as scanningLibraryScreen

class ConnectSpotifyFragment : Fragment() {

    @Inject
    lateinit var viewModelFactory: ViewModelProvider.Factory
    private val model by viewModels<ConnectSpotifyViewModel> { viewModelFactory }

    @Inject
    lateinit var spotifyAuthDispatcher: SpotifyAuthDispatcher

    @Inject
    lateinit var analyticsDispatcher: AnalyticsDispatcher

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
        // TODO: only show this once, instead of after each configuration change
        if (navArgs.networkErrorReceived) reportSpotifyError()

        connectSpotifyButton.setOnClickListener { onConnectSpotifyButtonClicked() }
        model.libraryScanWorkInfo.observe(viewLifecycleOwner, ::onLibraryScanWorkChanged)
    }

    override fun onStart() {
        super.onStart()
        val libraryScanInProgress = getSpotifyPrefs().getBoolean(getString(R.string.spotify_initial_scan_in_progress_key), false)
        if (libraryScanInProgress) displayLibraryScanScreen()
        else displayConnectSpotifyScreen()
    }

    override fun onResume() {
        super.onResume()
        analyticsDispatcher.sendViewConnectSpotifyScreenEvent()
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
        val currentlyConnectedUserId = getSpotifyPrefs().getString(getString(R.string.spotify_user_id_key), null)
        analyticsDispatcher.sendClickConnectSpotifyEvent(currentlyConnectedUserId)

        lifecycleScope.launch {
            try {
                // TODO: don't request authorization if we already have it
                // TODO: somehow find out if we will get automatic auth or if the user needs to give permission
                //  via the dialog first. If the former, then set withTimeout = true
                spotifyAuthDispatcher.requestAuthorization(withTimeout = false)
            } catch (e: SpotifyAuthException) {
                if (navArgs.networkErrorReceived) reportSpotifyError(error = e)
                else reportSpotifyError(R.string.toast_connecting_spotify_account_failed, e)
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
            reportSpotifyError(R.string.toast_library_scan_failed) // TODO: only show this once, instead of after each configuration change
        }
    }

    private fun reportSpotifyError(errorMessageResId: Int = R.string.toast_spotify_generic_network_error, error: Exception? = null) {
        Timber.e(error, getString(errorMessageResId))
        Toast.makeText(requireContext(), errorMessageResId, Toast.LENGTH_LONG).show()
    }

    private fun navigateToQueryFragment() {
        findNavController().navigate(ConnectSpotifyFragmentDirections.actionConnectSpotifyFragmentToQueryFragment())
    }

    private fun getSpotifyPrefs() =
        requireContext().getSharedPreferences(getString(R.string.spotify_prefs_name), Context.MODE_PRIVATE)

}
