package io.libzy.ui.settings

import android.icu.text.DateFormat
import androidx.lifecycle.asFlow
import androidx.lifecycle.viewModelScope
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkInfo
import androidx.work.WorkManager
import io.libzy.R
import io.libzy.domain.Query
import io.libzy.persistence.prefs.PrefsStore
import io.libzy.repository.SessionRepository
import io.libzy.repository.SettingsRepository
import io.libzy.repository.UserLibraryRepository
import io.libzy.ui.common.StateOnlyViewModel
import io.libzy.ui.findalbum.query.QueryUiState
import io.libzy.util.flatten
import io.libzy.util.toTextResource
import io.libzy.work.LibrarySyncWorker
import io.libzy.work.LibrarySyncWorker.Companion.LIBRARY_SYNC_INTERVAL
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import javax.inject.Inject

class SettingsViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository,
    private val sessionRepository: SessionRepository,
    private val userLibraryRepository: UserLibraryRepository,
    private val prefsStore: PrefsStore,
    private val workManager: WorkManager,
) : StateOnlyViewModel<SettingsUiState>() {

    private val lastSyncDateFormat = DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT)

    override val initialUiState = SettingsUiState(loading = true)

    init {
        collectPreferences()
        collectLibrarySyncState()
    }

    private fun collectPreferences() {
        viewModelScope.launch {
            settingsRepository.enabledQueryParams.collect { enabledQueryParams ->
                updateUiState {
                    copy(
                        enabledQueryParams = enabledQueryParams
                            ?.map { Query.Parameter.fromString(it) }
                            ?.toSet()
                            ?: QueryUiState.DEFAULT_STEP_ORDER.toSet(),
                        loading = false
                    )
                }
            }
        }
        viewModelScope.launch {
            sessionRepository.lastSyncTimestampMillis.collect { lastSyncTimestampMillis ->
                updateUiState {
                    copy(
                        lastLibrarySyncDate = lastSyncTimestampMillis
                            ?.let { lastSyncDateFormat.format(it) }
                            ?.toTextResource()
                            ?: R.string.unknown.toTextResource(),
                        loading = false
                    )
                }
            }
        }
    }

    private fun collectLibrarySyncState() {
        viewModelScope.launch {
            workManager
                .getWorkInfosForUniqueWorkLiveData(LibrarySyncWorker.WORK_NAME)
                .asFlow()
                .flatten()
                .map { it.state }
                .collect { librarySyncState ->
                    updateUiState {
                        copy(syncingLibrary = librarySyncState == WorkInfo.State.RUNNING)
                    }
                }
        }
    }

    fun toggleQueryParam(param: Query.Parameter) {
        val enabledQueryParams = with(uiState) {
            when {
                param in enabledQueryParams && enabledQueryParams.size > 1 -> enabledQueryParams.minus(param)
                else -> enabledQueryParams.plus(param)
            }
        }

        viewModelScope.launch {
            settingsRepository.setEnabledQueryParams(enabledQueryParams)
        }
    }

    fun syncLibrary() {
        val workRequest = PeriodicWorkRequestBuilder<LibrarySyncWorker>(LIBRARY_SYNC_INTERVAL).build()
        workManager.enqueueUniquePeriodicWork(
            LibrarySyncWorker.WORK_NAME,
            ExistingPeriodicWorkPolicy.CANCEL_AND_REENQUEUE,
            workRequest
        )
    }

    fun openLogOutConfirmation() {
        updateUiState { copy(logOutState = LogOutState.Confirmation) }
    }

    fun closeLogOutConfirmation() {
        updateUiState { copy(logOutState = LogOutState.None) }
    }

    fun logOut() {
        viewModelScope.launch {
            prefsStore.clear()
            userLibraryRepository.clearLibraryData()
            workManager.cancelUniqueWork(LibrarySyncWorker.WORK_NAME)
            updateUiState {
                copy(logOutState = LogOutState.LoggedOut)
            }
        }
    }

    fun resetLogOutState() {
        updateUiState {
            copy(logOutState = LogOutState.None)
        }
    }
}