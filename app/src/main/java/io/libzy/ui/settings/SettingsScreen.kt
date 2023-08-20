package io.libzy.ui.settings

import androidx.activity.compose.BackHandler
import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.AlertDialog
import androidx.compose.material.Checkbox
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.ExtendedFloatingActionButton
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.material.icons.rounded.Add
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import io.libzy.R
import io.libzy.domain.Query
import io.libzy.ui.Destination
import io.libzy.ui.common.component.BackIcon
import io.libzy.ui.common.component.LibzyIcon
import io.libzy.ui.common.component.LibzyScaffold
import io.libzy.ui.common.component.LoadedContent
import io.libzy.ui.common.util.hideIf
import io.libzy.ui.common.util.scrollableFade
import io.libzy.ui.common.util.surfaceBackground
import io.libzy.ui.settings.SettingsUiEvent.CloseLogOutConfirmation
import io.libzy.ui.settings.SettingsUiEvent.ExpandLibrary
import io.libzy.ui.settings.SettingsUiEvent.LogOut
import io.libzy.ui.settings.SettingsUiEvent.OpenLogOutConfirmation
import io.libzy.ui.settings.SettingsUiEvent.OpenTutorial
import io.libzy.ui.settings.SettingsUiEvent.ReturnToQuery
import io.libzy.ui.settings.SettingsUiEvent.SyncLibrary
import io.libzy.ui.settings.SettingsUiEvent.ToggleQueryParam
import io.libzy.ui.theme.LibzyDimens.FAB_REGION_HEIGHT
import io.libzy.ui.theme.LibzyDimens.HORIZONTAL_INSET
import io.libzy.ui.theme.LibzyIconTheme
import io.libzy.util.TextResource
import io.libzy.util.resolveText

@Composable
fun SettingsScreen(navController: NavController, viewModelFactory: ViewModelProvider.Factory) {
    val viewModel: SettingsViewModel = viewModel(factory = viewModelFactory)
    val uiState by viewModel.uiStateFlow.collectAsState()

    if (uiState.logOutState == LogOutState.LoggedOut) {
        LaunchedEffect(Unit) {
            navController.navigate(Destination.Query.route) {
                popUpTo(Destination.Query.route) {
                    inclusive = true
                }
            }
        }
    }

    SettingsScreen(uiState) { uiEvent ->
        when (uiEvent) {
            is ReturnToQuery -> navController.popBackStack()
            is OpenTutorial -> navController.navigate(Destination.Onboarding.route)
            is ExpandLibrary -> navController.navigate(Destination.ExpandLibrary.route)
            is SyncLibrary -> viewModel.syncLibrary()
            is OpenLogOutConfirmation -> viewModel.openLogOutConfirmation()
            is CloseLogOutConfirmation -> viewModel.closeLogOutConfirmation()
            is LogOut -> viewModel.logOut()
            is ToggleQueryParam -> viewModel.toggleQueryParam(uiEvent.param)
        }
    }
}

@Composable
private fun SettingsScreen(uiState: SettingsUiState, onUiEvent: (SettingsUiEvent) -> Unit) {
    LoadedContent(uiState.loading) {
        BackHandler {
            onUiEvent(ReturnToQuery)
        }
        LibzyScaffold(
            navigationIcon = { BackIcon(onClick = { onUiEvent(ReturnToQuery) }) },
            actionIcons = {
                IconButton(onClick = { onUiEvent(OpenTutorial) }) {
                    LibzyIcon(painterResource(R.drawable.ic_help), contentDescription = stringResource(R.string.tutorial))
                }
            },
            title = { Text(stringResource(R.string.settings)) },
            floatingActionButton = {
                LogOutButton(onUiEvent)
            }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .scrollableFade(
                        topFadeHeight = 16.dp,
                        bottomFadeHeight = FAB_REGION_HEIGHT.dp
                    )
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(Modifier.height(16.dp))
                AskMeAboutSection(uiState.enabledQueryParams, onUiEvent)
                LibrarySyncSection(uiState.lastLibrarySyncDate, uiState.syncingLibrary, onUiEvent)
                YourCollectionSection(onUiEvent)
                AppVersionSection(uiState.appVersion)
                Spacer(Modifier.height(FAB_REGION_HEIGHT.dp + 16.dp))
            }
            if (uiState.logOutState == LogOutState.Confirmation) {
                LogOutConfirmationDialog(onUiEvent)
            }
        }
    }
}

