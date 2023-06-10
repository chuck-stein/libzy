package io.libzy.ui.findalbum.query

import androidx.lifecycle.viewModelScope
import io.libzy.analytics.AnalyticsDispatcher
import io.libzy.domain.Query
import io.libzy.persistence.database.tuple.LibraryAlbum
import io.libzy.persistence.prefs.DataStoreKeys.ENABLED_QUERY_PARAMS
import io.libzy.recommendation.RecommendationService
import io.libzy.repository.PreferencesRepository
import io.libzy.repository.UserLibraryRepository
import io.libzy.ui.common.LibzyViewModel
import io.libzy.ui.findalbum.query.QueryUiEvent.AddGenre
import io.libzy.ui.findalbum.query.QueryUiEvent.ChangeAcousticness
import io.libzy.ui.findalbum.query.QueryUiEvent.ChangeDanceability
import io.libzy.ui.findalbum.query.QueryUiEvent.ChangeEnergy
import io.libzy.ui.findalbum.query.QueryUiEvent.ChangeValence
import io.libzy.ui.findalbum.query.QueryUiEvent.Continue
import io.libzy.ui.findalbum.query.QueryUiEvent.GoBack
import io.libzy.ui.findalbum.query.QueryUiEvent.RemoveGenre
import io.libzy.ui.findalbum.query.QueryUiEvent.SelectFamiliarity
import io.libzy.ui.findalbum.query.QueryUiEvent.SelectInstrumentalness
import io.libzy.ui.findalbum.query.QueryUiEvent.SelectNoPreference
import io.libzy.ui.findalbum.query.QueryUiEvent.SendDismissKeyboardAnalytics
import io.libzy.ui.findalbum.query.QueryUiEvent.StartGenreSearch
import io.libzy.ui.findalbum.query.QueryUiEvent.SubmitQuery
import io.libzy.ui.findalbum.query.QueryUiEvent.UpdateSearchQuery
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import javax.inject.Inject

