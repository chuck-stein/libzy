package io.libzy.ui.findalbum.query

import androidx.activity.compose.BackHandler
import androidx.annotation.StringRes
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.isImeVisible
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.material.icons.rounded.Diamond
import androidx.compose.material.icons.rounded.MicExternalOn
import androidx.compose.material.icons.rounded.Piano
import androidx.compose.material.icons.rounded.Repeat
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.StarOutline
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.constraintlayout.compose.ConstraintLayout
import androidx.constraintlayout.compose.Dimension
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavController
import com.google.accompanist.flowlayout.FlowRow
import io.libzy.R
import io.libzy.domain.Query
import io.libzy.domain.Query.Familiarity.CURRENT_FAVORITE
import io.libzy.domain.Query.Familiarity.RELIABLE_CLASSIC
import io.libzy.domain.Query.Familiarity.UNDERAPPRECIATED_GEM
import io.libzy.domain.Query.Parameter.ACOUSTICNESS
import io.libzy.domain.Query.Parameter.DANCEABILITY
import io.libzy.domain.Query.Parameter.ENERGY
import io.libzy.domain.Query.Parameter.FAMILIARITY
import io.libzy.domain.Query.Parameter.GENRES
import io.libzy.domain.Query.Parameter.INSTRUMENTALNESS
import io.libzy.domain.Query.Parameter.VALENCE
import io.libzy.ui.Destination
import io.libzy.ui.common.component.BackIcon
import io.libzy.ui.common.component.Chip
import io.libzy.ui.common.component.EventHandler
import io.libzy.ui.common.component.LibzyButton
import io.libzy.ui.common.component.LibzyIcon
import io.libzy.ui.common.component.LibzyScaffold
import io.libzy.ui.common.component.LoadedContent
import io.libzy.ui.common.component.SelectableButton
import io.libzy.ui.common.component.StartOverIconButton
import io.libzy.ui.common.util.StatefulAnimatedVisibility
import io.libzy.ui.common.util.goToNextPage
import io.libzy.ui.common.util.goToPreviousPage
import io.libzy.ui.common.util.restartFindAlbumFlow
import io.libzy.ui.findalbum.FindAlbumFlowViewModel
import io.libzy.ui.findalbum.query.QueryUiEvent.AddGenre
import io.libzy.ui.findalbum.query.QueryUiEvent.ChangeAcousticness
import io.libzy.ui.findalbum.query.QueryUiEvent.ChangeDanceability
import io.libzy.ui.findalbum.query.QueryUiEvent.ChangeEnergy
import io.libzy.ui.findalbum.query.QueryUiEvent.ChangeValence
import io.libzy.ui.findalbum.query.QueryUiEvent.OpenSettings
import io.libzy.ui.findalbum.query.QueryUiEvent.RemoveGenre
import io.libzy.ui.findalbum.query.QueryUiEvent.SelectFamiliarity
import io.libzy.ui.findalbum.query.QueryUiEvent.SelectInstrumentalness
import io.libzy.ui.findalbum.query.QueryUiEvent.SelectNoPreference
import io.libzy.ui.findalbum.query.QueryUiEvent.SendDismissKeyboardAnalytics
import io.libzy.ui.findalbum.query.QueryUiEvent.SendQuestionViewAnalytics
import io.libzy.ui.findalbum.query.QueryUiEvent.SendSubmitQueryAnalytics
import io.libzy.ui.findalbum.query.QueryUiEvent.StartOver
import io.libzy.ui.findalbum.query.QueryUiEvent.StartSearchingGenres
import io.libzy.ui.findalbum.query.QueryUiEvent.StopSearchingGenres
import io.libzy.ui.findalbum.query.QueryUiEvent.SubmitQuery
import io.libzy.ui.findalbum.query.QueryUiEvent.UpdateSearchQuery
import io.libzy.ui.theme.LibzyDimens.HORIZONTAL_INSET
import io.libzy.ui.theme.LibzyIconTheme
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.launch
import java.time.LocalTime

/**
 * **Stateful** Query Screen, displaying a series of questions about what the user is in the mood to listen to.
 */
