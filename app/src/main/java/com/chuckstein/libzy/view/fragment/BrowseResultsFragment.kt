package com.chuckstein.libzy.view.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.navigation.fragment.navArgs
import com.chuckstein.libzy.R
import com.chuckstein.libzy.view.adapter.GenresRecyclerAdapter
import com.chuckstein.libzy.viewmodel.BrowseResultsViewModel
import com.chuckstein.libzy.viewmodel.factory.BrowseResultsViewModelFactory
import kotlinx.android.synthetic.main.fragment_browse_results.genres_recycler as genresRecycler

class BrowseResultsFragment : Fragment() {

    private val navArgs: BrowseResultsFragmentArgs by navArgs()

    private val model: BrowseResultsViewModel by viewModels {
        BrowseResultsViewModelFactory(navArgs.selectedGenres)
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
        val genresRecyclerAdapter = GenresRecyclerAdapter { clickedAlbumUri -> model.playAlbum(clickedAlbumUri) }
        genresRecycler.adapter = genresRecyclerAdapter
        model.genreResults.observe(viewLifecycleOwner, Observer { genresRecyclerAdapter.genres = it })
    }
}
