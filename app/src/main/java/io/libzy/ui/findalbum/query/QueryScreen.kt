package io.libzy.ui.findalbum.query

import androidx.activity.compose.BackHandler
import androidx.annotation.StringRes
import androidx.compose.animation.AnimatedContentScope.SlideDirection
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.with
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Slider
import androidx.compose.material.SliderDefaults
import androidx.compose.material.SliderDefaults.InactiveTrackAlpha
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.material.TextField
import androidx.compose.material.TextFieldDefaults
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.MicExternalOn
import androidx.compose.material.icons.rounded.Piano
import androidx.compose.material.icons.rounded.RestartAlt
import androidx.compose.material.icons.rounded.SavedSearch
import androidx.compose.material.icons.rounded.Search
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.constraintlayout.compose.ConstraintLayout
import androidx.constraintlayout.compose.Dimension
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.google.accompanist.flowlayout.FlowRow
import com.google.accompanist.insets.LocalWindowInsets
import io.libzy.R
import io.libzy.domain.Query
import io.libzy.ui.Destination
import io.libzy.ui.LibzyContent
import io.libzy.ui.common.component.BackIcon
import io.libzy.ui.common.component.Chip
import io.libzy.ui.common.component.EventHandler
import io.libzy.ui.common.component.LibzyButton
import io.libzy.ui.common.component.LibzyIcon
import io.libzy.ui.common.component.LibzyScaffold
import io.libzy.ui.common.component.SelectableButton
import io.libzy.ui.common.util.AnimatedContent
import io.libzy.ui.common.util.StatefulAnimatedVisibility
import io.libzy.ui.common.util.restartFindAlbumFlow
import io.libzy.ui.findalbum.FindAlbumFlowViewModel
import io.libzy.ui.theme.LibzyDimens.HORIZONTAL_INSET
import io.libzy.ui.theme.LibzyIconTheme
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.filter
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

    // Initialize the current step in the flow on initial composition.
    // Can't do this using LaunchedEffect(Unit) because then a composition can render
    // before the state becomes initialized, leading to unintended effects such as the keyboard
    // showing if a genre search was previously in progress and not reset before the first render.
    var initializedCurrentStep by remember { mutableStateOf(false) }
    if (!initializedCurrentStep) {
        viewModel.initCurrentStep()
        initializedCurrentStep = true
        Timber.d("Logging this value to suppress 'unused' warning: $initializedCurrentStep")
    }

    val focusManager = LocalFocusManager.current
    val keyboard = LocalWindowInsets.current.ime

    QueryScreen(
        uiState = uiState,
        onBackClick = {
            if (keyboard.isVisible) focusManager.clearFocus()
            if (uiState.currentStep is QueryStep.Genres.Search) {
                viewModel.stopGenreSearch()
            } else {
                viewModel.goToPreviousStep()
            }
        },
        onStartOverClick = {
            navController.restartFindAlbumFlow()
            findAlbumFlowViewModel.sendClickStartOverAnalyticsEvent()
        },
        onContinueClick = viewModel::goToNextStep,
        onNoPreferenceClick = {
            when (uiState.currentStep) {
                is QueryStep.Familiarity -> viewModel.setFamiliarity(null)
                is QueryStep.Instrumentalness -> viewModel.setInstrumental(null)
                is QueryStep.Acousticness -> viewModel.setAcousticness(null)
                is QueryStep.Valence -> viewModel.setValence(null)
                is QueryStep.Energy -> viewModel.setEnergy(null)
                is QueryStep.Danceability -> viewModel.setDanceability(null)
                is QueryStep.Genres -> viewModel.setGenres(null)
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
        onEnergyChange = { viewModel.setEnergy(it) },
        onDanceabilityChange = { viewModel.setDanceability(it) },
        onSelectGenre = viewModel::addGenre,
        onDeselectGenre = viewModel::removeGenre,
        onSearchGenresClick = viewModel::startGenreSearch,
        onGenreSearchQueryChange = viewModel::searchGenres,
        onDismissKeyboard = viewModel::sendDismissKeyboardAnalyticsEvent
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
    onStartOverClick: () -> Unit,
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
    onDeselectGenre: (String) -> Unit,
    onSearchGenresClick: () -> Unit,
    onGenreSearchQueryChange: (String) -> Unit,
    onDismissKeyboard: () -> Unit
) {
    BackHandler(enabled = uiState.pastFirstStep, onBack = onBackClick)

    LibzyScaffold(
        navigationIcon = {
            AnimatedVisibility(visible = uiState.pastFirstStep, enter = fadeIn(), exit = fadeOut()) {
                BackIcon(onBackClick, enabled = uiState.pastFirstStep)
            }
        },
        actionIcons = {
            AnimatedVisibility(visible = uiState.startOverButtonVisible, enter = fadeIn(), exit = fadeOut()) {
                IconButton(onStartOverClick) {
                    LibzyIcon(LibzyIconTheme.RestartAlt,  contentDescription = stringResource(R.string.start_over))
                }
            }
        },
        title = {
            StatefulAnimatedVisibility(
                visible = uiState.currentStep is QueryStep.Genres.Search,
                stateToRemember = (uiState.currentStep as? QueryStep.Genres.Search)?.searchQuery ?: "",
                enter = fadeIn(
                    animationSpec = tween(
                        delayMillis = SEARCH_TRANSITION_PT_1_DURATION_MILLIS,
                        durationMillis = SEARCH_TRANSITION_PT_2_DURATION_MILLIS,
                        easing = LinearOutSlowInEasing
                    )
                ) + slideInVertically(
                    initialOffsetY = { fullHeight -> fullHeight },
                    animationSpec = tween(
                        delayMillis = SEARCH_TRANSITION_PT_1_DURATION_MILLIS,
                        durationMillis = SEARCH_TRANSITION_PT_2_DURATION_MILLIS,
                        easing = LinearOutSlowInEasing
                    )
                ),
                exit = fadeOut()
            ) { searchQuery ->
                GenreSearchBar(
                    searchQuery = searchQuery,
                    enableTrailingIcon = !uiState.startOverButtonVisible,
                    onSearchQueryChange = onGenreSearchQueryChange,
                    onDismissKeyboard = onDismissKeyboard
                )
            }
        }
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxSize()) {
            QueryScreenHeaders(visible = uiState.currentStep !is QueryStep.Genres.Search)

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
                onSearchGenresClick,
                modifier = Modifier.weight(1f)
            )

            LibzyButton(uiState.continueButtonText, Modifier.padding(bottom = 16.dp), onContinueClick, uiState.continueButtonEnabled)

            TextButton(onNoPreferenceClick,  Modifier.padding(bottom = 16.dp).padding(horizontal = HORIZONTAL_INSET.dp)) {
                Text(stringResource(R.string.no_preference).uppercase())
            }
        }
    }
}

