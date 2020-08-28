package com.chuckstein.libzy.view.launch

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.findNavController
import com.chuckstein.libzy.R

class LaunchFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_launch, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val navAction =
            if (spotifyConnected()) LaunchFragmentDirections.actionLaunchFragmentToQueryFragment()
            else LaunchFragmentDirections.actionLaunchFragmentToConnectSpotifyFragment()
        view.findNavController().navigate(navAction)
    }

    private fun spotifyConnected() =
        requireContext().getSharedPreferences(getString(R.string.spotify_prefs_name), Context.MODE_PRIVATE)
            .getBoolean(getString(R.string.spotify_connected_key), false)

}
