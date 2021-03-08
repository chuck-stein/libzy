package io.libzy.view.launch

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import io.libzy.R
import io.libzy.util.spotifyConnected

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
        findNavController().navigate(navAction)
    }

}