@ExperimentalAnimationApi
@Composable
private fun GenreSearchBar(
    searchQuery: String,
    enableTrailingIcon: Boolean,
    onSearchQueryChange: (String) -> Unit,
    onDismissKeyboard: () -> Unit,
) {
    val focusRequester = remember { FocusRequester() }
    val focusManager = LocalFocusManager.current
    val keyboard = LocalWindowInsets.current.ime
    val textStyle = MaterialTheme.typography.subtitle1.copy(textAlign = TextAlign.Start)

    LaunchedEffect(Unit) {
        focusRequester.requestFocus() // TextField should be focused when search begins
    }

    LaunchedEffect(keyboard, focusManager) {
        var keyboardPreviouslyVisible = false
        snapshotFlow { keyboard.isVisible }.collect { keyboardNowVisible ->
            if (keyboardPreviouslyVisible && !keyboardNowVisible) {
                focusManager.clearFocus() // TextField should be unfocused when keyboard is dismissed
                onDismissKeyboard()
            }
            keyboardPreviouslyVisible = keyboardNowVisible
        }
    }

    TextField(
        value = searchQuery,
        onValueChange = { onSearchQueryChange(it.take(GENRE_SEARCH_CHARACTER_LIMIT)) },
        placeholder = { Text(stringResource(R.string.search_genres), style = textStyle) },
        trailingIcon = {
            if (enableTrailingIcon) {
                AnimatedVisibility(
                    visible = searchQuery.isNotEmpty(),
                    modifier = Modifier.padding(start = 30.dp),
                    enter = fadeIn(),
                    exit = fadeOut()
                ) {
                    IconButton(onClick = {
                        onSearchQueryChange("")
                        focusRequester.requestFocus()
                    }) {
                        LibzyIcon(LibzyIconTheme.Close, stringResource(R.string.cd_clear_search_query))
                    }
                }
            }
        },
        singleLine = true,
        textStyle = textStyle,
        colors = TextFieldDefaults.textFieldColors(
            textColor = Color.White,
            backgroundColor = Color.Transparent,
            focusedIndicatorColor = Color.Transparent,
            unfocusedIndicatorColor = Color.Transparent,
            placeholderColor = Color.Gray
        ),
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
        keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
        modifier = Modifier.fillMaxWidth().focusRequester(focusRequester)
    )
}

