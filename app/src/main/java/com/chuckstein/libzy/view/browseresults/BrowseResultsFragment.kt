package com.chuckstein.libzy.view.browseresults

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.findNavController
import androidx.navigation.fragment.navArgs
import com.bumptech.glide.Glide
import com.chuckstein.libzy.R
import com.chuckstein.libzy.common.LibzyApplication
import com.chuckstein.libzy.view.browseresults.adapter.GenresRecyclerAdapter
import javax.inject.Inject
import kotlinx.android.synthetic.main.fragment_browse_results.genres_recycler as genresRecycler

class BrowseResultsFragment : Fragment() {

    @Inject
    lateinit var viewModelFactory: ViewModelProvider.Factory

    private val model by viewModels<BrowseResultsViewModel> { viewModelFactory }

    private val navArgs: BrowseResultsFragmentArgs by navArgs()

    override fun onAttach(context: Context) {
        super.onAttach(context)
        (requireActivity().application as LibzyApplication).appComponent.inject(this)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_browse_results, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // TODO: ensure when modifying a view in a RecyclerView, it gets recycled correctly (see Udacity lesson)
        val placeholderAlbumArt =
            resources.getDrawable(R.drawable.placeholder_album_art, requireContext().theme)
        val genresRecyclerAdapter =
            GenresRecyclerAdapter(Glide.with(this), placeholderAlbumArt) { clickedAlbumUri ->
                model.playAlbum(clickedAlbumUri)
            }
        genresRecycler.adapter = genresRecyclerAdapter
        // TODO: provide placeholder GenreResults as skeleton screen to GenresRecyclerAdapter

        model.fetchResults(navArgs.selectedGenres)
        model.genreResults.observe(viewLifecycleOwner, Observer { genresRecyclerAdapter.genres = it })
        model.receivedSpotifyNetworkError.observe(viewLifecycleOwner, Observer { if (it) onSpotifyNetworkError() }) // TODO: abstract this
    }

    // TODO: abstract this
    private fun onSpotifyNetworkError() {
        val networkErrorNavAction =
            BrowseResultsFragmentDirections.actionBrowseResultsFragmentToConnectSpotifyFragment()
        networkErrorNavAction.networkErrorReceived = true
        requireView().findNavController().navigate(networkErrorNavAction)
    }
}
