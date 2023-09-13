package io.libzy.ui.library

import android.net.ConnectivityManager
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.lifecycle.viewModelScope
import androidx.work.ExistingPeriodicWorkPolicy.CANCEL_AND_REENQUEUE
import androidx.work.ExistingPeriodicWorkPolicy.KEEP
import androidx.work.WorkManager
import com.adamratzman.spotify.models.Album
import io.libzy.R
import io.libzy.analytics.AnalyticsConstants.EventProperties.ALBUM
import io.libzy.analytics.AnalyticsConstants.EventProperties.ENOUGH_ALBUMS_SAVED
import io.libzy.analytics.AnalyticsConstants.EventProperties.ID
import io.libzy.analytics.AnalyticsConstants.EventProperties.NUM_ALBUMS_REMAINING
import io.libzy.analytics.AnalyticsConstants.EventProperties.NUM_ALBUMS_SAVED
import io.libzy.analytics.AnalyticsConstants.EventProperties.NUM_ALBUMS_SAVED_IN_CACHE
import io.libzy.analytics.AnalyticsConstants.EventProperties.NUM_ALBUMS_SAVED_ON_SPOTIFY
import io.libzy.analytics.AnalyticsConstants.Events.REMOVE_ALBUM
import io.libzy.analytics.AnalyticsConstants.Events.SAVE_ALBUM
import io.libzy.analytics.AnalyticsConstants.Events.START_EXPAND_LIBRARY_AUTO_SYNC
import io.libzy.analytics.AnalyticsConstants.Events.VIEW_EXPAND_LIBRARY_SCREEN
import io.libzy.analytics.AnalyticsDispatcher
import io.libzy.domain.artworkUrl
import io.libzy.domain.describe
import io.libzy.persistence.database.tuple.LibraryAlbum
import io.libzy.recommendation.LibraryRecommendationService
import io.libzy.repository.UserLibraryRepository
import io.libzy.ui.common.StateOnlyViewModel
import io.libzy.ui.common.component.AlbumUiState
import io.libzy.ui.library.ExpandLibraryUiEvent.AwaitLibrarySync
import io.libzy.ui.library.ExpandLibraryUiEvent.DismissError
import io.libzy.ui.library.ExpandLibraryUiEvent.ExitApp
import io.libzy.ui.library.ExpandLibraryUiEvent.GoBack
import io.libzy.ui.library.ExpandLibraryUiEvent.Initialize
import io.libzy.ui.library.ExpandLibraryUiEvent.NavToQueryScreen
import io.libzy.ui.library.ExpandLibraryUiEvent.RecommendAlbums
import io.libzy.ui.library.ExpandLibraryUiEvent.Refresh
import io.libzy.ui.library.ExpandLibraryUiEvent.RemoveAlbum
import io.libzy.ui.library.ExpandLibraryUiEvent.SaveAlbum
import io.libzy.ui.theme.LibzyIconTheme
import io.libzy.util.TextResource
import io.libzy.util.networkConnectedFlow
import io.libzy.util.toTextResource
import io.libzy.work.enqueuePeriodicLibrarySync
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.time.Duration.Companion.seconds

