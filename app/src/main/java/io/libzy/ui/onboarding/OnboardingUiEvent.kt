package io.libzy.ui.onboarding

sealed interface OnboardingUiEvent {
    object PlayAlbum : OnboardingUiEvent
    object CompleteOnboarding: OnboardingUiEvent
}