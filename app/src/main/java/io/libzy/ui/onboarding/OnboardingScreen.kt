package io.libzy.ui.onboarding

import androidx.activity.compose.BackHandler
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CornerSize
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Quiz
import androidx.compose.material.icons.rounded.RestartAlt
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import io.libzy.R
import io.libzy.analytics.AnalyticsConstants.EventProperties.STEP_NUM
import io.libzy.analytics.AnalyticsConstants.Events.VIEW_ONBOARDING_STEP
import io.libzy.analytics.LocalAnalytics
import io.libzy.ui.Destination
import io.libzy.ui.LibzyContent
import io.libzy.ui.common.component.ALBUM_LIST_ITEM_PADDING
import io.libzy.ui.common.component.AlbumArtwork
import io.libzy.ui.common.component.AlbumListItem
import io.libzy.ui.common.component.AlbumUiState
import io.libzy.ui.common.component.LibzyButton
import io.libzy.ui.common.component.LibzyIcon
import io.libzy.ui.common.component.LibzyScaffold
import io.libzy.ui.common.component.PagingIndicator
import io.libzy.ui.common.util.fadingMarquee
import io.libzy.ui.common.util.goToNextPage
import io.libzy.ui.common.util.surfaceBackground
import io.libzy.ui.onboarding.OnboardingUiEvent.CompleteOnboarding
import io.libzy.ui.theme.LibzyColors
import io.libzy.ui.theme.LibzyIconTheme
import io.libzy.util.toTextResource
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.sin
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