@Composable
fun QueryScreen(
    navController: NavController,
    viewModelFactory: ViewModelProvider.Factory,
    backStackEntry: NavBackStackEntry
) {
    val viewModel: QueryViewModel = viewModel(factory = viewModelFactory)
    val uiState by viewModel.uiStateFlow.collectAsState()

    val findAlbumFlowViewModel: FindAlbumFlowViewModel = viewModel(
        viewModelStoreOwner = remember(backStackEntry) {
            navController.getBackStackEntry(Destination.FindAlbumFlow.route)
        },
        factory = viewModelFactory
    )

    LaunchedEffect(findAlbumFlowViewModel, uiState.query) {
        findAlbumFlowViewModel.setQuery(uiState.query)
    }

    val submitQuery = {
        findAlbumFlowViewModel.setQuery(uiState.query)
        navController.navigate(Destination.Results.route)
        viewModel.processEvent(SendSubmitQueryAnalytics)
        // stop searching genres so that when we return we are not back on the search screen,
        // but have a small delay first so that we don't see the screen flash to a different state before navigating
        viewModel.processEvent(StopSearchingGenres(sendAnalytics = false, delayFirst = true))
    }

    EventHandler(viewModel.uiEvents) {
        if (it == SubmitQuery) submitQuery()
    }

    QueryScreen(
        uiState = uiState,
        onUiEvent = {
            when (it) {
                is OpenSettings -> navController.navigate(Destination.Settings.route)
                is StartOver -> {
                    navController.restartFindAlbumFlow()
                    findAlbumFlowViewModel.sendClickStartOverAnalyticsEvent()
                }
                is SubmitQuery -> submitQuery()
                is QueryUiEvent.ForViewModel -> viewModel.processEvent(it)
            }
        }
    )
}

/**
 * **Stateless** Query Screen, displaying a series of questions about what the user is in the mood to listen to.
 */
@OptIn(ExperimentalLayoutApi::class, ExperimentalFoundationApi::class)
@Composable
private fun QueryScreen(uiState: QueryUiState, onUiEvent: (QueryUiEvent) -> Unit) {
    val queryStepPagerState = rememberPagerState { uiState.stepOrder.size }
    val currentQueryStep = uiState.stepOrder[queryStepPagerState.currentPage]
    val onFirstQueryStep = queryStepPagerState.currentPage == 0
    val onFinalQueryStep = queryStepPagerState.currentPage == uiState.stepOrder.lastIndex
    val showBackButton = !onFirstQueryStep || uiState.searchingGenres

    val focusManager = LocalFocusManager.current
    val genreSearchBarFocusRequester = remember { FocusRequester() }
    val keyboardVisible = WindowInsets.isImeVisible
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(queryStepPagerState.currentPage) {
        onUiEvent(SendQuestionViewAnalytics(currentQueryStep))
        onUiEvent(StopSearchingGenres(sendAnalytics = false))
    }

    val onBackClick: () -> Unit = {
        if (keyboardVisible) {
            focusManager.clearFocus()
        }
        if (uiState.searchingGenres) {
            onUiEvent(StopSearchingGenres(sendAnalytics = true))
        } else {
            coroutineScope.launch {
                queryStepPagerState.goToPreviousPage()
            }
        }
    }

    LoadedContent(uiState.loading) {
        BackHandler(enabled = showBackButton, onBack = onBackClick)

        LibzyScaffold(
            navigationIcon = {
                AnimatedVisibility(visible = showBackButton, enter = fadeIn(), exit = fadeOut()) {
                    BackIcon(onBackClick, enabled = showBackButton)
                }
            },
            actionIcons = {
                QueryScreenActionIcons(onFirstQueryStep, genreSearchBarFocusRequester, uiState, onUiEvent)
            },
            title = {
                QueryScreenTitleBar(genreSearchBarFocusRequester, uiState, onUiEvent)
            }
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxSize()) {
                QueryScreenHeaders(visible = !uiState.searchingGenres)
                CurrentQueryStep(uiState, queryStepPagerState, onUiEvent, Modifier.weight(1f))
                ContinueButton(uiState, queryStepPagerState, currentQueryStep, onFinalQueryStep, onUiEvent)
                NoPreferenceButton(onUiEvent, currentQueryStep, queryStepPagerState)
                PagingIndicator(queryStepPagerState)
            }
        }
    }
}