@Composable
private fun LogOutButton(onUiEvent: (SettingsUiEvent) -> Unit) {
    ExtendedFloatingActionButton(
        text = { Text(stringResource(R.string.log_out).uppercase()) },
        onClick = { onUiEvent(OpenLogOutConfirmation) },
        icon = {
            Icon(
                painter = painterResource(R.drawable.ic_spotify_black),
                contentDescription = stringResource(R.string.cd_spotify_logo)
            )
        }
    )
}

@Composable
private fun AskMeAboutSection(enabledQueryParams: Set<Query.Parameter>, onUiEvent: (SettingsUiEvent) -> Unit) {
    SettingsSection(headerTextResId = R.string.ask_me_about) {
        Query.Parameter.defaultOrder.forEach { queryParam ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(
                    checked = queryParam in enabledQueryParams,
                    enabled = queryParam !in enabledQueryParams || enabledQueryParams.size > 1,
                    onCheckedChange = { onUiEvent(ToggleQueryParam(queryParam)) }
                )
                Text(stringResource(queryParam.labelResId), textAlign = TextAlign.Start)
            }
        }
    }
}

@Composable
private fun LibrarySyncSection(
    lastLibrarySyncDate: TextResource,
    syncingLibrary: Boolean,
    onUiEvent: (SettingsUiEvent) -> Unit
) {
    SettingsSection(headerTextResId = R.string.last_sync_date_label) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = HORIZONTAL_INSET.dp)
        ) {
            Text(lastLibrarySyncDate.resolveText(), style = MaterialTheme.typography.body1)
            Spacer(Modifier.weight(1f))
            LibrarySyncButton(syncingLibrary, onUiEvent)
        }
    }
}

@Composable
private fun YourCollectionSection(onUiEvent: (SettingsUiEvent) -> Unit) {
    SettingsSection(headerTextResId = R.string.manage_your_collection) {
        TextButton(onClick = { onUiEvent(ExpandLibrary) }) {
            Icon(LibzyIconTheme.Add, contentDescription = null, Modifier.padding(end = 8.dp))
            Text(stringResource(R.string.add_more_albums))
        }
    }
}

@Composable
private fun LibrarySyncButton(syncingLibrary: Boolean, onUiEvent: (SettingsUiEvent) -> Unit) {
    Box(contentAlignment = Alignment.Center) {
        TextButton(
            modifier = Modifier.hideIf(syncingLibrary),
            enabled = !syncingLibrary,
            onClick = { onUiEvent(SyncLibrary) }
        ) {
            Text(stringResource(R.string.sync), maxLines = 1)
        }
        CircularProgressIndicator(
            Modifier
                .size(24.dp)
                .hideIf(!syncingLibrary))
    }
}

@Composable
private fun LogOutConfirmationDialog(onUiEvent: (SettingsUiEvent) -> Unit) {
    AlertDialog(
        onDismissRequest = { onUiEvent(CloseLogOutConfirmation) },
        dismissButton = {
            TextButton(onClick = { onUiEvent(CloseLogOutConfirmation) }) {
                Text(stringResource(R.string.action_cancel).uppercase())
            }
        },
        confirmButton = {
            TextButton(onClick = { onUiEvent(LogOut) }) {
                Text(stringResource(R.string.log_out).uppercase())
            }
        },
        title = { Text(stringResource(R.string.log_out)) },
        text = { Text(stringResource(R.string.log_out_dialog_description), textAlign = TextAlign.Start) }
    )
}

@Composable
private fun AppVersionSection(appVersion: TextResource) {
    Text(
        text = appVersion.resolveText(),
        style = MaterialTheme.typography.caption,
        modifier = Modifier.padding(vertical = 16.dp)
    )
}

@Composable
private fun SettingsSection(
    @StringRes headerTextResId: Int,
    content: @Composable ColumnScope.() -> Unit
) {
    Box(
        modifier = Modifier
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .surfaceBackground()
            .padding(16.dp)
            .fillMaxWidth()
    ) {
        Column {
            Text(
                text = stringResource(headerTextResId),
                modifier = Modifier
                    .padding(bottom = 8.dp)
                    .padding(horizontal = HORIZONTAL_INSET.dp),
                style = MaterialTheme.typography.h6.copy(textAlign = TextAlign.Start),
                textAlign = TextAlign.Start
            )
            content()
        }
    }
}