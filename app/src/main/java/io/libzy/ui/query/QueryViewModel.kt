package io.libzy.ui.query

import androidx.lifecycle.viewModelScope
import io.libzy.analytics.AnalyticsDispatcher
import io.libzy.domain.Query
import io.libzy.recommendation.RecommendationService
import io.libzy.repository.UserLibraryRepository
import io.libzy.ui.common.ScreenViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

class QueryViewModel @Inject constructor(
    userLibraryRepository: UserLibraryRepository,
    private val recommendationService: RecommendationService,
    private val analyticsDispatcher: AnalyticsDispatcher
) : ScreenViewModel<QueryUiState, QueryUiEvent>() {

    override val initialUiState = QueryUiState()

    private val libraryAlbums = userLibraryRepository.albums.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    fun goToPreviousStep() {
        goToStep(uiState.value.currentStepIndex - 1)
    }

    fun goToNextStep() {
        with(uiState.value) {
            val newStepIndex = currentStepIndex + 1
            if (newStepIndex == querySteps.size) {
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
            ).withRecommendedGenresIfNecessary()
        }

        with(uiState.value) {
            analyticsDispatcher.sendViewQuestionEvent(
                questionName = currentStep.stringValue,
                questionNum = currentStepIndex + 1,
                totalQuestions = querySteps.size
            )
        }
    }

    private fun QueryUiState.withRecommendedGenresIfNecessary() =
        when (currentStep) {
            QueryStep.GENRES -> copy(
                recommendedGenres = recommendationService.recommendGenres(
                    query = query,
                    libraryAlbums = libraryAlbums.value
                )
            // TODO: remove any selected genres from query that are not in the newly recommended genres?
            //  or should the algorithm handle selecting ANY genres, not just those recommended?
            //  if the latter, then we should display the selected genres to the user in QueryStep
            )
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
            copy(query = query.copy(genres = genres))
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