@Composable
private fun QueryScreenActionIcons(
    onFirstQueryStep: Boolean,
    genreSearchBarFocusRequester: FocusRequester,
    uiState: QueryUiState,
    onUiEvent: (QueryUiEvent) -> Unit
) {
    val actionIcon = when {
        uiState.searchingGenres && uiState.genreSearchQuery.isNullOrBlank() -> null
        uiState.searchingGenres -> QueryScreenActionIcon.ClearSearchQuery
        onFirstQueryStep -> QueryScreenActionIcon.Settings
        else -> QueryScreenActionIcon.StartOver
    }

    val iconRotation = remember { Animatable(0f) }
    val actionIconState = rememberUpdatedState(actionIcon)
    LaunchedEffect(iconRotation, actionIconState) {
        var previousActionIcon = actionIconState.value
        snapshotFlow { actionIconState.value }.collect { newActionIcon ->
            if (newActionIcon == QueryScreenActionIcon.Settings || previousActionIcon == QueryScreenActionIcon.Settings) {
                iconRotation.animateTo(iconRotation.targetValue - 360f, animationSpec = tween())
            }
            previousActionIcon = newActionIcon
        }
    }

    Crossfade(
        targetState = actionIcon,
        modifier = Modifier.rotate(iconRotation.value)
    ) { currentActionIcon ->
        when (currentActionIcon) {
            QueryScreenActionIcon.Settings -> IconButton(onClick = { onUiEvent(OpenSettings) }) {
                LibzyIcon(LibzyIconTheme.Settings, contentDescription = stringResource(R.string.settings))
            }
            QueryScreenActionIcon.StartOver -> StartOverIconButton { onUiEvent(StartOver) }
            QueryScreenActionIcon.ClearSearchQuery -> IconButton(
                onClick = {
                    onUiEvent(UpdateSearchQuery(("")))
                    genreSearchBarFocusRequester.requestFocus()
                }
            ) {
                LibzyIcon(LibzyIconTheme.Close, stringResource(R.string.cd_clear_search_query))
            }
            null -> {}
        }
    }
}

