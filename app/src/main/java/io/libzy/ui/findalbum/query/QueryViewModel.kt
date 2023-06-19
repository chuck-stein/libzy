package io.libzy.ui.findalbum.query

import androidx.lifecycle.viewModelScope
import io.libzy.analytics.AnalyticsDispatcher
import io.libzy.domain.Query
import io.libzy.domain.Query.Parameter.ACOUSTICNESS
import io.libzy.domain.Query.Parameter.DANCEABILITY
import io.libzy.domain.Query.Parameter.ENERGY
import io.libzy.domain.Query.Parameter.FAMILIARITY
import io.libzy.domain.Query.Parameter.GENRES
import io.libzy.domain.Query.Parameter.INSTRUMENTALNESS
import io.libzy.domain.Query.Parameter.VALENCE
import io.libzy.persistence.database.tuple.LibraryAlbum
import io.libzy.recommendation.RecommendationService
import io.libzy.repository.SettingsRepository
import io.libzy.repository.UserLibraryRepository
import io.libzy.ui.common.LibzyViewModel
import io.libzy.ui.findalbum.query.QueryUiEvent.AddGenre
import io.libzy.ui.findalbum.query.QueryUiEvent.ChangeAcousticness
import io.libzy.ui.findalbum.query.QueryUiEvent.ChangeDanceability
import io.libzy.ui.findalbum.query.QueryUiEvent.ChangeEnergy
import io.libzy.ui.findalbum.query.QueryUiEvent.ChangeValence
import io.libzy.ui.findalbum.query.QueryUiEvent.RemoveGenre
import io.libzy.ui.findalbum.query.QueryUiEvent.SelectFamiliarity
import io.libzy.ui.findalbum.query.QueryUiEvent.SelectInstrumentalness
import io.libzy.ui.findalbum.query.QueryUiEvent.SelectNoPreference
import io.libzy.ui.findalbum.query.QueryUiEvent.SendDismissKeyboardAnalytics
import io.libzy.ui.findalbum.query.QueryUiEvent.SendQuestionViewAnalytics
import io.libzy.ui.findalbum.query.QueryUiEvent.SendSubmitQueryAnalytics
import io.libzy.ui.findalbum.query.QueryUiEvent.StartSearchingGenres
import io.libzy.ui.findalbum.query.QueryUiEvent.StopSearchingGenres
import io.libzy.ui.findalbum.query.QueryUiEvent.SubmitQuery
import io.libzy.ui.findalbum.query.QueryUiEvent.UpdateSearchQuery
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.time.Duration.Companion.milliseconds

