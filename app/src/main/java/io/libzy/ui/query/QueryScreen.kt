package io.libzy.ui.query

import androidx.activity.compose.BackHandler
import androidx.annotation.StringRes
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Button
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Slider
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.constraintlayout.compose.ConstraintLayout
import androidx.constraintlayout.compose.Dimension
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.google.accompanist.flowlayout.FlowRow
import io.libzy.R
import io.libzy.domain.Query
import io.libzy.ui.Destination.Results.navigateToResultsScreen
import io.libzy.ui.LibzyContent
import io.libzy.ui.common.component.BackIcon
import io.libzy.ui.common.component.Chip
import io.libzy.ui.common.component.EventHandler
import io.libzy.ui.common.component.LibzyScaffold
import io.libzy.ui.theme.LibzyDimens.BUTTON_GROUP_HORIZONTAL_INSET
import io.libzy.ui.theme.LibzyDimens.BUTTON_GROUP_SPACING
import io.libzy.ui.theme.LibzyDimens.HORIZONTAL_INSET
import timber.log.Timber
import java.time.LocalTime

/**
 * **Stateful** Query Screen, displaying a series of questions about what the user is in the mood to listen to.
 */
@ExperimentalAnimationApi
@Composable
fun QueryScreen(navController: NavController, viewModelFactory: ViewModelProvider.Factory) {
    val viewModel: QueryViewModel = viewModel(factory = viewModelFactory)
    val uiState by viewModel.uiState

    EventHandler(viewModel.uiEvents) {
        when (it) {
            QueryUiEvent.SUBMIT_QUERY -> navController.navigateToResultsScreen(uiState.query)
        }
    }

    LaunchedEffect(Unit) {
        viewModel.sendQuestionViewAnalyticsEvent()
    }

    QueryScreen(
        uiState = uiState,
        onBackClick = viewModel::goToPreviousStep,
        onNoPreferenceClick = {
            when (uiState.currentStep) {
                QueryStep.FAMILIARITY -> viewModel.setFamiliarity(null)
                QueryStep.INSTRUMENTALNESS -> viewModel.setInstrumental(null)
                QueryStep.ACOUSTICNESS -> viewModel.setAcousticness(null)
                QueryStep.VALENCE -> viewModel.setValence(null)
                QueryStep.ENERGY -> viewModel.setEnergy(null)
                QueryStep.DANCEABILITY -> viewModel.setDanceability(null)
                QueryStep.GENRES -> viewModel.setGenres(null)
            }
            viewModel.goToNextStep()
        },
        onCurrentFavoriteClick = {
            viewModel.setFamiliarity(Query.Familiarity.CURRENT_FAVORITE)
            viewModel.goToNextStep()
        },
        onReliableClassicClick = {
            viewModel.setFamiliarity(Query.Familiarity.RELIABLE_CLASSIC)
            viewModel.goToNextStep()
        },
        onUnderappreciatedGemClick = {
            viewModel.setFamiliarity(Query.Familiarity.UNDERAPPRECIATED_GEM)
            viewModel.goToNextStep()
        },
        onInstrumentalClick = {
            viewModel.setInstrumental(true)
            viewModel.goToNextStep()
        },
        onVocalClick = {
            viewModel.setInstrumental(false)
            viewModel.goToNextStep()
        },
        onContinueClick = { submittedValue ->

            fun logUnexpectedClick() {
                Timber.w("Continue button was clicked on the ${uiState.currentStep.stringValue} step, where it should not be visible")
            }

            when (uiState.currentStep) {
                QueryStep.ACOUSTICNESS -> viewModel.setAcousticness(1 - submittedValue) // lower value = more acoustic
                QueryStep.VALENCE -> viewModel.setValence(submittedValue)
                QueryStep.ENERGY -> viewModel.setEnergy(submittedValue)
                QueryStep.DANCEABILITY -> viewModel.setDanceability(submittedValue)
                else -> logUnexpectedClick()
            }
            viewModel.goToNextStep()
        },
        onSelectGenre = viewModel::addGenre,
        onDeselectGenre = viewModel::removeGenre,
        onReadyClick = viewModel::goToNextStep
    )
}