class QueryViewModel @Inject constructor(
    userLibraryRepository: UserLibraryRepository,
    private val preferencesRepository: PreferencesRepository,
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
            preferencesRepository.prefsFlowOf(ENABLED_QUERY_PARAMS).collect { enabledParams ->
                initUiStateForEnabledQueryParams(enabledParams)
            }
        }
    }

    private fun initUiStateForEnabledQueryParams(enabledParams: Set<String>?) {
        val stepOrder = if (enabledParams == null) {
            QueryUiState.DEFAULT_STEP_ORDER
        } else {
            QueryUiState.DEFAULT_STEP_ORDER.filter { it.stringValue in enabledParams }
        }
        updateUiState {
            QueryUiState(
                stepOrder = stepOrder,
                currentStep = stepOrder.first().asDefaultQueryStep()
            )
        }
    }

    private fun Query.Parameter.asDefaultQueryStep() = when (this) {
        Query.Parameter.FAMILIARITY -> QueryStep.Familiarity
        Query.Parameter.INSTRUMENTALNESS -> QueryStep.Instrumentalness
        Query.Parameter.ACOUSTICNESS -> QueryStep.Acousticness
        Query.Parameter.VALENCE -> QueryStep.Valence
        Query.Parameter.ENERGY -> QueryStep.Energy
        Query.Parameter.DANCEABILITY -> QueryStep.Danceability
        Query.Parameter.GENRES -> QueryStep.Genres.Recommendations()
    }

    private fun collectGenreRecommendations(libraryAlbumsFlow: Flow<List<LibraryAlbum>>) {
        viewModelScope.launch {
            combine(uiStateFlow, libraryAlbumsFlow) { uiState, libraryAlbums ->
                recommendationService.recommendGenres(uiState.query, libraryAlbums)
            }.collect { recommendedGenres ->
                updateUiState {
                    copy(
                        currentStep = when (currentStep) {
                            is QueryStep.Genres.Recommendations -> currentStep.copy(genreOptions = recommendedGenres)
                            is QueryStep.Genres.Search -> currentStep.copy(
                                genreOptions = when {
                                    currentStep.searchQuery.isBlank() -> recommendedGenres
                                    else -> recommendedGenres.filter {
                                        it.contains(currentStep.searchQuery, ignoreCase = true)
                                    }
                                }
                            )
                            else -> currentStep
                        }
                    )
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
            is StartGenreSearch -> startGenreSearch()
            is UpdateSearchQuery -> searchGenres(event.searchQuery)
            is SendDismissKeyboardAnalytics -> sendDismissKeyboardAnalyticsEvent()
            is GoBack -> goBack()
            is Continue -> goToNextStep()
            is SelectNoPreference -> selectNoPreference()
        }

    }

    fun initCurrentStep() {
        updateUiState {
            copy(currentStep = currentStep.parameterType.asDefaultQueryStep())
        }
    }

    fun sendQuestionViewAnalyticsEvent() {
        with(uiState) {
            analyticsDispatcher.sendViewQuestionEvent(
                questionName = currentStep.parameterType.stringValue,
                questionNum = currentStepIndex + 1,
                totalQuestions = stepOrder.size
            )
        }
    }

    private fun sendDismissKeyboardAnalyticsEvent() {
        analyticsDispatcher.sendDismissKeyboardEvent(currentSearchQuery ?: "", currentlySelectedGenres)
    }

    private fun goToPreviousStep() {
        goToStep(uiState.currentStepIndex - 1)
    }

    private fun goToNextStep() {
        goToStep(uiState.currentStepIndex + 1)
    }

    private fun goToStep(newStepIndex: Int) {
        updateUiState {
            copy(
                previousStepIndex = currentStepIndex,
                currentStep = stepOrder[newStepIndex.coerceIn(stepOrder.indices)].asDefaultQueryStep(),
            )
        }
        sendQuestionViewAnalyticsEvent()
    }

    private fun startGenreSearch() {
        uiState.currentStep.let { currentStep ->
            if (currentStep is QueryStep.Genres.Recommendations) {
                updateUiState {
                    copy(
                        currentStep = QueryStep.Genres.Search(
                            searchQuery = "",
                            genreOptions = currentStep.genreOptions // show recommendations when search query is blank
                        )
                    )
                }
                analyticsDispatcher.sendStartGenreSearchEvent(currentlySelectedGenres)
            }
        }
    }

    private fun stopGenreSearch() {
        if (uiState.currentStep is QueryStep.Genres.Search) {
            updateUiState {
                copy(currentStep = QueryStep.Genres.Recommendations())
            }
            analyticsDispatcher.sendStopGenreSearchEvent(currentlySelectedGenres)
        }
    }

    private fun searchGenres(searchQuery: String) {
        updateUiState {
            copy(
                currentStep = when (currentStep) {
                    is QueryStep.Genres.Search -> currentStep.copy(searchQuery = searchQuery)
                    else -> currentStep
                }
            )
        }
    }

    private fun goBack() {
        if (uiState.currentStep is QueryStep.Genres.Search) {
            stopGenreSearch()
        } else {
            goToPreviousStep()
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
            val newCurrentStep = when (currentStep) {
                is QueryStep.Genres.Recommendations -> {
                    val previousGenres = query.genres
                    val removedGenres = previousGenres?.minus(newGenres ?: emptySet()) ?: emptySet()
                    currentStep.copy(recentlyRemovedGenres = currentStep.recentlyRemovedGenres.plus(removedGenres))
                }
                else -> currentStep
            }
            copy(query = query.copy(genres = newGenres), currentStep = newCurrentStep)
        }
    }

    private fun addGenre(genre: String) {
        val selectedGenres = currentlySelectedGenres.plus(genre)
        setGenres(selectedGenres)

        analyticsDispatcher.sendSelectGenreEvent(
            genre = genre,
            fromCurrentOptions = genreInCurrentOptions(genre),
            currentlySearching = currentlySearchingGenres,
            currentSearchQuery = currentSearchQuery,
            currentlySelectedGenres = selectedGenres
        )
    }

    private fun removeGenre(genre: String) {
        uiState.query.genres?.let {
            val selectedGenres = it.minus(genre)
            setGenres(selectedGenres)

            analyticsDispatcher.sendDeselectGenreEvent(
                genre = genre,
                fromCurrentOptions = genreInCurrentOptions(genre),
                currentlySearching = currentlySearchingGenres,
                currentSearchQuery = currentSearchQuery,
                currentlySelectedGenres = selectedGenres
            )
        }
    }

    private fun genreInCurrentOptions(genre: String) =
        (uiState.currentStep as? QueryStep.Genres)?.run {
            genreOptions.take(numGenreOptionsToShow).contains(genre)
        } ?: false

    private val currentlySearchingGenres: Boolean
        get() = uiState.currentStep is QueryStep.Genres.Search

    private val currentSearchQuery: String?
        get() = (uiState.currentStep as? QueryStep.Genres.Search)?.searchQuery

    private val currentlySelectedGenres: Set<String>
        get() = uiState.query.genres.orEmpty()

    private fun selectNoPreference() {
        when (uiState.currentStep) {
            is QueryStep.Familiarity -> setFamiliarity(null)
            is QueryStep.Instrumentalness -> setInstrumental(null)
            is QueryStep.Acousticness -> setAcousticness(null)
            is QueryStep.Valence -> setValence(null)
            is QueryStep.Energy -> setEnergy(null)
            is QueryStep.Danceability -> setDanceability(null)
            is QueryStep.Genres -> setGenres(null)
        }
        if (uiState.onFinalStep) {
            produceUiEvent(SubmitQuery)
        } else {
            goToNextStep()
        }
    }
}