@ExperimentalAnimationApi
@Composable
private fun QueryScreenHeaders(visible: Boolean) {

    val greetingResId = when (LocalTime.now().hour) {
        in 4..11 -> R.string.morning_greeting_text
        in 12..16 -> R.string.afternoon_greeting_text
        else -> R.string.evening_greeting_text
    }

    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(),
        exit = fadeOut(
            animationSpec = tween(
                durationMillis = SEARCH_TRANSITION_DURATION_MILLIS,
                easing = FastOutLinearInEasing
            )
        ) + shrinkVertically(
            animationSpec = tween(
                durationMillis = SEARCH_TRANSITION_PT_1_DURATION_MILLIS,
                easing = LinearEasing
            )
        ),
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                stringResource(greetingResId),
                style = MaterialTheme.typography.h3,
                modifier = Modifier.padding(horizontal = HORIZONTAL_INSET.dp)
            )
            Text(
                text = stringResource(R.string.query_instructions_text),
                style = MaterialTheme.typography.h6,
                modifier = Modifier.padding(top = 24.dp, start = HORIZONTAL_INSET.dp, end = HORIZONTAL_INSET.dp)
            )
        }
    }
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
    onSearchGenresClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(modifier) {
        AnimatedContent(
            targetState = uiState.currentStep,
            key = uiState.currentStep.type,
            transitionSpec = {
                val slideDirection = if (uiState.navigatingForward) SlideDirection.Left else SlideDirection.Right
                slideIntoContainer(slideDirection) with slideOutOfContainer(slideDirection)
            }
        ) { currentStep ->
            Box(
                Modifier
                    .fillMaxSize()
                    .padding(horizontal = HORIZONTAL_INSET.dp), contentAlignment = Alignment.Center) {
                when (currentStep) {
                    is QueryStep.Familiarity -> FamiliarityStep(
                        uiState.query.familiarity,
                        onCurrentFavoriteClick,
                        onReliableClassicClick,
                        onUnderappreciatedGemClick
                    )
                    is QueryStep.Instrumentalness -> InstrumentalnessStep(
                        uiState.query.instrumental,
                        onInstrumentalClick,
                        onVocalClick
                    )
                    is QueryStep.Acousticness -> SliderQueryStep(
                        initialValue = uiState.query.acousticness?.let { 1 - it }, // higher acousticness = lower slider value
                        leftLabelResId = R.string.acoustic,
                        rightLabelResId = R.string.electric_electronic,
                        onValueChange = onAcousticnessChange
                    )
                    is QueryStep.Valence -> SliderQueryStep(
                        initialValue = uiState.query.valence,
                        leftLabelResId = R.string.negative_emotion,
                        rightLabelResId = R.string.positive_emotion,
                        onValueChange = onValenceChange
                    )
                    is QueryStep.Energy -> SliderQueryStep(
                        initialValue = uiState.query.energy,
                        leftLabelResId = R.string.chill,
                        rightLabelResId = R.string.energetic,
                        onValueChange = onEnergyChange
                    )
                    is QueryStep.Danceability -> SliderQueryStep(
                        initialValue = uiState.query.danceability,
                        leftLabelResId = R.string.arrhythmic,
                        rightLabelResId = R.string.danceable,
                        onValueChange = onDanceabilityChange
                    )
                    is QueryStep.Genres -> GenresStep(
                        genresStep = currentStep,
                        selectedGenres = uiState.query.genres.orEmpty(),
                        onSelectGenre = onSelectGenre,
                        onDeselectGenre = onDeselectGenre,
                        onSearchGenresClick = onSearchGenresClick
                    )
                }
            }
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
            image = LibzyIconTheme.FavoriteBorder,
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

@ExperimentalAnimationApi
@Composable
private fun GenresStep(
    genresStep: QueryStep.Genres,
    selectedGenres: Set<String>,
    onSelectGenre: (String) -> Unit,
    onDeselectGenre: (String) -> Unit,
    onSearchGenresClick: () -> Unit
) {
    val focusManager = LocalFocusManager.current
    val keyboard = LocalWindowInsets.current.ime
    val scrollState = rememberScrollState()
    val genresToDisplay = genresStep.genreOptions.take(genresStep.numGenreOptionsToShow).let { genreOptions ->
        when (genresStep) {
            is QueryStep.Genres.Recommendations -> {
                // If we're not searching, then we should display all selected genres,
                // even if they aren't part of genreOptions, and keep them displayed if they were just deselected.
                // Sort them to preserve order across recompositions.
                val selectedAndDeselectedGenres = selectedGenres.plus(genresStep.recentlyRemovedGenres).sorted()
                genreOptions.toSet().plus(selectedAndDeselectedGenres)
            }
            else -> genreOptions
        }
    }

    LaunchedEffect(scrollState, keyboard) {
        snapshotFlow { scrollState.isScrollInProgress }.filter { it && keyboard.isVisible }.collect {
            focusManager.clearFocus() // close the keyboard if it is open when the user starts scrolling
        }
    }

    // wrapping genresStep in State so that LaunchedEffect can reference its most recent value without relaunching
    val genresStepState = rememberUpdatedState(genresStep)
    LaunchedEffect(scrollState, genresStepState) {
        var previousSearchQuery: String? = null
        snapshotFlow { (genresStepState.value as? QueryStep.Genres.Search)?.searchQuery }.collect { newSearchQuery ->
            // If the search query changes, ensure the scroll position is at the top of the new search results.
            // Likewise, if the search query changes to/from null (indicating leaving or entering the search screen),
            // ensure the scroll position is at the top of the new screen's displayed genres.
            val startingSearch = previousSearchQuery == null
            if (startingSearch) scrollState.animateScrollTo(0) else scrollState.scrollTo(0)
            previousSearchQuery = newSearchQuery
        }
    }

    Column {
        SearchGenresButton(
            visible = genresStep is QueryStep.Genres.Recommendations,
            onSearchGenresClick = onSearchGenresClick,
            modifier = Modifier.padding(top = 24.dp, bottom = 6.dp)
        )

        if (genresStep is QueryStep.Genres.Search && genresStep.genreOptions.isEmpty()) {
            Text(
                stringResource(R.string.no_genre_results, genresStep.searchQuery),
                style = MaterialTheme.typography.h6,
                modifier = Modifier.padding(horizontal = 8.dp)
            )
        } else {
            // TODO: add visual scroll bar when it is supported
            FlowRow(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = 18.dp, bottom = 24.dp)
                    .verticalScroll(scrollState)
                    .weight(1f),
                mainAxisSpacing = 10.dp,
                crossAxisSpacing = 16.dp,
            ) {
                genresToDisplay.forEach { genre ->
                    val selected = selectedGenres.contains(genre)
                    Chip(selected = selected, text = genre, onClick = {
                        if (selected) onDeselectGenre(genre) else onSelectGenre(genre)
                    })
                }
            }
        }
    }
}

@ExperimentalAnimationApi
@Composable
private fun SearchGenresButton(visible: Boolean, onSearchGenresClick: () -> Unit, modifier: Modifier = Modifier) {
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(),
        exit = fadeOut(
            animationSpec = tween(
                durationMillis = SEARCH_TRANSITION_DURATION_MILLIS,
                easing = FastOutLinearInEasing
            )
        ) + shrinkVertically(
            animationSpec = tween(
                delayMillis = SEARCH_TRANSITION_PT_1_DURATION_MILLIS,
                durationMillis = SEARCH_TRANSITION_PT_2_DURATION_MILLIS,
                easing = LinearEasing
            )
        )
    ) {
        Button(
            onClick = onSearchGenresClick,
            shape = RoundedCornerShape(18.dp),
            colors = ButtonDefaults.buttonColors(
                backgroundColor = MaterialTheme.colors.secondaryVariant,
                contentColor = Color.Gray
            ),
            modifier = modifier.fillMaxWidth()
        ) {
            Icon(
                imageVector = LibzyIconTheme.Search,
                contentDescription = null, // "Search genres" text suffices as CD
                modifier = Modifier.padding(end = 10.dp)
            )
            Text(stringResource(R.string.search_genres))
            Spacer(modifier = Modifier.weight(1f))
        }
    }
}

