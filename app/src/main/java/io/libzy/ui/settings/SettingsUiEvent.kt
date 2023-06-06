package io.libzy.ui.settings

import io.libzy.domain.Query

sealed interface SettingsUiEvent {
    object ReturnToQuery : SettingsUiEvent
    data class ToggleQueryParam(val param: Query.Parameter) : SettingsUiEvent
    object SyncLibrary : SettingsUiEvent
    object OpenLogOutConfirmation : SettingsUiEvent
    object CloseLogOutConfirmation : SettingsUiEvent
    object LogOut : SettingsUiEvent
}