@Composable
fun OnboardingScreen(navController: NavController, viewModelFactory: ViewModelProvider.Factory, exitApp: () -> Unit) {
    val viewModel: OnboardingViewModel = viewModel(factory = viewModelFactory)
    val uiState by viewModel.uiStateFlow.collectAsState()

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner, viewModel) {
        lifecycleOwner.lifecycle.addObserver(viewModel)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(viewModel)
        }
    }

    BackHandler(enabled = uiState.onboardingMandatory, onBack = exitApp)

    LaunchedEffect(uiState) {
        if (uiState.onboardingCompleted) {
            navController.popBackStack(Destination.Query.route, inclusive = false)
        }
    }

    OnboardingScreen(uiState) { event ->
        viewModel.processEvent(event)
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun OnboardingScreen(
    uiState: OnboardingUiState,
    initialStepIndex: Int = 0,
    useAutotypingEffect: Boolean = true,
    onUiEvent: (OnboardingUiEvent) -> Unit = {}
) {
    val onboardingStepPager = rememberPagerState(initialStepIndex, pageCount = { 7 })
    OnboardingAnalyticsDispatcher(onboardingStepPager)

    LibzyScaffold(showTopBar = false) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Spacer(Modifier.weight(1f))
            OnboardingHeader()
            Spacer(Modifier.height(32.dp))
            OnboardingPager(onboardingStepPager, useAutotypingEffect, Modifier.weight(7f), uiState, onUiEvent)
            PagingIndicator(onboardingStepPager, Modifier.padding(bottom = 16.dp))
            Spacer(Modifier.weight(1f))
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun OnboardingAnalyticsDispatcher(onboardingStepPager: PagerState) {
    val analytics = LocalAnalytics.current
    LaunchedEffect(onboardingStepPager.currentPage) {
        analytics.sendEvent(
            eventName = VIEW_ONBOARDING_STEP,
            eventProperties = mapOf(STEP_NUM to onboardingStepPager.currentPage + 1)
        )
    }
}

@Composable
private fun OnboardingHeader() {
    Text(
        text = stringResource(R.string.onboarding_header),
        style = MaterialTheme.typography.h4,
        color = LibzyColors.Gray,
        modifier = Modifier.padding(16.dp)
    )
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun OnboardingPager(
    pagerState: PagerState,
    useAutotypingEffect: Boolean,
    modifier: Modifier = Modifier,
    uiState: OnboardingUiState,
    onUiEvent: (OnboardingUiEvent) -> Unit = {}
) {
    HorizontalPager(pagerState, modifier) { stepIndex ->
        Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            Box(
                modifier = Modifier
                    .padding(32.dp)
                    .surfaceBackground()
                    .padding(vertical = 32.dp)
                    .sizeIn(maxHeight = 600.dp, maxWidth = 500.dp)
            ) {
                val isCurrentStep = pagerState.currentPage == stepIndex
                Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.SpaceBetween) {
                    OnboardingInfo(stepIndex, isCurrentStep, useAutotypingEffect)
                    OnboardingVisual(stepIndex, isCurrentStep, uiState, onUiEvent, Modifier.padding(vertical = 16.dp))
                    OnboardingCta(pagerState, Modifier.align(Alignment.CenterHorizontally), onUiEvent)
                }
            }
        }
    }
}

@Composable
private fun OnboardingInfo(stepIndex: Int, isCurrentStep: Boolean, useAutotypingEffect: Boolean) {
    getOnboardingInfoText(stepIndex)?.let { onboardingInfoText ->

        var numDisplayedCharacters by remember {
            mutableStateOf(if (useAutotypingEffect) 0 else onboardingInfoText.length)
        }
        LaunchedEffect(isCurrentStep) {
            if (isCurrentStep) {
                while (numDisplayedCharacters < onboardingInfoText.length) {
                    delay(15.milliseconds)
                    numDisplayedCharacters++
                }
            }
        }

        Box {
            repeat(2) { index ->
                val isHiddenTextJustForLayoutSizing = index == 1
                val numChars = if (isHiddenTextJustForLayoutSizing) onboardingInfoText.length else numDisplayedCharacters
                val alpha = if (isHiddenTextJustForLayoutSizing) 0f else 1f

                Text(
                    text = onboardingInfoText.take(numChars),
                    style = MaterialTheme.typography.subtitle1,
                    textAlign = TextAlign.Start,
                    modifier = Modifier
                        .padding(horizontal = 32.dp)
                        .alpha(alpha)
                )
            }
        }
    }
}

@Composable
private fun getOnboardingInfoText(stepIndex: Int) = when (stepIndex) {
    0 -> R.string.onboarding_step_1
    1 -> R.string.onboarding_step_2
    2 -> R.string.onboarding_step_3
    3 -> R.string.onboarding_step_4
    4 -> R.string.onboarding_step_5
    5 -> R.string.onboarding_step_6
    6 -> R.string.onboarding_step_7
    else -> null
}?.let { stringResource(it) }

@Composable
private fun ColumnScope.OnboardingVisual(
    stepIndex: Int,
    isCurrentStep: Boolean,
    uiState: OnboardingUiState,
    onUiEvent: (OnboardingUiEvent) -> Unit = {},
    modifier: Modifier
) {
    when (stepIndex) {
        0 -> AlbumArtworkCarousel(uiState.randomAlbumArtUrls, modifier)
        1 -> QuestionIconCarousel(modifier)
        2 -> PreferenceSelectionIcons(isCurrentStep, modifier)
        3 -> ExampleAlbumRecommendations(uiState.albumArtUrlsForExampleRecommendations, isCurrentStep, modifier)
        4 -> ExampleAlbumResult(uiState.exampleAlbumRecommendation, onUiEvent, modifier)
        5 -> PulsingIcon(LibzyIconTheme.RestartAlt, modifier)
        6 -> SpinningIcon(LibzyIconTheme.Settings, modifier)
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun OnboardingCta(onboardingStepPager: PagerState, modifier: Modifier, onUiEvent: (OnboardingUiEvent) -> Unit) {
    val coroutineScope = rememberCoroutineScope()
    val onLastStep = onboardingStepPager.currentPage == onboardingStepPager.pageCount - 1
    LibzyButton(
        textResId = if (onLastStep) R.string.lets_go else R.string.next,
        shape = MaterialTheme.shapes.small.copy(CornerSize(percent = 50)),
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 32.dp)
    ) {
        if (onLastStep) {
            onUiEvent(CompleteOnboarding)
        } else {
            coroutineScope.launch {
                onboardingStepPager.goToNextPage()
            }
        }
    }
}

@Composable
private fun AlbumArtworkCarousel(albumArtUrls: List<String>, modifier: Modifier) {
    BoxWithConstraints {
        val albumArtSize = maxWidth / 3
        Row(
            horizontalArrangement = Arrangement.spacedBy(0.dp),
            verticalAlignment = Alignment.CenterVertically,
            modifier = modifier
                .fadingMarquee()
                .padding(vertical = 16.dp)
        ) {
            albumArtUrls.forEach {
                AlbumArtwork(it, Modifier.size(albumArtSize).padding(end = 16.dp))
            }
        }
    }
}

@Composable
private fun QuestionIconCarousel(modifier: Modifier) {
    val numQuestions = 5
    BoxWithConstraints {
        val iconSize = maxWidth / 3
        Row(
            horizontalArrangement = Arrangement.spacedBy(0.dp),
            verticalAlignment = Alignment.CenterVertically,
            modifier = modifier.fadingMarquee()
        ) {
            repeat(numQuestions) { index ->
                LibzyIcon(
                    imageVector = LibzyIconTheme.Quiz,
                    contentDescription = null,
                    tint = LibzyColors.Gray,
                    modifier = Modifier
                        .size(iconSize)
                        .padding(end = 16.dp)
                        .offset(y = sin(2 * Math.PI / numQuestions * index).dp * 10)
                )
            }
        }
    }
}

@Composable
private fun ColumnScope.PreferenceSelectionIcons(isCurrentStep: Boolean, modifier: Modifier) {
    val iconAnimations = remember { List(4) { Animatable(0f) } }
    val jumpUpDurationMillis = 500
    val fallDownDurationMillis = 500
    val overlapDurationMillis = 500

    LaunchedEffect(isCurrentStep, iconAnimations) {

        suspend fun Animatable<Float, AnimationVector1D>.animate() {
            animateTo(1f, animationSpec = tween(jumpUpDurationMillis, easing = LinearOutSlowInEasing))
            launch { animateTo(0f, animationSpec = tween(fallDownDurationMillis, easing = LinearEasing)) }
            delay(fallDownDurationMillis.milliseconds - overlapDurationMillis.milliseconds)
        }

        while (isCurrentStep) {
            iconAnimations.forEach { it.animate() }
            delay(overlapDurationMillis.milliseconds)
        }
    }

    BoxWithConstraints(
        modifier = modifier
            .weight(1f)
            .padding(top = 16.dp),
        contentAlignment = Alignment.BottomCenter
    ) {
        val iconSize = maxWidth / 6
        val maxOffset = maxOf(maxHeight * -1 + iconSize, (-350).dp)

        Row(horizontalArrangement = Arrangement.SpaceEvenly, modifier = Modifier.fillMaxWidth()) {
            repeat(iconAnimations.size) { index ->
                val isNoPreference = index == 1 || index == 3
                val animatedOffset = maxOffset * iconAnimations[index].value
                val animatedAlpha = iconAnimations[index].value

                LibzyIcon(
                    imageVector = if (isNoPreference) LibzyIconTheme.Close else LibzyIconTheme.Check,
                    contentDescription = null,
                    tint = LibzyColors.Gray,
                    modifier = Modifier
                        .size(iconSize)
                        .offset(y = animatedOffset)
                        .alpha(animatedAlpha)
                )
            }
        }
    }
}

@Composable
private fun ColumnScope.ExampleAlbumRecommendations(
    albumArtUrls: List<List<String>>,
    isCurrentStep: Boolean,
    modifier: Modifier
) {
    var recommendationGroupIndex by remember { mutableStateOf(0) }
    val gridArrangement = Arrangement.Center

    Crossfade(
        targetState = recommendationGroupIndex,
        animationSpec = tween(durationMillis = 1000),
        modifier = modifier
            .weight(1f)
            .padding(horizontal = 16.dp),
        label = "crossfade between example album recommendations"
    ) { index ->
        if (index in albumArtUrls.indices) {
            val albumArtUrlsForCurrentGroup = albumArtUrls[index]
            val topRowAlbumArtUrls = albumArtUrlsForCurrentGroup.take(albumArtUrlsForCurrentGroup.size / 2)
            val bottomRowAlbumArtUrls = albumArtUrlsForCurrentGroup.takeLast(albumArtUrlsForCurrentGroup.size / 2)

            Column(Modifier.wrapContentSize(), gridArrangement, Alignment.CenterHorizontally) {
                Spacer(Modifier.weight(1f))
                Row(horizontalArrangement = gridArrangement) {
                    topRowAlbumArtUrls.forEach {
                        AlbumArtwork(it, Modifier.weight(1f).padding(4.dp))
                    }
                }
                Row(horizontalArrangement = gridArrangement) {
                    bottomRowAlbumArtUrls.forEach {
                        AlbumArtwork(it, Modifier.weight(1f).padding(4.dp))
                    }
                }
                Spacer(Modifier.weight(1f))
            }
        }
    }

    LaunchedEffect(isCurrentStep) {
        while (isCurrentStep) {
            delay(2.seconds)
            recommendationGroupIndex = (recommendationGroupIndex + 1) % albumArtUrls.size
        }
    }
}

@Composable
private fun ColumnScope.ExampleAlbumResult(
    album: AlbumUiState?,
    onUiEvent: (OnboardingUiEvent) -> Unit,
    modifier: Modifier = Modifier
) {
    if (album != null) {
        Spacer(Modifier.weight(1f))
        BoxWithConstraints(modifier.align(Alignment.CenterHorizontally)) {
            val albumArtWidth = maxWidth * 0.4f
            AlbumListItem(
                album = album,
                onAlbumClick = { if (it is OnboardingUiEvent) onUiEvent(it) },
                modifier = Modifier.width(albumArtWidth + ALBUM_LIST_ITEM_PADDING.dp),
                maxLinesPerLabel = 2
            )
        }
        Spacer(Modifier.weight(1f))
    }
}

@Composable
private fun ColumnScope.PulsingIcon(icon: ImageVector, modifier: Modifier) {
    val scale by rememberInfiniteTransition(label = "pulsing icon transition").animateFloat(
        initialValue = 0.7f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulsing icon scale"
    )
    LibzyIcon(
        imageVector = icon,
        contentDescription = null,
        tint = LibzyColors.Gray,
        modifier = modifier
            .weight(1f)
            .align(Alignment.CenterHorizontally)
            .widthIn(max = 250.dp)
            .fillMaxWidth()
            .scale(scale)
    )
}

@Composable
private fun ColumnScope.SpinningIcon(icon: ImageVector, modifier: Modifier) {
    val rotation by rememberInfiniteTransition().animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 7000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        )
    )
    LibzyIcon(
        imageVector = icon,
        contentDescription = null,
        tint = LibzyColors.Gray,
        modifier = modifier
            .weight(1f)
            .align(Alignment.CenterHorizontally)
            .widthIn(max = 200.dp)
            .fillMaxWidth()
            .rotate(rotation)
    )
}

