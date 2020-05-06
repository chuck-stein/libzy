package com.chuckstein.libzy.view.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.chuckstein.libzy.R
import com.chuckstein.libzy.auth.SpotifyAuthManager
import com.chuckstein.libzy.auth.SpotifyAuthMediator
import kotlinx.coroutines.runBlocking
import kotlinx.android.synthetic.main.fragment_connect_spotify.connect_spotify_button as connectSpotifyButton

class ConnectSpotifyFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_connect_spotify, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        connectSpotifyButton.setOnClickListener {
//            SpotifyAuthManager.connectSpotify(requireActivity()) TODO: delete once satisfied with new approach
            runBlocking { // TODO: determine best CoroutineScope here (and eventually handle navigation once connectSpotify() is complete
                SpotifyAuthMediator.connectSpotify()
            }
        }
    }

}
