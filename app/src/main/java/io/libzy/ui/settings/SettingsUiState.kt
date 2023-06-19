package io.libzy.ui.settings

import io.libzy.R
import io.libzy.domain.Query
import io.libzy.util.TextResource
import io.libzy.util.emptyTextResource
import io.libzy.util.toTextResource

data class SettingsUiState(
    val enabledQueryParams: Set<Query.Parameter> = Query.Parameter.defaultOrder.toSet(),
    val lastLibrarySyncDate: TextResource = R.string.unknown.toTextResource(),
    val syncingLibrary: Boolean = false,
    val logOutState: LogOutState = LogOutState.None,
    val appVersion: TextResource = emptyTextResource,
    val loading: Boolean = false
)

enum class LogOutState {
    None,
    Confirmation,
    LoggedOut
}