@ExperimentalAnimationApi
@Preview(device = Devices.PIXEL_4_XL)
@Composable
private fun AcousticnessQueryScreen() {
    LibzyContent {
        QueryScreen(
            uiState = QueryUiState(stepOrder = QueryUiState.DEFAULT_STEP_ORDER, currentStep = QueryStep.Acousticness),
            onBackClick = {},
            onStartOverClick = {},
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
            onDeselectGenre = {},
            onSearchGenresClick = {},
            onGenreSearchQueryChange = {},
            onDismissKeyboard = {}
        )
    }
}

@ExperimentalAnimationApi
@Preview(device = Devices.PIXEL_4_XL)
@Composable
private fun GenresQueryScreen() {
    LibzyContent {
        QueryScreen(
            uiState = QueryUiState(
                stepOrder = QueryUiState.DEFAULT_STEP_ORDER,
                currentStep = QueryStep.Genres.Recommendations(
                    genreOptions = List(60) { index ->
                        when (index) {
                            0 -> "rock"
                            1 -> "pop"
                            2 -> "hip hop"
                            3 -> "electronica"
                            4 -> "funk"
                            5 -> "heavy metal"
                            6 -> "progressive rock"
                            7 -> "indie pop"
                            8 -> "neo soul"
                            9 -> "trap"
                            10 -> "r&b"
                            else -> "genre $index"
                        }
                    }
                )
            ),
            onBackClick = {},
            onStartOverClick = {},
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
            onDeselectGenre = {},
            onSearchGenresClick = {},
            onGenreSearchQueryChange = {},
            onDismissKeyboard = {}
        )
    }
}

private const val GENRE_SEARCH_CHARACTER_LIMIT = 40

// When starting a genre search, the animation consists of two parts:
//    1. shrinking out the "greeting text", which brings the search button up to just below where the search bar appears
//    2. shrinking out the search button and making the search bar slide up from where the search button was
//
// These two parts must share the same timing, even though they involve components
// across different levels of the compose tree, so top-level constants are used.
private const val SEARCH_TRANSITION_PT_1_DURATION_MILLIS = 100
private const val SEARCH_TRANSITION_PT_2_DURATION_MILLIS = 50
private const val SEARCH_TRANSITION_DURATION_MILLIS =
    SEARCH_TRANSITION_PT_1_DURATION_MILLIS + SEARCH_TRANSITION_PT_2_DURATION_MILLIS
