package com.chuckstein.libzy.work

import android.app.NotificationManager
import android.content.Context
import android.content.SharedPreferences
import android.content.pm.ServiceInfo
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.content.edit
import androidx.work.*
import com.adamratzman.spotify.SpotifyException
import com.chuckstein.libzy.BuildConfig
import com.chuckstein.libzy.R
import com.chuckstein.libzy.common.appInForeground
import com.chuckstein.libzy.common.createNotificationTapAction
import com.chuckstein.libzy.common.currentTimeSeconds
import com.chuckstein.libzy.repository.UserLibraryRepository
import com.chuckstein.libzy.spotify.auth.SpotifyAuthException

class RefreshLibraryWorker(
    appContext: Context,
    params: WorkerParameters,
    private val userLibraryRepository: UserLibraryRepository
) : CoroutineWorker(appContext, params) {

    companion object {
        private val TAG = RefreshLibraryWorker::class.java.simpleName
        const val WORK_NAME = "com.chuckstein.libzy.work.RefreshLibraryWorker"

        // the parameter key for this worker's input data, representing whether this is the first-time library scan,
        // which is initiated when the user first connects their spotify account
        const val IS_INITIAL_SCAN = "is_initial_scan"
    }

    override suspend fun doWork(): Result {

        if (BuildConfig.DEBUG) Log.d(TAG, "Initiating Spotify library data sync...")

        val isInitialScan = inputData.getBoolean(IS_INITIAL_SCAN, false)

        // load shared preferences if this is the initial scan, otherwise we won't need it
        val sharedPrefs = if (!isInitialScan) null else applicationContext.getSharedPreferences(
            applicationContext.getString(R.string.spotify_prefs_name),
            Context.MODE_PRIVATE
        )

        if (isInitialScan) {
            setForeground(createInitialScanForegroundInfo())
            sharedPrefs?.edit {
                putBoolean(applicationContext.getString(R.string.spotify_initial_scan_in_progress_key), true)
            }
        }

        try {
            userLibraryRepository.refreshLibraryData()
        } catch (e: SpotifyException.BadRequestException) {
            e.statusCode.let { statusCode ->
                // TODO: find a better way to always catch all server errors (may have to forgo the Spotify API wrapper library)
                return if (!isInitialScan && isServerError(statusCode)) {
                    Log.e(TAG, "Failed to refresh Spotify library data due to a server error. Retrying...", e)
                    Result.retry()
                } else fail(e, isInitialScan, sharedPrefs)
            }
        } catch (e: SpotifyException) {
            return fail(e, isInitialScan, sharedPrefs)
        } catch (e: SpotifyAuthException) {
            return fail(e, isInitialScan, sharedPrefs)
        }
        if (isInitialScan) {
            sharedPrefs?.edit {
                putBoolean(applicationContext.getString(R.string.spotify_connected_key), true)
                putBoolean(applicationContext.getString(R.string.spotify_initial_scan_in_progress_key), false)
            }
            notifyLibraryScanEnded(
                notificationTitleResId = R.string.initial_library_scan_succeeded_notification_title,
                notificationTextResId = R.string.initial_library_scan_succeeded_notification_text,
                tapDestinationResId = R.id.queryFragment
            )
        }
        return Result.success()
    }

    private fun isServerError(statusCode: Int?) = statusCode != null && statusCode >= 500 && statusCode < 600

    private fun fail(exception: Exception, isInitialScan: Boolean, sharedPrefs: SharedPreferences?): Result {
        Log.e(TAG, "Failed to refresh Spotify library data", exception)
        if (isInitialScan) {
            sharedPrefs?.edit {
                putBoolean(applicationContext.getString(R.string.spotify_initial_scan_in_progress_key), false)
            }
            notifyLibraryScanEnded(
                notificationTitleResId = R.string.initial_library_scan_failed_notification_title,
                notificationTextResId = R.string.initial_library_scan_failed_notification_text,
                tapDestinationResId = R.id.connectSpotifyFragment
            )
        }
        return Result.failure()
    }

    private fun createInitialScanForegroundInfo(): ForegroundInfo {
        val notificationChannelId = applicationContext.getString(R.string.library_scan_progress_notification_channel_id)
        val notificationTitle = applicationContext.getString(R.string.initial_library_scan_notification_title)

        val notification = NotificationCompat.Builder(applicationContext, notificationChannelId)
            .setSmallIcon(R.drawable.placeholder_album_art) // TODO: replace this with app icon once it's made (in the mean time, figure out why it just shows a gray square)
            .setContentTitle(notificationTitle)
            .setContentIntent(applicationContext.createNotificationTapAction(R.id.connectSpotifyFragment))
            .setCategory(NotificationCompat.CATEGORY_PROGRESS) // TODO: if scan progress is never added to this notification, change to CATEGORY_SERVICE
            .setOngoing(true)
            .setShowWhen(false)
            .setTicker(notificationTitle)
            .build()

        val notificationId = currentTimeSeconds()
        return ForegroundInfo(notificationId, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC)
    }

    private fun notifyLibraryScanEnded(notificationTitleResId: Int, notificationTextResId: Int, tapDestinationResId: Int) {
        if (appInForeground()) return // no need to send notification, user will see that the scan has ended

        val notificationTitle = applicationContext.getString(notificationTitleResId)
        val notificationText = applicationContext.getString(notificationTextResId)
        val notificationChannelId = applicationContext.getString(R.string.library_scan_update_notification_channel_id)

        val notification = NotificationCompat.Builder(applicationContext, notificationChannelId)
            .setSmallIcon(R.drawable.placeholder_album_art) // TODO: replace this with app icon once it's made
            .setContentTitle(notificationTitle)
            .setContentText(notificationText)
            .setContentIntent(applicationContext.createNotificationTapAction(tapDestinationResId))
            .setAutoCancel(true)
            .setCategory(NotificationCompat.CATEGORY_PROGRESS)
            .setTicker(notificationTitle)
            .build()

        val notificationManager =
            applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val notificationId = currentTimeSeconds()
        notificationManager.notify(notificationId, notification)
    }

    class Factory(private val userLibraryRepository: UserLibraryRepository) : WorkerFactory() {

        override fun createWorker(
            appContext: Context,
            workerClassName: String,
            workerParameters: WorkerParameters
        ): ListenableWorker? {

            return when (workerClassName) {
                RefreshLibraryWorker::class.java.name ->
                    RefreshLibraryWorker(appContext, workerParameters, userLibraryRepository)
                else -> null
            }
        }
    }
}