/**
 * **Stateless** Query Screen, displaying a series of questions about what the user is in the mood to listen to.
 */
@ExperimentalAnimationApi
@Composable
private fun QueryScreen(
    uiState: QueryUiState,
    onBackClick: () -> Unit,
    onNoPreferenceClick: () -> Unit,
    onCurrentFavoriteClick: () -> Unit,
    onReliableClassicClick: () -> Unit,
    onUnderappreciatedGemClick: () -> Unit,
    onInstrumentalClick: () -> Unit,
    onVocalClick: () -> Unit,
    onContinueClick: (Float) -> Unit,
    onSelectGenre: (String) -> Unit,
    onDeselectGenre: (String) -> Unit,
    onReadyClick: () -> Unit
) {
    val canGoToPreviousQueryStep = uiState.currentStepIndex > 0
    BackHandler(enabled = canGoToPreviousQueryStep, onBack = onBackClick)

    LibzyScaffold(
        navigationIcon = {
            AnimatedVisibility(visible = canGoToPreviousQueryStep, enter = fadeIn(), exit = fadeOut()) {
                BackIcon(onBackClick, enabled = canGoToPreviousQueryStep)
            }
        },
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxSize()) {
            GreetingText()
            Text(
                text = stringResource(R.string.query_instructions_text),
                style = MaterialTheme.typography.h6,
                modifier = Modifier.padding(top = 24.dp, start = HORIZONTAL_INSET.dp, end = HORIZONTAL_INSET.dp)
            )
            CurrentQueryStep(
                uiState,
                onCurrentFavoriteClick,
                onReliableClassicClick,
                onUnderappreciatedGemClick,
                onInstrumentalClick,
                onVocalClick,
                onContinueClick,
                onSelectGenre,
                onDeselectGenre,
                onReadyClick,
                modifier = Modifier.weight(1f)
            )
            Button(
                onNoPreferenceClick,
                modifier = Modifier.padding(bottom = 24.dp, start = HORIZONTAL_INSET.dp, end = HORIZONTAL_INSET.dp)
            ) {
                Text(stringResource(R.string.no_preference))
            }
        }
    }
}

@Composable
private fun GreetingText() {
    val greetingResId = when (LocalTime.now().hour) {
        in 4..11 -> R.string.morning_greeting_text
        in 12..16 -> R.string.afternoon_greeting_text
        else -> R.string.evening_greeting_text
    }
    Text(
        stringResource(greetingResId),
        style = MaterialTheme.typography.h3,
        modifier = Modifier.padding(horizontal = HORIZONTAL_INSET.dp)
    )
}

@ExperimentalAnimationApi
@Composable
private fun CurrentQueryStep(
    uiState: QueryUiState,
    onCurrentFavoriteClick: () -> Unit,
    onReliableClassicClick: () -> Unit,
    onUnderappreciatedGemClick: () -> Unit,
    onInstrumentalClick: () -> Unit,
    onVocalClick: () -> Unit,
    onContinueClick: (Float) -> Unit,
    onSelectGenre: (String) -> Unit,
    onDeselectGenre: (String) -> Unit,
    onReadyClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    fun Float?.toSliderValue(flipValueDirection: Boolean = false) = when {
        this == null -> 0.5f
        flipValueDirection -> 1 - this
        else -> this
    }

    Box(modifier) {
        QueryStepAnimator(uiState, QueryStep.FAMILIARITY) {
            FamiliarityStep(
                onCurrentFavoriteClick,
                onReliableClassicClick,
                onUnderappreciatedGemClick
            )
        }

        QueryStepAnimator(uiState, QueryStep.INSTRUMENTALNESS) {
            InstrumentalnessStep(onInstrumentalClick, onVocalClick)
        }

        QueryStepAnimator(uiState, QueryStep.ACOUSTICNESS) {
            SliderQueryStep(
                initialSliderValue = uiState.query.acousticness.toSliderValue(flipValueDirection = true),
                leftLabelResId = R.string.acoustic,
                rightLabelResId = R.string.electric_electronic,
                onContinueClick = onContinueClick
            )
        }

        QueryStepAnimator(uiState, QueryStep.VALENCE) {
            SliderQueryStep(
                initialSliderValue = uiState.query.valence.toSliderValue(),
                leftLabelResId = R.string.negative_emotion,
                rightLabelResId = R.string.positive_emotion,
                onContinueClick = onContinueClick
            )
        }

        QueryStepAnimator(uiState, QueryStep.ENERGY) {
            SliderQueryStep(
                initialSliderValue = uiState.query.energy.toSliderValue(),
                leftLabelResId = R.string.chill,
                rightLabelResId = R.string.energetic,
                onContinueClick = onContinueClick
            )
        }

        QueryStepAnimator(uiState, QueryStep.DANCEABILITY) {
            SliderQueryStep(
                initialSliderValue = uiState.query.danceability.toSliderValue(),
                leftLabelResId = R.string.arrhythmic,
                rightLabelResId = R.string.danceable,
                onContinueClick = onContinueClick
            )
        }

        QueryStepAnimator(uiState, QueryStep.GENRES) {
            GenresStep(
                genreOptions = uiState.recommendedGenres,
                selectedGenres = uiState.query.genres.orEmpty(),
                onSelectGenre = onSelectGenre,
                onDeselectGenre = onDeselectGenre,
                onReadyClick = onReadyClick
            )
        }
    }
}

