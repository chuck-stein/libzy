package io.libzy.ui.onboarding

import io.libzy.ui.common.component.AlbumUiState

data class OnboardingUiState(
    val randomAlbumArtUrls: List<String> = emptyList(),
    val albumArtUrlsForExampleRecommendations: List<List<String>> = emptyList(),
    val exampleAlbumRecommendation: AlbumUiState? = null,
    val onboardingCompleted: Boolean = false,
    val onboardingMandatory: Boolean = true
)
