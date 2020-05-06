package com.chuckstein.libzy.view.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CompoundButton
import androidx.core.view.children
import androidx.core.view.doOnNextLayout
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.navigation.findNavController
import com.airbnb.paris.extensions.style
import com.chuckstein.libzy.R
import com.chuckstein.libzy.viewmodel.SelectGenresViewModel
import com.facebook.shimmer.ShimmerFrameLayout
import com.google.android.material.chip.Chip
import kotlin.random.Random
import kotlinx.android.synthetic.main.fragment_select_genres.genre_options_chip_group as genreOptionsChipGroup
import kotlinx.android.synthetic.main.fragment_select_genres.genre_options_scroll_view as genreOptionsScrollView
import kotlinx.android.synthetic.main.fragment_select_genres.instructions_text as instructionsText
import kotlinx.android.synthetic.main.fragment_select_genres.submit_genres_button as submitGenresButton

class SelectGenresFragment : Fragment() {

    private val model: SelectGenresViewModel by viewModels()

    // TODO: instead of using newGenresReady and loadingChips, just removeAllViews before populationg chipgroup, unless there are bugs/drawbacks
    private val loadingChips = mutableListOf<ShimmerFrameLayout>()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_select_genres, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        submitGenresButton.setOnClickListener { submitGenreSelection() }
        if (model.albumsGroupedByGenre.value == null) createLoadingChips()
        model.loadingShouldBegin.observe(viewLifecycleOwner, Observer { if (it) startLoadingChipAnimations() })
        model.loadingShouldEnd.observe(viewLifecycleOwner, Observer { if (it) clearLoadingChips() })
        model.albumsGroupedByGenre.observe(viewLifecycleOwner, Observer(::onGenreOptionsReady))
    }

    private fun submitGenreSelection() {
        val selectedGenres = mutableSetOf<String>()
        for (chip in genreOptionsChipGroup.children) {
            if (chip is Chip && chip.isChecked) {
                selectedGenres.add(chip.text.toString())
            }
        }
        val submitGenresNavAction = SelectGenresFragmentDirections
            .actionSelectGenresFragmentToBrowseResultsFragment(selectedGenres.toTypedArray())
        requireView().findNavController().navigate(submitGenresNavAction)
    }

    // TODO: Determine if there's a cleaner way to do this that doesn't involve guessing the number of chips to add then removing extras
    private fun createLoadingChips() {
        repeat(50) { // TODO: to support tablets, calculate this based on scroll view size and average chip size
            // TODO: clean up this nested apply() syntax if it's unreadable or bad practice
            // chip container to make it shimmer to indicate loading
            val chipShimmer = (ShimmerFrameLayout(requireContext()/*add attribute*/)).apply {
                // shouldn't start shimmering until model sends the loading start event
                stopShimmer()
                addView(Chip(requireContext()).apply {
                    // random-length spacing text for random chip size
                    text = "\t".repeat(Random.nextInt(5, 20))
                    isClickable = false
                })
            }
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

    // TODO: determine why the animations start before Spotify auth still
    private fun startLoadingChipAnimations() {
        for (loadingChip in loadingChips) {
//            loadingChip.startShimmer()
        }
        model.onLoadingStarted()
    }

    private fun clearLoadingChips() {
        for (loadingChip in loadingChips) {
            genreOptionsChipGroup.removeView(loadingChip)
        }
        model.onLoadingEnded()
    }

    private fun onGenreOptionsReady(albumsGroupedByGenre: Map<String, Set<String>>) {
        // a list of genres in the user's library, sorted by how many of their saved albums fit that genre, descending
        val orderedGenres = albumsGroupedByGenre.keys.sortedByDescending { albumsGroupedByGenre[it]?.size }
        for (genre in orderedGenres.take(100)) { // TODO: need a RecyclerView to prevent the lag when adding full list (take(100) is just so it doesn't lag for the time being)
            with(Chip(requireContext())) {
                style(R.style.Chip) // TODO: partially works, but some material styling missing... probably an issue with the way I defined the style (or could it be because there are no shimmer wrappers? or no width/height/id? or maybe the style is just straight up right... but no the loading ones look bigger... could THAT be shimmer?)
                text = genre
                setOnCheckedChangeListener { _: CompoundButton, _: Boolean -> updateSubmitButtonState() }
                genreOptionsChipGroup.addView(this)
            }
        }
        instructionsText.text = getString(R.string.select_genres_instructions_text)
    }

    private fun updateSubmitButtonState() {
        submitGenresButton.isEnabled = genresAreSelected()
    }

    private fun genresAreSelected() = genreOptionsChipGroup.checkedChipIds.size > 0

}