class QueryViewModel @Inject constructor(
    userLibraryRepository: UserLibraryRepository,
    private val settingsRepository: SettingsRepository,
    private val recommendationService: RecommendationService,
    private val analyticsDispatcher: AnalyticsDispatcher
) : LibzyViewModel<QueryUiState, QueryUiEvent>() {

    override val initialUiState = QueryUiState(loading = true)

    init {
        collectEnabledQueryParams()
        collectGenreRecommendations(userLibraryRepository.albums)
    }

    private fun collectEnabledQueryParams() {
        viewModelScope.launch {
            settingsRepository.enabledQueryParams.collect { enabledParams ->
                val stepOrder = if (enabledParams == null) {
                    Query.Parameter.defaultOrder
                } else {
                    Query.Parameter.defaultOrder.filter { it.stringValue in enabledParams }
                }
                updateUiState {
                    QueryUiState(stepOrder = stepOrder)
                }
            }
        }
    }

    private fun collectGenreRecommendations(libraryAlbumsFlow: Flow<List<LibraryAlbum>>) {
        viewModelScope.launch {
            uiStateFlow
                .map { it.query.copy(genres = null) }
                .combine(libraryAlbumsFlow) { query, libraryAlbums ->
                    recommendationService.recommendGenres(query, libraryAlbums)
                }.collect { recommendedGenres ->
                    updateUiState {
                        copy(genreOptions = recommendedGenres)
                    }
                }
        }
    }

    fun processEvent(event: QueryUiEvent.ForViewModel) {
        when (event) {
            is SelectFamiliarity -> setFamiliarity(event.familiarity)
            is SelectInstrumentalness -> setInstrumental(event.instrumental)
            is ChangeAcousticness -> setAcousticness(event.acousticness)
            is ChangeValence -> setValence(event.valence)
            is ChangeEnergy -> setEnergy(event.energy)
            is ChangeDanceability -> setDanceability(event.danceability)
            is AddGenre -> addGenre(event.genre)
            is RemoveGenre -> removeGenre(event.genre)
            is StartSearchingGenres -> startSearchingGenres()
            is StopSearchingGenres -> stopSearchingGenres(event.sendAnalytics, event.delayFirst)
            is UpdateSearchQuery -> searchGenres(event.searchQuery)
            is SelectNoPreference -> selectNoPreference(event.queryParam)
            is SendDismissKeyboardAnalytics -> sendDismissKeyboardAnalyticsEvent()
            is SendQuestionViewAnalytics -> sendQuestionViewAnalyticsEvent(event.queryParam)
            is SendSubmitQueryAnalytics -> analyticsDispatcher.sendSubmitQueryEvent(uiState.query)
        }
    }

    private fun sendQuestionViewAnalyticsEvent(queryParam: Query.Parameter) {
        with(uiState) {
            analyticsDispatcher.sendViewQuestionEvent(
                questionName = queryParam.stringValue,
                questionNum = stepOrder.indexOf(queryParam) + 1,
                totalQuestions = stepOrder.size
            )
        }
    }

    private fun sendDismissKeyboardAnalyticsEvent() {
        analyticsDispatcher.sendDismissKeyboardEvent(uiState.genreSearchQuery, currentlySelectedGenres)
    }

    private fun startSearchingGenres() {
        if (!uiState.searchingGenres) {
            updateUiState {
                copy(genreSearchState = GenreSearchState.Searching())
            }
            analyticsDispatcher.sendStartGenreSearchEvent(currentlySelectedGenres)
        }
    }

    private fun stopSearchingGenres(sendAnalytics: Boolean, delayFirst: Boolean) {
        if (uiState.searchingGenres) {
            viewModelScope.launch {
                if (delayFirst) {
                    delay(500.milliseconds)
                }
                updateUiState {
                    copy(genreSearchState = GenreSearchState.NotSearching())
                }
                if (sendAnalytics) {
                    analyticsDispatcher.sendStopGenreSearchEvent(currentlySelectedGenres)
                }
            }
        }
    }

    private fun searchGenres(searchQuery: String) {
        updateUiState {
            copy(
                genreSearchState = when (genreSearchState) {
                    is GenreSearchState.Searching -> genreSearchState.copy(searchQuery = searchQuery)
                    is GenreSearchState.NotSearching -> genreSearchState
                }
            )
        }
    }

    private fun setFamiliarity(familiarity: Query.Familiarity?) {
        updateUiState {
            copy(query = query.copy(familiarity = familiarity))
        }
    }

    private fun setInstrumental(instrumental: Boolean?) {
        updateUiState {
            copy(query = query.copy(instrumental = instrumental))
        }
    }

    private fun setAcousticness(acousticness: Float?) {
        updateUiState {
            copy(query = query.copy(acousticness = acousticness))
        }
    }

    private fun setValence(valence: Float?) {
        updateUiState {
            copy(query = query.copy(valence = valence))
        }
    }

    private fun setEnergy(energy: Float?) {
        updateUiState {
            copy(query = query.copy(energy = energy))
        }
    }

    private fun setDanceability(danceability: Float?) {
        updateUiState {
            copy(query = query.copy(danceability = danceability))
        }
    }

    private fun setGenres(genres: Set<String>?) {
        updateUiState {
            val newGenres = genres?.takeUnless { it.isEmpty() }
            val previousGenres = query.genres
            val removedGenres = previousGenres?.minus(newGenres.orEmpty()) ?: emptySet()

            copy(
                query = query.copy(genres = newGenres),
                genreSearchState = when (genreSearchState) {
                    is GenreSearchState.NotSearching -> genreSearchState.copy(
                        recentlyRemovedGenres = genreSearchState.recentlyRemovedGenres.plus(removedGenres)
                    )
                    else -> genreSearchState
                }
            )
        }
    }

    private fun addGenre(genre: String) {
        val selectedGenres = currentlySelectedGenres.plus(genre)
        setGenres(selectedGenres)

        analyticsDispatcher.sendSelectGenreEvent(
            genre = genre,
            currentlySearching = uiState.searchingGenres,
            currentSearchQuery = uiState.genreSearchQuery,
            currentlySelectedGenres = selectedGenres
        )
    }

    private fun removeGenre(genre: String) {
        uiState.query.genres?.let {
            val selectedGenres = it.minus(genre)
            setGenres(selectedGenres)

            analyticsDispatcher.sendDeselectGenreEvent(
                genre = genre,
                currentlySearching = uiState.searchingGenres,
                currentSearchQuery = uiState.genreSearchQuery,
                currentlySelectedGenres = selectedGenres
            )
        }
    }

    private val currentlySelectedGenres: Set<String>
        get() = uiState.query.genres.orEmpty()

    private fun selectNoPreference(queryParam: Query.Parameter) {
        when (queryParam) {
            FAMILIARITY -> setFamiliarity(null)
            INSTRUMENTALNESS -> setInstrumental(null)
            ACOUSTICNESS -> setAcousticness(null)
            VALENCE -> setValence(null)
            ENERGY -> setEnergy(null)
            DANCEABILITY -> setDanceability(null)
            GENRES -> setGenres(null)
        }
        val onFinalStep = uiState.stepOrder.indexOf(queryParam) == uiState.stepOrder.lastIndex
        if (onFinalStep) {
            produceUiEvent(SubmitQuery)
        }
    }
}
