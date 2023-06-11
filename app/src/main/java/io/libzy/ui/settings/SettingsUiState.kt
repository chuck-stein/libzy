package io.libzy.ui.settings

import io.libzy.R
import io.libzy.domain.Query
import io.libzy.ui.findalbum.query.QueryUiState
import io.libzy.util.TextResource
import io.libzy.util.toTextResource

data class SettingsUiState(
    val enabledQueryParams: Set<Query.Parameter> = QueryUiState.DEFAULT_STEP_ORDER.toSet(),
    val lastLibrarySyncDate: TextResource = R.string.unknown.toTextResource(),
    val syncingLibrary: Boolean = false,
    val logOutState: LogOutState = LogOutState.None,
    val loading: Boolean = false,
)

enum class LogOutState {
    None,
    Confirmation,
    LoggedOut
}