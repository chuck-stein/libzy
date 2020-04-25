package com.chuckstein.libzy.activity

import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.doOnNextLayout
import androidx.lifecycle.Observer
import com.airbnb.paris.extensions.style
import com.chuckstein.libzy.R
import com.chuckstein.libzy.extension.initializeBackground
import com.chuckstein.libzy.viewmodel.SelectGenresViewModel
import com.facebook.shimmer.ShimmerFrameLayout
import com.google.android.material.chip.Chip
import kotlinx.android.synthetic.main.activity_select_genres.*
import kotlin.random.Random

class SelectGenresActivity : AppCompatActivity() {

    companion object {
        val TAG = SelectGenresActivity::class.java.simpleName
    }

    private val model: SelectGenresViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_select_genres)
        initializeBackground()
        createLoadingChips()

        // TODO: ensure this callback is only called once, the one time I want it
        model.albumsGroupedByGenre.observe(this, Observer(::populateGenreOptions))
    }

    private fun populateGenreOptions(albumsGroupedByGenre: Map<String, Set<String>>) {
        // a list of genres in the user's library, sorted by how many of their saved albums fit that genre (desc.)
        val orderedGenres = albumsGroupedByGenre.keys.sortedByDescending { albumsGroupedByGenre[it]?.size }
        genreOptionsChipGroup.removeAllViews()
        for (genre in orderedGenres.take(100)) { // TODO: need a RecyclerView to prevent the lag when adding full list (take(100) is just so it doesn't lag for the time being)
            val genreChip = Chip(this)
            genreChip.style(R.style.Chip) // TODO: partially works, but some material styling missing... probably an issue with the way I defined the style (or could it be because there are no shimmer wrappers? or no width/height/id? or maybe the style is just straight up right... but no the loading ones look bigger... could THAT be shimmer?)
            genreChip.text = genre
            genreOptionsChipGroup.addView(genreChip)
        }
        instructionsText.text = getString(R.string.select_genres_instructions_text)
    }

    // TODO: Handle case where user rotates phone while genres are loading, so the loading chips defined in different
    //       orientation no longer fill up the scroll view. Also, determine if there's a cleaner way to do this that
    //       doesn't involve guessing the number of chips to add then removing extras
    private fun createLoadingChips() {

        // remove the placeholder chips, which are just for testing new styling in layout editor w/o compiling
        genreOptionsChipGroup.removeAllViews()

        // TODO: to support tablets, calculate this based on scroll view size and average chip size
        val maxLoadingChips = 50

        for (i in 1..maxLoadingChips) {
            val chip = Chip(this)
            val spacingText = "\t".repeat(Random.nextInt(5, 20))
            chip.text = spacingText
            chip.isClickable = false
            val chipContainer = ShimmerFrameLayout(this) // to make the chip shimmer to indicate loading
            chipContainer.addView(chip)
            genreOptionsChipGroup.addView(chipContainer)
        }

        genreOptionsScrollView.doOnNextLayout {
            // remove any chips beyond the bottom of the scroll view window
            var chipIndex = genreOptionsChipGroup.childCount - 1
            while(chipIndex > 0 && genreOptionsChipGroup.getChildAt(chipIndex).bottom > genreOptionsScrollView.height) {
                genreOptionsChipGroup.removeViewAt(chipIndex)
                chipIndex--
            }
        }
    }

}
