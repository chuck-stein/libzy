package com.chuckstein.libzy.view.activity

import android.content.Intent
import android.os.Bundle
import androidx.activity.viewModels
import androidx.core.view.children
import androidx.core.view.doOnNextLayout
import androidx.lifecycle.Observer
import com.airbnb.paris.extensions.style
import com.chuckstein.libzy.R
import com.chuckstein.libzy.view.activity.common.GradientBackgroundActivity
import com.chuckstein.libzy.viewmodel.SelectGenresViewModel
import com.facebook.shimmer.ShimmerFrameLayout
import com.google.android.material.chip.Chip
import kotlinx.android.synthetic.main.activity_select_genres.submit_genres_button as submitGenresButton
import kotlinx.android.synthetic.main.activity_select_genres.genre_options_chip_group as genreOptionsChipGroup
import kotlinx.android.synthetic.main.activity_select_genres.genre_options_scroll_view as genreOptionsScrollView
import kotlinx.android.synthetic.main.activity_select_genres.instructions_text as instructionsText
import kotlin.random.Random

class SelectGenresActivity : GradientBackgroundActivity() {

    companion object {
        val TAG = SelectGenresActivity::class.java.simpleName
    }

    private val model: SelectGenresViewModel by viewModels()
    private val loadingChips = mutableListOf<ShimmerFrameLayout>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_select_genres)
        submitGenresButton.setOnClickListener { submitGenreSelection() }
        if (model.albumsGroupedByGenre.value == null) createLoadingChips()
        model.newGenreDataReady.observe(this, Observer { if (it) clearLoadingChips() })
        model.albumsGroupedByGenre.observe(this, Observer(::populateGenreOptions))
    }

    // TODO: Determine if there's a cleaner way to do this that doesn't involve guessing the number of chips to add then removing extras
    private fun createLoadingChips() {
        repeat(50) { // TODO: to support tablets, calculate this based on scroll view size and average chip size
            val chip = Chip(this)
            with(chip) {
                text = "\t".repeat(Random.nextInt(5, 20)) // random-length spacing text for random chip size
                isClickable = false
            }
            val chipShimmer = ShimmerFrameLayout(this)  // chip container to make it shimmer to indicate loading
            chipShimmer.addView(chip)
            genreOptionsChipGroup.addView(chipShimmer)
            loadingChips.add(chipShimmer)
        }

        fun chipIsOffScreen(chipIndex: Int) =
            chipIndex > 0 && genreOptionsChipGroup.getChildAt(chipIndex).bottom > genreOptionsScrollView.height

        genreOptionsScrollView.doOnNextLayout {
            // remove any chips beyond the bottom of the scroll view window
            var chipIndex = genreOptionsChipGroup.childCount - 1
            while (chipIsOffScreen(chipIndex)) {
                genreOptionsChipGroup.removeViewAt(chipIndex)
                chipIndex--
            }
        }
    }

    private fun clearLoadingChips() {
        for (loadingChip in loadingChips) {
            genreOptionsChipGroup.removeView(loadingChip)
        }
        model.onLoadingEnded()
    }

    private fun populateGenreOptions(albumsGroupedByGenre: Map<String, Set<String>>) {
        // a list of genres in the user's library, sorted by how many of their saved albums fit that genre, descending
        val orderedGenres = albumsGroupedByGenre.keys.sortedByDescending { albumsGroupedByGenre[it]?.size }
        for (genre in orderedGenres.take(100)) { // TODO: need a RecyclerView to prevent the lag when adding full list (take(100) is just so it doesn't lag for the time being)
            with(Chip(this)) {
                style(R.style.Chip) // TODO: partially works, but some material styling missing... probably an issue with the way I defined the style (or could it be because there are no shimmer wrappers? or no width/height/id? or maybe the style is just straight up right... but no the loading ones look bigger... could THAT be shimmer?)
                text = genre
                genreOptionsChipGroup.addView(this)
            }
        }
        instructionsText.text = getString(R.string.select_genres_instructions_text)
    }

    private fun submitGenreSelection() {
        val selectedGenres = mutableSetOf<String>()
        for (chip in genreOptionsChipGroup.children) {
            if (chip is Chip && chip.isChecked) {
                selectedGenres.add(chip.text.toString())
            }
        }
        val startBrowsingSelectedGenres = Intent(this, BrowseResultsActivity::class.java)
        startBrowsingSelectedGenres.putExtra(getString(R.string.selected_genres_extra), selectedGenres.toTypedArray())
        startActivity(startBrowsingSelectedGenres)
    }

}