private val previewUiState = OnboardingUiState(
    randomAlbumArtUrls = listOf(
        "https://i.scdn.co/image/ab67616d0000b2730869a7bb597fe38cb1b98102",
        "https://i.scdn.co/image/ab67616d0000b273c55d2f306d5dffbc66f67c52",
        "https://i.scdn.co/image/ab67616d0000b27313e54d6687e65678d60466c2",
        "https://i.scdn.co/image/ab67616d0000b273f66c0cfb9c2c87a9c299c800",
        "https://i.scdn.co/image/ab67616d0000b273c5626e40ff7ec9f2a9405bf3",
        "https://i.scdn.co/image/ab67616d0000b273e4d7c0434669bb29c4480f8f",
        "https://i.scdn.co/image/ab67616d0000b2733f29c5269be5a4e9c4611d7a",
        "https://i.scdn.co/image/ab67616d0000b2735901aaa980d3e714bf01171c",
    ),
    albumArtUrlsForExampleRecommendations = listOf(
        listOf(
            "https://i.scdn.co/image/ab67616d0000b2730869a7bb597fe38cb1b98102",
            "https://i.scdn.co/image/ab67616d0000b273c55d2f306d5dffbc66f67c52",
            "https://i.scdn.co/image/ab67616d0000b27313e54d6687e65678d60466c2",
            "https://i.scdn.co/image/ab67616d0000b273f66c0cfb9c2c87a9c299c800",
            "https://i.scdn.co/image/ab67616d0000b273c5626e40ff7ec9f2a9405bf3",
            "https://i.scdn.co/image/ab67616d0000b273e4d7c0434669bb29c4480f8f"
        ),
        listOf(
            "https://i.scdn.co/image/ab67616d0000b2733f29c5269be5a4e9c4611d7a",
            "https://i.scdn.co/image/ab67616d0000b273f66c0cfb9c2c87a9c299c800",
            "https://i.scdn.co/image/ab67616d0000b273e4d7c0434669bb29c4480f8f",
            "https://i.scdn.co/image/ab67616d0000b2730869a7bb597fe38cb1b98102",
            "https://i.scdn.co/image/ab67616d0000b2735901aaa980d3e714bf01171c",
            "https://i.scdn.co/image/ab67616d0000b273c55d2f306d5dffbc66f67c52"
        )
    ),
    exampleAlbumRecommendation = AlbumUiState(
        title = "Bitches Brew".toTextResource(),
        artists = "Miles Davis".toTextResource(),
        spotifyUri = "",
        spotifyId = "",
        artworkUrl = "https://i.scdn.co/image/ab67616d0000b2733f29c5269be5a4e9c4611d7a"
    )
)

@Preview
@Composable
fun OnboardingStep1Preview() {
    LibzyContent {
        OnboardingScreen(previewUiState, useAutotypingEffect = false)
    }
}

@Preview(device = Devices.PIXEL_3A)
@Composable
fun OnboardingStep5Preview() {
    LibzyContent {
        Column {
            OnboardingScreen(previewUiState, initialStepIndex = 4, useAutotypingEffect = false)
        }
    }
}