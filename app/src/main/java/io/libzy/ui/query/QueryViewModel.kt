package io.libzy.ui.query

import androidx.lifecycle.viewModelScope
import io.libzy.analytics.AnalyticsDispatcher
import io.libzy.domain.Query
import io.libzy.recommendation.RecommendationService
import io.libzy.repository.UserLibraryRepository
import io.libzy.ui.common.LibzyViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

class QueryViewModel @Inject constructor(
    userLibraryRepository: UserLibraryRepository,
    private val recommendationService: RecommendationService,
    private val analyticsDispatcher: AnalyticsDispatcher
) : LibzyViewModel<QueryUiState, QueryUiEvent>() {

    override val initialUiState = QueryUiState()

    private val libraryAlbums = userLibraryRepository.albums.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    fun sendQuestionViewAnalyticsEvent() {
        with(uiState.value) {
            analyticsDispatcher.sendViewQuestionEvent(
                questionName = currentStep.stringValue,
                questionNum = currentStepIndex + 1,
                totalQuestions = querySteps.size
            )
        }
    }

    fun goToPreviousStep() {
        goToStep(uiState.value.currentStepIndex - 1)
    }

    fun goToNextStep() {
        with(uiState.value) {
            val newStepIndex = currentStepIndex + 1
            if (newStepIndex == querySteps.size) {
                analyticsDispatcher.sendSubmitQueryEvent(query)
                produceUiEvent(QueryUiEvent.SUBMIT_QUERY)
            } else {
                goToStep(newStepIndex)
            }
        }
    }

    private fun goToStep(newStepIndex: Int) {
        updateUiState {
            copy(
                previousStepIndex = currentStepIndex,
                currentStepIndex = newStepIndex.coerceIn(querySteps.indices)
            ).withUpdatedGenresIfNecessary()
        }
        sendQuestionViewAnalyticsEvent()
    }

    private fun QueryUiState.withUpdatedGenresIfNecessary() =
        when (currentStep) {
            QueryStep.GENRES -> {
                val recommendedGenres = recommendationService.recommendGenres(query, libraryAlbums.value)
                copy(
                    recommendedGenres = recommendedGenres,
                    query = query.copy(
                        genres = query.genres
                            .orEmpty()
                            .intersect(recommendedGenres.take(30)) // TODO: remove magic number
                            .takeUnless { it.isEmpty() }
                    )
                )
            }
            else -> this
        }

    fun setFamiliarity(familiarity: Query.Familiarity?) {
        updateUiState {
            copy(query = query.copy(familiarity = familiarity))
        }
    }

    fun setInstrumental(instrumental: Boolean?) {
        updateUiState {
            copy(query = query.copy(instrumental = instrumental))
        }
    }

    fun setAcousticness(acousticness: Float?) {
        updateUiState {
            copy(query = query.copy(acousticness = acousticness))
        }
    }

    fun setValence(valence: Float?) {
        updateUiState {
            copy(query = query.copy(valence = valence))
        }
    }

    fun setEnergy(energy: Float?) {
        updateUiState {
            copy(query = query.copy(energy = energy))
        }
    }

    fun setDanceability(danceability: Float?) {
        updateUiState {
            copy(query = query.copy(danceability = danceability))
        }
    }

    fun setGenres(genres: Set<String>?) {
        updateUiState {
            copy(query = query.copy(genres = genres?.takeUnless { it.isEmpty() }))
        }
    }

    fun addGenre(genre: String) {
        setGenres(uiState.value.query.genres.orEmpty().plus(genre))
    }

    fun removeGenre(genre: String) {
        uiState.value.query.genres?.let {
            setGenres(it.minus(genre))
        }
    }
}