@ExperimentalAnimationApi
@Composable
private fun QueryStepAnimator(
    uiState: QueryUiState,
    stepToAnimate: QueryStep,
    stepContent: @Composable AnimatedVisibilityScope.() -> Unit
) {
    val slideOppositeDirection = with(uiState) { previousStepIndex != null && previousStepIndex > currentStepIndex }
    val slideOffsetMultiplier = if (slideOppositeDirection) -1 else 1

    AnimatedVisibility(
        visible = uiState.currentStep == stepToAnimate,
        enter = slideInHorizontally(
            initialOffsetX = { contentWidth -> contentWidth * slideOffsetMultiplier }
        ),
        exit = slideOutHorizontally(
            targetOffsetX = { contentWidth -> -contentWidth * slideOffsetMultiplier }
        )
    ) {
        Box(Modifier.fillMaxSize().padding(horizontal = HORIZONTAL_INSET.dp), contentAlignment = Alignment.Center) {
            stepContent()
        }
    }
}

@Composable
private fun ButtonGroupSpacer() {
    Spacer(modifier = Modifier.height(BUTTON_GROUP_SPACING.dp))
}

private val BUTTON_IN_GROUP_MODIFIER = Modifier.fillMaxWidth().padding(horizontal = BUTTON_GROUP_HORIZONTAL_INSET.dp)

@Composable
private fun FamiliarityStep(
    onCurrentFavoriteClick: () -> Unit,
    onReliableClassicClick: () -> Unit,
    onUnderappreciatedGemClick: () -> Unit
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Button(onCurrentFavoriteClick, modifier = BUTTON_IN_GROUP_MODIFIER) {
            Text(stringResource(R.string.a_current_favorite))
        }

        ButtonGroupSpacer()

        Button(onReliableClassicClick, modifier = BUTTON_IN_GROUP_MODIFIER) {
            Text(stringResource(R.string.a_reliable_classic))
        }

        ButtonGroupSpacer()

        Button(onUnderappreciatedGemClick, modifier = BUTTON_IN_GROUP_MODIFIER) {
            Text(stringResource(R.string.an_underappreciated_gem))
        }
    }
}

@Composable
private fun InstrumentalnessStep(onInstrumentalClick: () -> Unit, onVocalClick: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Button(onInstrumentalClick, modifier = BUTTON_IN_GROUP_MODIFIER) {
            Text(stringResource(R.string.instrumental))
        }

        ButtonGroupSpacer()

        Button(onVocalClick, modifier = BUTTON_IN_GROUP_MODIFIER) {
            Text(stringResource(R.string.vocal))
        }
    }
}

