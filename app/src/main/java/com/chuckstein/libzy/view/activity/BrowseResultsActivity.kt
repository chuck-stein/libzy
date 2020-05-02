package com.chuckstein.libzy.view.activity

import android.os.Bundle
import androidx.activity.viewModels
import androidx.lifecycle.Observer
import com.chuckstein.libzy.R
import com.chuckstein.libzy.view.activity.common.GradientBackgroundActivity
import com.chuckstein.libzy.view.adapter.GenresRecyclerAdapter
import com.chuckstein.libzy.viewmodel.BrowseResultsViewModel
import com.chuckstein.libzy.viewmodel.factory.BrowseResultsViewModelFactory
import kotlinx.android.synthetic.main.activity_browse_results.genres_recycler as genresRecycler

class BrowseResultsActivity : GradientBackgroundActivity() {

    private val model: BrowseResultsViewModel by viewModels {
        // TODO: ensure the empty array case will just show a no results screen
        val selectedGenres = intent.getStringArrayExtra(getString(R.string.selected_genres_extra)) ?: emptyArray()
        BrowseResultsViewModelFactory(selectedGenres) // TODO: determine whether "return@viewModels" is necessary, or what's best practice
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_browse_results)

        val genresRecyclerAdapter = GenresRecyclerAdapter()
        genresRecycler.adapter = genresRecyclerAdapter
        model.genreResults.observe(this, Observer { genresRecyclerAdapter.genres = it })
    }
}
