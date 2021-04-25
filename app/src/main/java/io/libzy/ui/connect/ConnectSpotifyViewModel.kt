package io.libzy.ui.connect

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.*
import io.libzy.R
import io.libzy.work.LibrarySyncWorker
import kotlinx.coroutines.launch
import javax.inject.Inject

class ConnectSpotifyViewModel @Inject constructor(private val appContext: Context) : ViewModel() {

    companion object {
        private const val LIBRARY_SCAN_WORK_TAG = "initial_library_scan"
    }

    private val workManager = WorkManager.getInstance(appContext)

    val libraryScanWorkInfo: LiveData<List<WorkInfo>> = workManager.getWorkInfosByTagLiveData(LIBRARY_SCAN_WORK_TAG)

    fun scanLibrary() {
        val alreadyScanning =
            appContext.getSharedPreferences(appContext.getString(R.string.spotify_prefs_name), Context.MODE_PRIVATE)
                .getBoolean(appContext.getString(R.string.spotify_initial_scan_in_progress_key), false)
        // TODO: instead of checking shared prefs here, check if the worker is running
        //  (then can we can probably remove these shared prefs scanning state properties)
        if (alreadyScanning) return

        viewModelScope.launch {
            val workRequest = OneTimeWorkRequestBuilder<LibrarySyncWorker>()
                .setInputData(workDataOf(LibrarySyncWorker.IS_INITIAL_SCAN to true))
                .addTag(LIBRARY_SCAN_WORK_TAG)
                .build()

            workManager.enqueueUniqueWork(
                LibrarySyncWorker.WORK_NAME,
                ExistingWorkPolicy.REPLACE,
                workRequest
            )
        }
    }
}
