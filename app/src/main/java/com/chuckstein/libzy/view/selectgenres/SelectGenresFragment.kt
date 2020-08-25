package com.chuckstein.libzy.view.selectgenres

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.doOnLayout
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.findNavController
import com.airbnb.paris.extensions.style
import com.chuckstein.libzy.R
import com.chuckstein.libzy.common.LibzyApplication
import com.facebook.shimmer.ShimmerFrameLayout
import com.google.android.material.chip.Chip
import javax.inject.Inject
import kotlin.random.Random
import kotlinx.android.synthetic.main.fragment_select_genres.genre_options_chip_group as genreOptionsChipGroup
import kotlinx.android.synthetic.main.fragment_select_genres.genre_options_scroll_view as genreOptionsScrollView
import kotlinx.android.synthetic.main.fragment_select_genres.instructions_text as instructionsText

class SelectGenresFragment : Fragment() {

    @Inject
    lateinit var viewModelFactory: ViewModelProvider.Factory
    private val model by viewModels<SelectGenresViewModel> { viewModelFactory }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        (requireActivity().application as LibzyApplication).appComponent.inject(this)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_select_genres, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        if (model.genreOptions.value?.isEmpty() != false) createLoadingChips() // TODO: both the if condition and createLoadingChips take a few seconds, make it more efficient or do work on a background thread where I can
        model.genreOptions.observe(viewLifecycleOwner, Observer(::onGenreOptionsUpdated)) // TODO: figure out why this line takes a few seconds and fix it (NOTE: next one does not, interestingly)
        model.receivedSpotifyNetworkError.observe(viewLifecycleOwner, Observer { if (it) onSpotifyNetworkError() }) // TODO: abstract this
    }

    // TODO: Determine if there's a cleaner way to do this that doesn't involve guessing the number of chips to add then removing extras
    //       (or just calculate repeat # based on scroll view size and average chip size?)
    private fun createLoadingChips() {
        repeat(50) {
            // chip container to make it shimmer to indicate loading
            val chipContainer = ShimmerFrameLayout(requireContext())
            chipContainer.addView(Chip(requireContext()).apply {
                // random-length spacing text for random chip size
                text = "\t".repeat(Random.nextInt(5, 20))
                isClickable = false
            })
            genreOptionsChipGroup.addView(chipContainer)
        }

        fun chipIsOffScreen(chipIndex: Int) =
            chipIndex > 0 && genreOptionsChipGroup.getChildAt(chipIndex).bottom > genreOptionsScrollView.height

        genreOptionsScrollView.doOnLayout {
            // remove any chips beyond the bottom of the scroll view window
            var chipIndex = genreOptionsChipGroup.childCount - 1
            while (chipIsOffScreen(chipIndex)) {
                genreOptionsChipGroup.removeViewAt(chipIndex)
                chipIndex--
            }
        }
    }

    private fun onGenreOptionsUpdated(genreOptions: Map<String, Int>) {
        if (genreOptions.isNotEmpty()) {
            instructionsText.text = getString(R.string.select_genres_instructions_text)
            // TODO: only replace all views if the loading chips are present and not actual genre options (otherwise do a diff, handle new or removed genre options)
            genreOptionsChipGroup.removeAllViews() // ensure chip group is clear to display new genre options
            populateGenreOptionsChipGroup(genreOptions)
        }
    }

    private fun populateGenreOptionsChipGroup(genreOptions: Map<String, Int>) {

        fun viewGenreResults(genre: String) {
            val numResults = model.genreOptions.value?.get(genre)
                ?: throw IllegalStateException("Failed to retrieve number of albums associated with genre $genre")
            val submitGenresNavAction =
                SelectGenresFragmentDirections.actionSelectGenresFragmentToBrowseResultsFragment(genre, numResults)
            requireView().findNavController().navigate(submitGenresNavAction)
        }

        // a list of genres in the user's library, sorted by how many of their saved albums fit that genre, descending
        val orderedGenres = genreOptions.keys.sortedByDescending { genreOptions[it] }

        // TODO: fix skipped frames
        for (genre in orderedGenres.take(150)) { // TODO: need a RecyclerView to prevent the lag when adding full list (take(150) is just so it doesn't lag for the time being)
            with(Chip(requireContext())) {
                style(R.style.Chip) // TODO: partially works, but some material styling missing... probably an issue with the way I defined the style (or could it be because there are no shimmer wrappers? or no width/height/id? or maybe the style is just straight up right... but no the loading ones look bigger... could THAT be shimmer?)
                text = genre
                setOnClickListener { viewGenreResults(text.toString()) }
                genreOptionsChipGroup.addView(this)
            }
        }
    }

    // TODO: abstract this
    private fun onSpotifyNetworkError() {
        val networkErrorNavAction = SelectGenresFragmentDirections.actionSelectGenresFragmentToConnectSpotifyFragment()
        networkErrorNavAction.networkErrorReceived = true
        requireView().findNavController().navigate(networkErrorNavAction)
    }

}