@Composable
private fun SliderQueryStep(
    initialSliderValue: Float,
    @StringRes leftLabelResId: Int,
    @StringRes rightLabelResId: Int,
    onContinueClick: (Float) -> Unit
) {
    var sliderValue by remember { mutableStateOf(initialSliderValue) }
    val horizontalSpacing = 8.dp
    val verticalSpacing = 24.dp

    ConstraintLayout {
        val (leftLabel, rightLabel, slider, continueButton) = createRefs()

        Slider(
            value = sliderValue,
            onValueChange = { sliderValue = it },
            modifier = Modifier.width(150.dp).constrainAs(slider) {
                centerTo(parent)
            }
        )
        Text(
            stringResource(leftLabelResId),
            style = MaterialTheme.typography.body2,
            modifier = Modifier.fillMaxWidth().constrainAs(leftLabel) {
                centerVerticallyTo(parent)
                start.linkTo(parent.start, margin = horizontalSpacing)
                end.linkTo(slider.start, margin = horizontalSpacing)
                width = Dimension.preferredWrapContent
            }
        )
        Text(
            stringResource(rightLabelResId),
            style = MaterialTheme.typography.body2,
            modifier = Modifier.fillMaxWidth().constrainAs(rightLabel) {
                centerVerticallyTo(parent)
                start.linkTo(slider.end, margin = horizontalSpacing)
                end.linkTo(parent.end, margin = horizontalSpacing)
                width = Dimension.preferredWrapContent
            }
        )
        Button(onClick = { onContinueClick(sliderValue) }, modifier = Modifier.constrainAs(continueButton) {
            centerHorizontallyTo(parent)
            top.linkTo(slider.bottom, margin = verticalSpacing)
            bottom.linkTo(parent.bottom, margin = verticalSpacing)
        }) {
            Text(stringResource(R.string.continue_button))
        }
    }
}

@Composable
private fun GenresStep(
    genreOptions: List<String>,
    selectedGenres: Set<String>,
    onSelectGenre: (String) -> Unit,
    onDeselectGenre: (String) -> Unit,
    onReadyClick: () -> Unit
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        // TODO: add visual scroll bar when it is supported
        FlowRow(
            modifier = Modifier.padding(vertical = 24.dp).verticalScroll(rememberScrollState()).weight(1f),
            mainAxisSpacing = 10.dp,
            crossAxisSpacing = 16.dp,
        ) {
            // TODO: remove magic number
            genreOptions.take(30).forEach { genre ->
                val selected = selectedGenres.contains(genre)
                Chip(selected = selected, text = genre, onClick = {
                    if (selected) onDeselectGenre(genre) else onSelectGenre(genre)
                })
            }
        }

        Button(onReadyClick, modifier = Modifier.padding(bottom = 16.dp)) {
            Text(stringResource(R.string.ready_button))
        }
    }
}

@ExperimentalAnimationApi
@Preview(device = Devices.PIXEL_4_XL)
@Composable
private fun AcousticnessQueryScreen() {
    LibzyContent {
        QueryScreen(
            uiState = with(QueryUiState()) {
                copy(currentStepIndex = querySteps.indexOf(QueryStep.ACOUSTICNESS))
            },
            onBackClick = {},
            onNoPreferenceClick = {},
            onCurrentFavoriteClick = {},
            onReliableClassicClick = {},
            onUnderappreciatedGemClick = {},
            onInstrumentalClick = {},
            onVocalClick = {},
            onContinueClick = {},
            onSelectGenre = {},
            onDeselectGenre = {},
            onReadyClick = {}
        )
    }
}

@ExperimentalAnimationApi
@Preview(device = Devices.PIXEL_4_XL)
@Composable
private fun GenresQueryScreen() {
    LibzyContent {
        QueryScreen(
            uiState = with(QueryUiState()) {
                copy(currentStepIndex = querySteps.indexOf(QueryStep.GENRES))
            },
            onBackClick = {},
            onNoPreferenceClick = {},
            onCurrentFavoriteClick = {},
            onReliableClassicClick = {},
            onUnderappreciatedGemClick = {},
            onInstrumentalClick = {},
            onVocalClick = {},
            onContinueClick = {},
            onSelectGenre = {},
            onDeselectGenre = {},
            onReadyClick = {}
        )
    }
}
