package io.libzy.view.connect

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.*
import io.libzy.R
import io.libzy.work.RefreshLibraryWorker
import kotlinx.coroutines.launch
import javax.inject.Inject

class ConnectSpotifyViewModel @Inject constructor(private val appContext: Context) : ViewModel() {

    companion object {
        private const val LIBRARY_SCAN_WORK_TAG = "initial_library_scan"
    }

    private val workManager = WorkManager.getInstance(appContext)

    val libraryScanWorkInfo: LiveData<List<WorkInfo>> = workManager.getWorkInfosByTagLiveData(LIBRARY_SCAN_WORK_TAG) // TODO: add a Transformations map to save the work id if !state.isFinished, and only send finished work if it was previously unfinished

    fun scanLibrary() {
        val alreadyScanning =
            appContext.getSharedPreferences(appContext.getString(R.string.spotify_prefs_name), Context.MODE_PRIVATE)
                .getBoolean(appContext.getString(R.string.spotify_initial_scan_in_progress_key), false)
        if (alreadyScanning) return

        viewModelScope.launch {
            val workRequest = OneTimeWorkRequestBuilder<RefreshLibraryWorker>()
                .setInputData(workDataOf(RefreshLibraryWorker.IS_INITIAL_SCAN to true))
                .addTag(LIBRARY_SCAN_WORK_TAG)
                .build()

            workManager.enqueueUniqueWork(
                RefreshLibraryWorker.WORK_NAME,
                ExistingWorkPolicy.REPLACE,
                workRequest
            )
        }
    }
}