class ExpandLibraryViewModel @Inject constructor(
    private val userLibraryRepository: UserLibraryRepository,
    private val libraryRecommendationService: LibraryRecommendationService,
    private val workManager: WorkManager,
    private val connectivityManager: ConnectivityManager,
    private val analytics: AnalyticsDispatcher
) : StateOnlyViewModel<ExpandLibraryUiState>(libraryRecommendationService) {

    override val initialUiState = ExpandLibraryUiState()

    private var refreshSchedulerJob: Job? = null
    private var refreshJob: Job? = null

    fun processEvent(event: ExpandLibraryUiEvent.ForViewModel) {
        when (event) {
            is Initialize -> initialize()
            is Refresh -> refresh()
            is RecommendAlbums -> recommendAlbums()
            is SaveAlbum -> saveAlbum(event.id)
            is RemoveAlbum -> removeAlbum(event.id)
            is AwaitLibrarySync -> awaitLibrarySync()
            is DismissError -> dismissError()
        }
    }

    private fun initialize() {
        analytics.sendEvent(eventName = VIEW_EXPAND_LIBRARY_SCREEN, eventProperties = getCommonAnalyticsProperties())

        viewModelScope.launch {
            setInitialNumAlbumsSaved()
        }
        viewModelScope.launch {
            monitorWhetherEnoughAlbumsSavedInCache()
        }
        viewModelScope.launch {
            monitorNetwork()
        }
        viewModelScope.launch {
            recommendAlbums()
        }
    }

    private suspend fun setInitialNumAlbumsSaved() {
        val initialNumAlbumsSaved = userLibraryRepository.albums.first().size
        updateUiState {
            copy(initialNumAlbumsSaved = initialNumAlbumsSaved)
        }
        updateNumAlbumsSaved(initialNumAlbumsSaved)
    }

    private suspend fun monitorWhetherEnoughAlbumsSavedInCache() {
        userLibraryRepository.enoughAlbumsSavedFlow.collect { enoughAlbumsSavedInCache ->
            updateUiState {
                copy(
                    doneSavingEvent = if (enoughAlbumsSavedInCache) NavToQueryScreen else AwaitLibrarySync,
                    backClickEvent = if (enoughAlbumsSavedInCache) GoBack else ExitApp,
                    enoughAlbumsSavedAndCached = enoughAlbumsSavedInCache
                )
            }
        }
    }

    private suspend fun monitorNetwork() {
        connectivityManager.networkConnectedFlow().collect { networkConnected ->
            updateUiState {
                copy(networkConnected = networkConnected)
            }
        }
    }

    /**
     * Ensure our count of how many albums the user has saved is up to date,
     * because if that count is too low this screen will block usage of the rest of the app.
     * While our count should ideally be accurate, it cannot be as accurate as the source of truth on Spotify's servers,
     * because we could always fail to write to disk when trying to update our cache.
     *
     * This should be called every time the screen is opened,
     * in case the user is coming back from the Spotify app where they were saving more albums.
     *
     * This refresh is on a timer, so that we are periodically fetching the latest count from Spotify,
     * as to not block app usage unnecessarily if one request fails.
     */
    private fun refresh() {
        refreshSchedulerJob?.cancel()
        refreshSchedulerJob = viewModelScope.launch {
            while (isActive) {
                refreshJob = launch {
                    val cachedLibraryAlbums = userLibraryRepository.albums.first()
                    highlightSavedAlbums(cachedLibraryAlbums)
                    val previousNumAlbumsSaved = uiState.numAlbumsSaved
                    val numAlbumsSaved = userLibraryRepository.getNumAlbumsSaved()
                    updateNumAlbumsSaved(numAlbumsSaved ?: cachedLibraryAlbums.size)
                    if (uiState.numAlbumsSaved != cachedLibraryAlbums.size) {
                        workManager.enqueuePeriodicLibrarySync(
                            existingWorkPolicy = when {
                                uiState.numAlbumsSaved > previousNumAlbumsSaved -> CANCEL_AND_REENQUEUE
                                else -> KEEP
                            }
                        )
                        analytics.sendEvent(
                            eventName = START_EXPAND_LIBRARY_AUTO_SYNC,
                            eventProperties = mapOf(
                                NUM_ALBUMS_SAVED_ON_SPOTIFY to uiState.numAlbumsSaved,
                                NUM_ALBUMS_SAVED_IN_CACHE to cachedLibraryAlbums.size
                            )
                        )
                    }
                }
                delay(30.seconds)
            }
        }
    }

    private fun recommendAlbums() = viewModelScope.launch {
        updateUiState {
            copy(fetchingMoreRecommendations = true)
        }
        val newPageOfRecommendedAlbums = libraryRecommendationService.recommendAlbums()
        if (newPageOfRecommendedAlbums != null) {
            updateUiState {
                copy(
                    recommendedAlbums = recommendedAlbums.orEmpty().plus(newPageOfRecommendedAlbums.map { it.toUiState() }),
                    albumsById = albumsById.plus(newPageOfRecommendedAlbums.associateBy { it.id }),
                    lastRecommendationPageSize = newPageOfRecommendedAlbums.size,
                    fetchingMoreRecommendations = false,
                    errorFetchingRecommendations = false
                )
            }
        } else {
            updateUiState {
                copy(errorFetchingRecommendations = true)
            }
            delay(1.seconds) // debounce on error
            updateUiState {
                copy(fetchingMoreRecommendations = false)
            }
        }
    }

    private fun saveAlbum(id: String) = viewModelScope.launch {
        val album = uiState.albumsById[id] ?: return@launch
        updateAlbumState(
            id = album.id,
            icon = LibzyIconTheme.Favorite,
            iconContentDescription = R.string.remove_album_cd.toTextResource(),
            isHighlighted = true,
            clickEvent = null
        )
        analytics.sendEvent(
            eventName = SAVE_ALBUM,
            eventProperties = getCommonAnalyticsProperties() + mapOf(ID to id, ALBUM to album.describe())
        )
        refreshJob?.join()
        updateNumAlbumsSaved(uiState.numAlbumsSaved + 1)

        val albumSavedSuccessfully = userLibraryRepository.saveAlbum(album)

        updateAlbumState(
            id = album.id,
            icon = if (albumSavedSuccessfully) LibzyIconTheme.Favorite else LibzyIconTheme.FavoriteBorder,
            iconContentDescription = if (albumSavedSuccessfully) R.string.remove_album_cd.toTextResource() else R.string.save_album_cd.toTextResource(),
            isHighlighted = albumSavedSuccessfully,
            clickEvent = if (albumSavedSuccessfully) RemoveAlbum(id) else SaveAlbum(id)
        )
        if (!albumSavedSuccessfully) {
            updateNumAlbumsSaved(uiState.numAlbumsSaved - 1)
        }
    }

    private fun removeAlbum(id: String) = viewModelScope.launch {
        val album = uiState.albumsById[id] ?: return@launch
        updateAlbumState(
            id = album.id,
            icon = LibzyIconTheme.FavoriteBorder,
            iconContentDescription = R.string.save_album_cd.toTextResource(),
            isHighlighted = false,
            clickEvent = null
        )
        analytics.sendEvent(
            eventName = REMOVE_ALBUM,
            eventProperties = getCommonAnalyticsProperties() + mapOf(ID to id, ALBUM to album.describe())
        )
        refreshJob?.join()
        updateNumAlbumsSaved(uiState.numAlbumsSaved - 1)

        val albumRemovedSuccessfully = userLibraryRepository.removeAlbum(album)

        updateAlbumState(
            id = album.id,
            icon = if (albumRemovedSuccessfully) LibzyIconTheme.FavoriteBorder else LibzyIconTheme.Favorite,
            iconContentDescription = if (albumRemovedSuccessfully) R.string.save_album_cd.toTextResource() else R.string.remove_album_cd.toTextResource(),
            isHighlighted = !albumRemovedSuccessfully,
            clickEvent = if (albumRemovedSuccessfully) SaveAlbum(id) else RemoveAlbum(id)
        )
        if (!albumRemovedSuccessfully) {
            updateNumAlbumsSaved(uiState.numAlbumsSaved + 1)
        }
    }

    private fun awaitLibrarySync() = viewModelScope.launch {
        updateUiState {
            copy(awaitingLibrarySync = true)
        }
    }

    private fun dismissError() {
        updateUiState {
            copy(errorFetchingRecommendations = false)
        }
    }

    private fun updateNumAlbumsSaved(numAlbumsSaved: Int) {
        updateUiState {
            val enoughAlbumsSaved = numAlbumsSaved >= UserLibraryRepository.MINIMUM_NUM_ALBUMS_SAVED
            copy(
                loading = false,
                enoughAlbumsSaved = enoughAlbumsSaved,
                awaitingLibrarySync = if (!enoughAlbumsSaved) false else awaitingLibrarySync,
                numAlbumsSaved = numAlbumsSaved,
                savedAlbumsText = TextResource.Plural(
                    R.plurals.albums_in_your_collection,
                    numAlbumsSaved,
                    numAlbumsSaved
                ),
                headerText = if (enoughAlbumsSaved) {
                    R.string.add_more_albums_header.toTextResource()
                } else {
                    val numAlbumsRemaining = UserLibraryRepository.MINIMUM_NUM_ALBUMS_SAVED - numAlbumsSaved
                    TextResource.Plural(R.plurals.not_enough_albums_header, numAlbumsRemaining, numAlbumsRemaining)
                }
            )
        }
    }

    private fun updateAlbumState(
        id: String,
        icon: ImageVector,
        iconContentDescription: TextResource,
        isHighlighted: Boolean,
        clickEvent: Any?
    ) {
        updateUiState {
            copy(
                recommendedAlbums = recommendedAlbums?.map { album ->
                    if (album.spotifyId == id) {
                        album.copy(
                            icon = icon,
                            iconContentDescription = iconContentDescription,
                            clickEvent = clickEvent,
                            isHighlighted = isHighlighted
                        )
                    } else {
                        album
                    }
                }
            )
        }
    }

    private fun highlightSavedAlbums(libraryAlbums: List<LibraryAlbum>) {
        updateUiState {
            copy(
                recommendedAlbums = recommendedAlbums?.map { albumState ->
                    val isSaved = libraryAlbums.find { it.spotifyId == albumState.spotifyId } != null
                    albumState.copy(
                        isHighlighted = isSaved,
                        icon = if (isSaved) LibzyIconTheme.Favorite else LibzyIconTheme.FavoriteBorder,
                        iconContentDescription = if (isSaved) R.string.remove_album_cd.toTextResource() else R.string.save_album_cd.toTextResource(),
                        clickEvent = if (isSaved) RemoveAlbum(albumState.spotifyId) else SaveAlbum(albumState.spotifyId)
                    )
                }
            )
        }
    }

    private fun Album.toUiState() = AlbumUiState(
        title = name.toTextResource(),
        artists = artists.joinToString { it.name }.toTextResource(),
        spotifyId = id,
        spotifyUri = uri.uri,
        artworkUrl = artworkUrl,
        icon = LibzyIconTheme.FavoriteBorder,
        iconContentDescription = R.string.save_album_cd.toTextResource(),
        clickEvent = SaveAlbum(id)
    )

    private fun getCommonAnalyticsProperties() = with(uiState) {
        mapOf(
            NUM_ALBUMS_SAVED to numAlbumsSaved,
            ENOUGH_ALBUMS_SAVED to enoughAlbumsSavedAndCached,
            NUM_ALBUMS_REMAINING to UserLibraryRepository.MINIMUM_NUM_ALBUMS_SAVED - numAlbumsSaved
        )
    }
}