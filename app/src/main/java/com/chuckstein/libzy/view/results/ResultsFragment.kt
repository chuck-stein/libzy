package com.chuckstein.libzy.view.results

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.content.res.ResourcesCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.navigation.findNavController
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.GridLayoutManager
import com.bumptech.glide.Glide
import com.chuckstein.libzy.R
import com.chuckstein.libzy.common.LibzyApplication
import com.chuckstein.libzy.model.AlbumResult
import com.chuckstein.libzy.view.results.adapter.AlbumsRecyclerAdapter
import kotlinx.android.synthetic.main.fragment_results.albums_recycler as albumsRecycler
import kotlinx.android.synthetic.main.fragment_results.results_header as resultsHeader
import kotlinx.coroutines.launch
import javax.inject.Inject

// TODO: add back button to this screen
class ResultsFragment : Fragment() {

    companion object {
        // TODO: determine this based on screen size instead of hardcoding it
        private const val NUM_PLACEHOLDER_RESULTS = 50
    }

    @Inject
    lateinit var viewModelFactory: ViewModelProvider.Factory
    private val model by viewModels<ResultsViewModel> { viewModelFactory }

    private val navArgs: ResultsFragmentArgs by navArgs()

    override fun onAttach(context: Context) {
        super.onAttach(context)
        (requireActivity().application as LibzyApplication).appComponent.inject(this)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_results, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // TODO: fix infinite placeholders if recommendation is empty
        val placeholderAlbumArt =
            ResourcesCompat.getDrawable(resources, R.drawable.placeholder_album_art, requireContext().theme)
        val albumsRecyclerAdapter = AlbumsRecyclerAdapter(Glide.with(this), placeholderAlbumArt, ::onAlbumClicked)
        albumsRecyclerAdapter.albums = List(NUM_PLACEHOLDER_RESULTS) {
            AlbumResult("Fetching album data", "Please wait...", isPlaceholder = true)
        }
        albumsRecycler.adapter = albumsRecyclerAdapter
        albumsRecycler.layoutManager = GridLayoutManager(
            requireContext(),
            3
        ) // TODO: either don't hardcode spanCount or change this screen's UI to browse by artist, category, audio features, etc

        // TODO: remove unnecessary coroutine launching, if suspend functions are never used within here
        lifecycleScope.launch {
            model.getResults(navArgs.query).observe(viewLifecycleOwner, { results ->
                albumsRecyclerAdapter.albums = results
                if (results.isEmpty()) resultsHeader.text = 
                    "Sorry! No results were found for that query. Try saving more albums on Spotify or entering a different query." 
                // TODO: implement a better no results screen (w/ "try another query" button)
            })
        }

        // TODO: abstract this
        model.receivedSpotifyNetworkError.observe(viewLifecycleOwner, { if (it) onSpotifyNetworkError() })
    }

    override fun onStart() {
        super.onStart()
        model.connectSpotifyAppRemote {} // TODO: make failure handler display a message over bottom "now playing" banner with an option to try reconnecting to Spotify remote (and if an album is tapped while remote is disabled, highlight this message)
    }

    override fun onStop() {
        super.onStop()
        model.disconnectSpotifyAppRemote()
    }

    private fun onAlbumClicked(spotifyUri: String) {
        try {
            model.playAlbum(spotifyUri)
        } catch (e: Exception) {
            Toast.makeText(requireContext(), R.string.toast_spotify_remote_failed, Toast.LENGTH_LONG).show()
        }
    }

    // TODO: abstract this
    private fun onSpotifyNetworkError() {
        requireView().findNavController().navigate(
            ResultsFragmentDirections.actionResultsFragmentToConnectSpotifyFragment()
        )
    }

}