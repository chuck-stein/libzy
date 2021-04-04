package io.libzy.view.query

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.content.res.ResourcesCompat
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.GridLayoutManager
import com.bumptech.glide.Glide
import io.libzy.LibzyApplication
import io.libzy.R
import io.libzy.analytics.AnalyticsDispatcher
import io.libzy.model.AlbumResult
import timber.log.Timber
import javax.inject.Inject
import kotlin.math.roundToInt
import kotlinx.android.synthetic.main.fragment_results.albums_recycler as albumsRecycler
import kotlinx.android.synthetic.main.fragment_results.back_button as backButton
import kotlinx.android.synthetic.main.fragment_results.rating_bar as ratingBar
import kotlinx.android.synthetic.main.fragment_results.rating_section as ratingSection
import kotlinx.android.synthetic.main.fragment_results.results_header as resultsHeader

class ResultsFragment : Fragment() {

    companion object {
        // TODO: determine this based on screen size instead of hardcoding it
        private const val NUM_PLACEHOLDER_RESULTS = 50
    }

    @Inject
    lateinit var viewModelFactory: ViewModelProvider.Factory
    private val model by activityViewModels<QueryResultsViewModel> { viewModelFactory }

    @Inject
    lateinit var analyticsDispatcher: AnalyticsDispatcher
    
    private lateinit var albumsRecyclerAdapter: AlbumsRecyclerAdapter

    /**
     * Whether the user has interacted with the results rating bar,
     * but their rating has not yet been submitted as an analytics event.
     */
    private var resultsRatingPendingSubmission = false

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
        initAlbumsRecycler()

        backButton.setOnClickListener { requireActivity().onBackPressedDispatcher.onBackPressed() }
        ratingBar.setOnRatingBarChangeListener { _, _, _ -> resultsRatingPendingSubmission = true  }

        // TODO: move observer lambda to its own function
        model.recommendedAlbums.observe(viewLifecycleOwner, { albums ->
            albumsRecyclerAdapter.albums = albums
            ratingSection.isVisible = albums.isNotEmpty()
            if (albums.isEmpty()) resultsHeader.text = getString(R.string.no_results_header)
        })
    }

    private fun initAlbumsRecycler() {
        val placeholderAlbumArt =
            ResourcesCompat.getDrawable(resources, R.drawable.placeholder_album_art, requireContext().theme)
        albumsRecyclerAdapter = AlbumsRecyclerAdapter(Glide.with(this), placeholderAlbumArt, ::onAlbumClicked)
        albumsRecyclerAdapter.albums = List(NUM_PLACEHOLDER_RESULTS) {
            AlbumResult("Fetching album data", "Please wait...", isPlaceholder = true)
        }
        albumsRecycler.adapter = albumsRecyclerAdapter
        albumsRecycler.layoutManager = GridLayoutManager(
            requireContext(),
            3
        ) // TODO: either don't hardcode spanCount, make it based on screen size
    }

    private fun onAlbumClicked(spotifyUri: String) {
        analyticsDispatcher.sendPlayAlbumEvent(spotifyUri)
        try {
            model.playAlbum(spotifyUri)
        } catch (e: Exception) {
            Timber.e(e, "Failed to play album remotely")
            Toast.makeText(requireContext(), R.string.toast_spotify_remote_failed, Toast.LENGTH_LONG).show()
        }
    }

    override fun onStart() {
        super.onStart()
        model.connectSpotifyAppRemote {
            // TODO: handle connection failure: https://chilipot.atlassian.net/browse/LIB-253
        }
    }

    override fun onStop() {
        super.onStop()
        model.disconnectSpotifyAppRemote()
        sendResultsRating()
    }

    private fun sendResultsRating() {
        if (resultsRatingPendingSubmission) {
            analyticsDispatcher.sendRateAlbumResultsEvent(ratingBar.rating.roundToInt())
            resultsRatingPendingSubmission = false
        }
    }

}
