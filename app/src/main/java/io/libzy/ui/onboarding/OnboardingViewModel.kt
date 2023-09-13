package io.libzy.ui.onboarding

import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.viewModelScope
import io.libzy.repository.SessionRepository
import io.libzy.repository.UserLibraryRepository
import io.libzy.spotify.remote.SpotifyAppRemoteService
import io.libzy.ui.common.StateOnlyViewModel
import io.libzy.ui.common.component.toUiState
import io.libzy.ui.onboarding.OnboardingUiEvent.CompleteOnboarding
import io.libzy.ui.onboarding.OnboardingUiEvent.PlayAlbum
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

class OnboardingViewModel @Inject constructor(
    private val userLibraryRepository: UserLibraryRepository,
    private val sessionRepository: SessionRepository,
    private val spotifyAppRemoteService: SpotifyAppRemoteService
) : StateOnlyViewModel<OnboardingUiState>(), DefaultLifecycleObserver {

    override val initialUiState = OnboardingUiState(onboardingMandatory = !sessionRepository.isOnboardingCompleted())

    init {
        viewModelScope.launch(Dispatchers.Default) {
            populateRandomAlbumArtUrls()
            populateAlbumArtUrlsForExampleRecommendations()
            createExampleAlbumResult()
        }
    }

    private suspend fun populateRandomAlbumArtUrls() {
        val albums = userLibraryRepository.albums.first()
        updateUiState {
            copy(
                randomAlbumArtUrls = albums
                    .shuffled()
                    .take(UserLibraryRepository.MINIMUM_NUM_ALBUMS_SAVED)
                    .mapNotNull { it.artworkUrl }
            )
        }
    }

    private suspend fun populateAlbumArtUrlsForExampleRecommendations() {
        val albums = userLibraryRepository.albums.first()
        val genres = userLibraryRepository.genres.first()
        val numAlbumsPerGenre = 6
        val albumsWithArtwork = albums.filter { it.artworkUrl != null }

        updateUiState {
            copy(
                albumArtUrlsForExampleRecommendations = genres
                    .take(10)
                    .map { genre ->
                        val notEnoughAlbumsHaveThisGenre = albums.count { genre in it.genres } < numAlbumsPerGenre
                        albumsWithArtwork
                            .filter { genre in it.genres || notEnoughAlbumsHaveThisGenre }
                            .mapNotNull { it.artworkUrl }
                            .shuffled()
                            .take(numAlbumsPerGenre)
                    }
            )
        }
    }

    private suspend fun createExampleAlbumResult() {
        val albums = userLibraryRepository.albums.first()
        updateUiState {
            copy(
                exampleAlbumRecommendation = albums
                    .shuffled()
                    .maxByOrNull { it.familiarity.longTermFavorite }
                    ?.toUiState(clickEvent = PlayAlbum)
            )
        }
    }

    override fun onStart(owner: LifecycleOwner) {
        spotifyAppRemoteService.connect()
    }

    override fun onStop(owner: LifecycleOwner) {
        spotifyAppRemoteService.disconnect()
    }

    fun processEvent(event: OnboardingUiEvent) {
        when (event) {
            is PlayAlbum -> playAlbum()
            is CompleteOnboarding -> completeOnboarding()
        }
    }

    private fun playAlbum() {
        uiState.exampleAlbumRecommendation?.spotifyUri?.let {
            spotifyAppRemoteService.playAlbum(it, onFailure = {})
        }
    }

    private fun completeOnboarding() {
        viewModelScope.launch {
            sessionRepository.setOnboardingCompleted(true)
            updateUiState {
                copy(onboardingCompleted = true)
            }
        }
    }
}