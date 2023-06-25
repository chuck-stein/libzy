package io.libzy.ui.onboarding

import io.libzy.domain.AlbumResult

data class OnboardingUiState(
    val randomAlbumArtUrls: List<String> = emptyList(),
    val albumArtUrlsForExampleRecommendations: List<List<String>> = emptyList(),
    val exampleAlbumResult: AlbumResult? = null,
    val onboardingCompleted: Boolean = false,
    val onboardingMandatory: Boolean = true
)
