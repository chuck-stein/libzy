package io.libzy.ui.findalbum.query

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
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Slider
import androidx.compose.material.SliderDefaults
import androidx.compose.material.SliderDefaults.InactiveTrackAlpha
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.MicExternalOn
import androidx.compose.material.icons.rounded.Piano
import androidx.compose.material.icons.rounded.SavedSearch
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
import io.libzy.ui.Destination
import io.libzy.ui.LibzyContent
import io.libzy.ui.common.component.BackIcon
import io.libzy.ui.common.component.Chip
import io.libzy.ui.common.component.EventHandler
import io.libzy.ui.common.component.LibzyButton
import io.libzy.ui.common.component.LibzyScaffold
import io.libzy.ui.common.component.SelectableButton
import io.libzy.ui.findalbum.FindAlbumFlowViewModel
import io.libzy.ui.theme.LibzyDimens.HORIZONTAL_INSET
import io.libzy.ui.theme.LibzyIconTheme
import java.time.LocalTime

/**
 * **Stateful** Query Screen, displaying a series of questions about what the user is in the mood to listen to.
 */
@ExperimentalAnimationApi
@Composable
fun QueryScreen(navController: NavController, viewModelFactory: ViewModelProvider.Factory) {
    val viewModel: QueryViewModel = viewModel(factory = viewModelFactory)
    val uiState by viewModel.uiState

    val findAlbumFlowViewModel: FindAlbumFlowViewModel = viewModel(
        viewModelStoreOwner = navController.getBackStackEntry(Destination.FindAlbumFlow.route),
        factory = viewModelFactory
    )

    LaunchedEffect(findAlbumFlowViewModel, uiState.query) {
        findAlbumFlowViewModel.setQuery(uiState.query)
    }

    EventHandler(viewModel.uiEvents) {
        if (it == QueryUiEvent.SUBMIT_QUERY) {
            findAlbumFlowViewModel.setQuery(uiState.query)
            navController.navigate(Destination.Results.route)
        }
    }

    LaunchedEffect(Unit) {
        viewModel.sendQuestionViewAnalyticsEvent()
    }

    QueryScreen(
        uiState = uiState,
        onBackClick = viewModel::goToPreviousStep,
        onContinueClick = viewModel::goToNextStep,
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
        onCurrentFavoriteClick = { viewModel.setFamiliarity(Query.Familiarity.CURRENT_FAVORITE) },
        onReliableClassicClick = { viewModel.setFamiliarity(Query.Familiarity.RELIABLE_CLASSIC) },
        onUnderappreciatedGemClick = { viewModel.setFamiliarity(Query.Familiarity.UNDERAPPRECIATED_GEM) },
        onInstrumentalClick = { viewModel.setInstrumental(true) },
        onVocalClick = { viewModel.setInstrumental(false) },
        onAcousticnessChange = { viewModel.setAcousticness(1 - it) }, // lower = more acoustic, so subtracting from 1
        onValenceChange = { viewModel.setValence(it) },
        onEnergyChange = {  viewModel.setEnergy(it) },
        onDanceabilityChange = { viewModel.setDanceability(it) },
        onSelectGenre = viewModel::addGenre,
        onDeselectGenre = viewModel::removeGenre
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
    onContinueClick: () -> Unit,
    onNoPreferenceClick: () -> Unit,
    onCurrentFavoriteClick: () -> Unit,
    onReliableClassicClick: () -> Unit,
    onUnderappreciatedGemClick: () -> Unit,
    onInstrumentalClick: () -> Unit,
    onVocalClick: () -> Unit,
    onAcousticnessChange: (Float) -> Unit,
    onValenceChange: (Float) -> Unit,
    onEnergyChange: (Float) -> Unit,
    onDanceabilityChange: (Float) -> Unit,
    onSelectGenre: (String) -> Unit,
    onDeselectGenre: (String) -> Unit
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
                onAcousticnessChange,
                onValenceChange,
                onEnergyChange,
                onDanceabilityChange,
                onSelectGenre,
                onDeselectGenre,
                modifier = Modifier.weight(1f)
            )
            val onFinalStep = uiState.currentStepIndex == uiState.querySteps.size - 1
            val continueButtonText = if (onFinalStep) R.string.ready_button else R.string.continue_button
            val continueButtonEnabled = when (uiState.currentStep) {
                QueryStep.FAMILIARITY -> uiState.query.familiarity != null
                QueryStep.INSTRUMENTALNESS -> uiState.query.instrumental != null
                QueryStep.ACOUSTICNESS -> uiState.query.acousticness != null
                QueryStep.VALENCE -> uiState.query.valence != null
                QueryStep.ENERGY -> uiState.query.energy != null
                QueryStep.DANCEABILITY -> uiState.query.danceability != null
                QueryStep.GENRES -> uiState.query.genres != null
            }
            LibzyButton(continueButtonText, Modifier.padding(bottom = 16.dp), onContinueClick, continueButtonEnabled)

            TextButton(onNoPreferenceClick, Modifier.padding(bottom = 16.dp).padding(horizontal = HORIZONTAL_INSET.dp)) {
                Text(stringResource(R.string.no_preference).uppercase())
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
    onAcousticnessChange: (Float) -> Unit,
    onValenceChange: (Float) -> Unit,
    onEnergyChange: (Float) -> Unit,
    onDanceabilityChange: (Float) -> Unit,
    onSelectGenre: (String) -> Unit,
    onDeselectGenre: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Box(modifier) {
        QueryStepAnimator(uiState, QueryStep.FAMILIARITY) {
            FamiliarityStep(
                uiState.query.familiarity,
                onCurrentFavoriteClick,
                onReliableClassicClick,
                onUnderappreciatedGemClick
            )
        }

        QueryStepAnimator(uiState, QueryStep.INSTRUMENTALNESS) {
            InstrumentalnessStep(uiState.query.instrumental, onInstrumentalClick, onVocalClick)
        }

        QueryStepAnimator(uiState, QueryStep.ACOUSTICNESS) {
            SliderQueryStep(
                initialValue = uiState.query.acousticness?.let { 1 - it }, // higher acousticness = lower slider value
                leftLabelResId = R.string.acoustic,
                rightLabelResId = R.string.electric_electronic,
                onValueChange = onAcousticnessChange
            )
        }

        QueryStepAnimator(uiState, QueryStep.VALENCE) {
            SliderQueryStep(
                initialValue = uiState.query.valence,
                leftLabelResId = R.string.negative_emotion,
                rightLabelResId = R.string.positive_emotion,
                onValueChange = onValenceChange
            )
        }

        QueryStepAnimator(uiState, QueryStep.ENERGY) {
            SliderQueryStep(
                initialValue = uiState.query.energy,
                leftLabelResId = R.string.chill,
                rightLabelResId = R.string.energetic,
                onValueChange = onEnergyChange
            )
        }

        QueryStepAnimator(uiState, QueryStep.DANCEABILITY) {
            SliderQueryStep(
                initialValue = uiState.query.danceability,
                leftLabelResId = R.string.arrhythmic,
                rightLabelResId = R.string.danceable,
                onValueChange = onDanceabilityChange
            )
        }

        QueryStepAnimator(uiState, QueryStep.GENRES) {
            GenresStep(
                genreOptions = uiState.recommendedGenres,
                selectedGenres = uiState.query.genres.orEmpty(),
                onSelectGenre = onSelectGenre,
                onDeselectGenre = onDeselectGenre
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
    Spacer(modifier = Modifier.height(16.dp))
}

@Composable
private fun FamiliarityStep(
    selectedFamiliarity: Query.Familiarity?,
    onCurrentFavoriteClick: () -> Unit,
    onReliableClassicClick: () -> Unit,
    onUnderappreciatedGemClick: () -> Unit
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        SelectableButton(
            textResId = R.string.current_favorite,
            image = LibzyIconTheme.History,
            selected = selectedFamiliarity == Query.Familiarity.CURRENT_FAVORITE,
            onClick = onCurrentFavoriteClick
        )

        ButtonGroupSpacer()

        SelectableButton(
            textResId = R.string.reliable_classic,
            image = LibzyIconTheme.Favorite,
            selected = selectedFamiliarity == Query.Familiarity.RELIABLE_CLASSIC,
            onClick = onReliableClassicClick
        )

        ButtonGroupSpacer()

        SelectableButton(
            textResId = R.string.underappreciated_gem,
            image = LibzyIconTheme.SavedSearch,
            selected = selectedFamiliarity == Query.Familiarity.UNDERAPPRECIATED_GEM,
            onClick = onUnderappreciatedGemClick
        )
    }
}

@Composable
private fun InstrumentalnessStep(
    selectedInstrumentalness: Boolean?,
    onInstrumentalClick: () -> Unit,
    onVocalClick: () -> Unit
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        SelectableButton(
            textResId = R.string.instrumental,
            image = LibzyIconTheme.Piano,
            selected = selectedInstrumentalness == true,
            onClick = onInstrumentalClick
        )

        ButtonGroupSpacer()

        SelectableButton(
            textResId = R.string.vocal,
            image = LibzyIconTheme.MicExternalOn,
            selected = selectedInstrumentalness == false,
            onClick = onVocalClick
        )
    }
}

@Composable
private fun SliderQueryStep(
    initialValue: Float?,
    @StringRes leftLabelResId: Int,
    @StringRes rightLabelResId: Int,
    onValueChange: (Float) -> Unit
) {
    var currentValue by remember { mutableStateOf(initialValue) }
    val sliderColor = if (currentValue != null) MaterialTheme.colors.primary else MaterialTheme.colors.secondary
    val horizontalSpacing = 8.dp
    
    ConstraintLayout {
        val (leftLabel, rightLabel, slider) = createRefs()

        Slider(
            value = currentValue ?: 0.5f,
            onValueChange = { currentValue = it },
            onValueChangeFinished = { currentValue?.let { onValueChange(it) } },
            colors = SliderDefaults.colors(
                thumbColor = sliderColor,
                activeTrackColor = sliderColor,
                inactiveTrackColor = sliderColor.copy(alpha = InactiveTrackAlpha),
            ),
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
    }
}

@Composable
private fun GenresStep(
    genreOptions: List<String>,
    selectedGenres: Set<String>,
    onSelectGenre: (String) -> Unit,
    onDeselectGenre: (String) -> Unit
) {
    // TODO: add visual scroll bar when it is supported
    FlowRow(
        modifier = Modifier.fillMaxSize().padding(vertical = 24.dp).verticalScroll(rememberScrollState()),
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
            onContinueClick = {},
            onNoPreferenceClick = {},
            onCurrentFavoriteClick = {},
            onReliableClassicClick = {},
            onUnderappreciatedGemClick = {},
            onInstrumentalClick = {},
            onVocalClick = {},
            onAcousticnessChange = {},
            onValenceChange = {},
            onEnergyChange = {},
            onDanceabilityChange = {},
            onSelectGenre = {},
            onDeselectGenre = {}
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
            onContinueClick = {},
            onNoPreferenceClick = {},
            onCurrentFavoriteClick = {},
            onReliableClassicClick = {},
            onUnderappreciatedGemClick = {},
            onInstrumentalClick = {},
            onVocalClick = {},
            onAcousticnessChange = {},
            onValenceChange = {},
            onEnergyChange = {},
            onDanceabilityChange = {},
            onSelectGenre = {},
            onDeselectGenre = {}
        )
    }
}