@Composable
private fun QueryScreenTitleBar(
    genreSearchBarFocusRequester: FocusRequester,
    uiState: QueryUiState,
    onUiEvent: (QueryUiEvent) -> Unit
) {
    StatefulAnimatedVisibility(
        visible = uiState.searchingGenres,
        stateToRemember = uiState.genreSearchQuery ?: "",
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
        GenreSearchBar(searchQuery, genreSearchBarFocusRequester, onUiEvent)
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun GenreSearchBar(searchQuery: String, focusRequester: FocusRequester, onUiEvent: (QueryUiEvent) -> Unit) {
    val focusManager = LocalFocusManager.current
    var keyboardPreviouslyVisible by remember { mutableStateOf(false) }
    val keyboardVisible = WindowInsets.isImeVisible
    val textStyle = MaterialTheme.typography.subtitle1.copy(textAlign = TextAlign.Start)

    LaunchedEffect(Unit) {
        focusRequester.requestFocus() // TextField should be focused when search begins
    }

    LaunchedEffect(keyboardVisible, focusManager) {
        if (keyboardPreviouslyVisible && !keyboardVisible) {
            focusManager.clearFocus() // TextField should be unfocused when keyboard is dismissed
            onUiEvent(SendDismissKeyboardAnalytics)
        }
        keyboardPreviouslyVisible = keyboardVisible
    }

    TextField(
        value = searchQuery,
        onValueChange = { onUiEvent(UpdateSearchQuery(it.take(GENRE_SEARCH_CHARACTER_LIMIT))) },
        placeholder = { Text(stringResource(R.string.search_genres), style = textStyle) },
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

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ContinueButton(
    uiState: QueryUiState,
    queryStepPagerState: PagerState,
    currentQueryStep: Query.Parameter,
    onFinalQueryStep: Boolean,
    onUiEvent: (QueryUiEvent) -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    LibzyButton(
        textResId = if (onFinalQueryStep) R.string.ready_button else R.string.continue_button,
        modifier = Modifier.padding(bottom = 16.dp),
        enabled = when (currentQueryStep) {
            FAMILIARITY -> uiState.query.familiarity != null
            INSTRUMENTALNESS -> uiState.query.instrumental != null
            ACOUSTICNESS -> uiState.query.acousticness != null
            VALENCE -> uiState.query.valence != null
            ENERGY -> uiState.query.energy != null
            DANCEABILITY -> uiState.query.danceability != null
            GENRES -> uiState.query.genres != null
        }
    ) {
        if (onFinalQueryStep) {
            onUiEvent(SubmitQuery)
        } else {
            coroutineScope.launch {
                queryStepPagerState.goToNextPage()
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun NoPreferenceButton(
    onUiEvent: (QueryUiEvent) -> Unit,
    currentQueryStep: Query.Parameter,
    queryStepPagerState: PagerState
) {
    val coroutineScope = rememberCoroutineScope()
    TextButton(
        onClick = {
            onUiEvent(SelectNoPreference(currentQueryStep))
            coroutineScope.launch {
                queryStepPagerState.goToNextPage()
            }
        },
        modifier = Modifier.padding(bottom = 16.dp).padding(horizontal = HORIZONTAL_INSET.dp)
    ) {
        Text(stringResource(R.string.no_preference).uppercase())
    }
}

@OptIn(ExperimentalAnimationApi::class, ExperimentalFoundationApi::class)
@Composable
private fun CurrentQueryStep(
    uiState: QueryUiState,
    pagerState: PagerState,
    onUiEvent: (QueryUiEvent) -> Unit,
    modifier: Modifier = Modifier
) {
    HorizontalPager(state = pagerState, modifier = modifier.fillMaxSize()) { currentStepIndex ->
        Box(
            modifier = Modifier.fillMaxSize().padding(horizontal = HORIZONTAL_INSET.dp),
            contentAlignment = Alignment.Center
        ) {
            when (uiState.stepOrder[currentStepIndex]) {
                FAMILIARITY -> FamiliarityStep(uiState.query.familiarity, onUiEvent)
                INSTRUMENTALNESS -> InstrumentalnessStep(uiState.query.instrumental, onUiEvent)
                ACOUSTICNESS -> SliderQueryStep(
                    initialValue = uiState.query.acousticness?.let { 1 - it }, // higher acousticness = lower slider value
                    leftLabelResId = R.string.acoustic,
                    rightLabelResId = R.string.electric_electronic,
                    onValueChange = { onUiEvent(ChangeAcousticness(1 - it)) } // higher slider value = lower acousticness
                )
                VALENCE -> SliderQueryStep(
                    initialValue = uiState.query.valence,
                    leftLabelResId = R.string.negative_emotion,
                    rightLabelResId = R.string.positive_emotion,
                    onValueChange = { onUiEvent(ChangeValence(it)) }
                )
                ENERGY -> SliderQueryStep(
                    initialValue = uiState.query.energy,
                    leftLabelResId = R.string.chill,
                    rightLabelResId = R.string.energetic,
                    onValueChange = { onUiEvent(ChangeEnergy(it)) }
                )
                DANCEABILITY -> SliderQueryStep(
                    initialValue = uiState.query.danceability,
                    leftLabelResId = R.string.arrhythmic,
                    rightLabelResId = R.string.danceable,
                    onValueChange = { onUiEvent(ChangeDanceability(it)) }
                )
                GENRES -> GenresStep(uiState, onUiEvent)
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun PagingIndicator(pagerState: PagerState) {
    if (pagerState.pageCount > 1) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            repeat(pagerState.pageCount) {
                val selected = it == pagerState.currentPage
                val color by animateColorAsState(if (selected) Color.White else Color.DarkGray)
                Box(Modifier.padding(horizontal = 4.dp).padding(bottom = 16.dp).background(color, CircleShape).size(8.dp))
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
    onUiEvent: (QueryUiEvent) -> Unit
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        SelectableButton(
            textResId = R.string.current_favorite,
            image = LibzyIconTheme.Repeat,
            selected = selectedFamiliarity == CURRENT_FAVORITE,
            onClick = { onUiEvent(SelectFamiliarity(CURRENT_FAVORITE)) }
        )

        ButtonGroupSpacer()

        SelectableButton(
            textResId = R.string.reliable_classic,
            image = LibzyIconTheme.StarOutline,
            selected = selectedFamiliarity == RELIABLE_CLASSIC,
            onClick = { onUiEvent(SelectFamiliarity(RELIABLE_CLASSIC)) }
        )

        ButtonGroupSpacer()

        SelectableButton(
            textResId = R.string.underappreciated_gem,
            image = LibzyIconTheme.Diamond,
            selected = selectedFamiliarity == UNDERAPPRECIATED_GEM,
            onClick = { onUiEvent(SelectFamiliarity(UNDERAPPRECIATED_GEM)) }
        )
    }
}

@Composable
private fun InstrumentalnessStep(
    selectedInstrumentalness: Boolean?,
    onUiEvent: (QueryUiEvent) -> Unit
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        SelectableButton(
            textResId = R.string.instrumental,
            image = LibzyIconTheme.Piano,
            selected = selectedInstrumentalness == true,
            onClick = { onUiEvent(SelectInstrumentalness(true)) }
        )

        ButtonGroupSpacer()

        SelectableButton(
            textResId = R.string.vocal,
            image = LibzyIconTheme.MicExternalOn,
            selected = selectedInstrumentalness == false,
            onClick = { onUiEvent(SelectInstrumentalness(false)) }
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

@OptIn(ExperimentalLayoutApi::class)
@ExperimentalAnimationApi
@Composable
private fun GenresStep(uiState: QueryUiState, onUiEvent: (QueryUiEvent) -> Unit) {
    val focusManager = LocalFocusManager.current
    val keyboardVisible = WindowInsets.isImeVisible
    val scrollState = rememberScrollState()

    LaunchedEffect(scrollState, keyboardVisible) {
        snapshotFlow { scrollState.isScrollInProgress }.filter { it && keyboardVisible }.collect {
            focusManager.clearFocus() // close the keyboard if it is open when the user starts scrolling
        }
    }

    val searchQuery = rememberUpdatedState(uiState.genreSearchQuery)
    LaunchedEffect(scrollState, searchQuery) {
        var previousSearchQuery: String? = null
        snapshotFlow { searchQuery.value }.collect { newSearchQuery ->
            // If the search query changes, ensure the scroll position is at the top of the new search results.
            // Likewise, if the search query changes to/from null (indicating leaving or entering the search screen),
            // ensure the scroll position is at the top of the new screen's displayed genres.
            val startingSearch = previousSearchQuery == null && newSearchQuery != null
            if (startingSearch) scrollState.animateScrollTo(0) else scrollState.scrollTo(0)
            previousSearchQuery = newSearchQuery
        }
    }

    Column {
        SearchGenresButton(
            visible = !uiState.searchingGenres,
            onSearchGenresClick = { onUiEvent(StartSearchingGenres) },
            modifier = Modifier.padding(top = 24.dp, bottom = 6.dp)
        )

        if (uiState.genreSearchState is GenreSearchState.Searching && uiState.genresToDisplay.isEmpty()) {
            Text(
                stringResource(R.string.no_genre_results, uiState.genreSearchState.searchQuery),
                style = MaterialTheme.typography.h6,
                modifier = Modifier.padding(horizontal = 8.dp)
            )
        } else {
            // TODO: add visual scroll bar when it is supported
            // TODO: use Compose 1.4 FlowRow
            FlowRow(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = 18.dp, bottom = 24.dp)
                    .verticalScroll(scrollState)
                    .weight(1f),
                mainAxisSpacing = 10.dp,
                crossAxisSpacing = 16.dp,
            ) {
                uiState.genresToDisplay.forEach { genre ->
                    val selected = uiState.selectedGenres.contains(genre)
                    Chip(selected = selected, text = genre, onClick = {
                        if (selected) onUiEvent(RemoveGenre(genre)) else onUiEvent(AddGenre(genre))
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
