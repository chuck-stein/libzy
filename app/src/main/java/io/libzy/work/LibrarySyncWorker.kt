package io.libzy.work

import android.app.NotificationManager
import android.content.Context
import android.content.pm.ServiceInfo
import android.net.Uri
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.ForegroundInfo
import androidx.work.ListenableWorker
import androidx.work.WorkerFactory
import androidx.work.WorkerParameters
import com.adamratzman.spotify.SpotifyException
import io.libzy.R
import io.libzy.analytics.AnalyticsDispatcher
import io.libzy.analytics.LibrarySyncResult
import io.libzy.config.NotificationIds
import io.libzy.repository.SessionRepository
import io.libzy.repository.UserLibraryRepository
import io.libzy.ui.Destination
import io.libzy.util.appInForeground
import io.libzy.util.createNotificationTapAction
import timber.log.Timber
import kotlin.time.Duration.Companion.minutes
import kotlin.time.DurationUnit
import kotlin.time.TimedValue
import kotlin.time.measureTimedValue
import kotlin.time.toJavaDuration

class LibrarySyncWorker(
    appContext: Context,
    params: WorkerParameters,
    private val userLibraryRepository: UserLibraryRepository,
    private val sessionRepository: SessionRepository,
    private val analyticsDispatcher: AnalyticsDispatcher
) : CoroutineWorker(appContext, params) {

    companion object {
        const val WORK_NAME = "io.libzy.work.LibrarySyncWorker"

        // the parameter key for this worker's input data, representing whether this is the first-time library sync,
        // which is initiated when the user first connects their spotify account
        const val IS_INITIAL_SYNC = "is_initial_sync"

        val LIBRARY_SYNC_INTERVAL = 15.minutes.toJavaDuration() // time to wait between Spotify library syncs
    }

    private val isInitialSync = inputData.getBoolean(IS_INITIAL_SYNC, false)

    override suspend fun doWork(): Result {
        if (sessionRepository.isSpotifyAuthExpired() && !appInForeground()) {
            // If auth has expired and the app is in the background,
            // fail the library sync job since we need to be in the foreground to refresh auth
            analyticsDispatcher.sendSyncLibraryDataEvent(LibrarySyncResult.FAILURE, isInitialSync)
            return Result.failure()
        }
        beforeLibrarySync()

        val numAlbumsSynced = try {
            measureTimedValue {
                userLibraryRepository.syncLibraryData()
            }
        } catch (e: SpotifyException.BadRequestException) {
            e.statusCode.let { statusCode ->
                // TODO: find a better way to always catch all server errors
                return if (!isInitialSync && isServerError(statusCode)) {
                    Timber.e(e, "Failed to sync Spotify library data due to a server error. Retrying...")
                    analyticsDispatcher.sendSyncLibraryDataEvent(LibrarySyncResult.RETRY, isInitialSync = false)
                    Result.retry()
                } else fail(e)
            }
        } catch (e: Exception) {
            return fail(e)
        }

        afterLibrarySync(numAlbumsSynced)
        return Result.success()
    }

    private suspend fun beforeLibrarySync() {
        Timber.i("Initiating Spotify library data sync...")

        if (isInitialSync) {
            setForeground(createInitialSyncForegroundInfo())
        }
    }

    private suspend fun afterLibrarySync(numAlbumsSynced: TimedValue<Int>) {
        if (isInitialSync) {
            sessionRepository.setSpotifyConnected(true)
            notifyLibrarySyncEnded(
                notificationTitleResId = R.string.initial_library_sync_succeeded_notification_title,
                notificationTextResId = R.string.initial_library_sync_succeeded_notification_text,
                tapDestinationUri = Destination.Query.deepLinkUri
            )
        }
        sessionRepository.setLastSyncTimestamp(System.currentTimeMillis())
        Timber.i("Successfully synced Spotify library data")
        analyticsDispatcher.sendSyncLibraryDataEvent(
            LibrarySyncResult.SUCCESS,
            isInitialSync,
            numAlbumsSynced.value,
            numAlbumsSynced.duration.toDouble(DurationUnit.SECONDS)
        )
    }

    private fun isServerError(statusCode: Int?) = statusCode != null && statusCode >= 500 && statusCode < 600

    private fun fail(exception: Exception): Result {
        Timber.e(exception, "Failed to sync Spotify library data")
        analyticsDispatcher.sendSyncLibraryDataEvent(LibrarySyncResult.FAILURE, isInitialSync)
        if (isInitialSync) {
            notifyLibrarySyncEnded(
                notificationTitleResId = R.string.initial_library_sync_failed_notification_title,
                notificationTextResId = R.string.initial_library_sync_failed_notification_text,
                tapDestinationUri = Destination.ConnectSpotify.deepLinkUri
            )
        }
        return Result.failure()
    }

    private fun createInitialSyncForegroundInfo(): ForegroundInfo {
        val notificationChannelId = applicationContext.getString(R.string.library_sync_progress_notification_channel_id)
        val notificationTitle = applicationContext.getString(R.string.initial_library_sync_notification_title)

        val notification = NotificationCompat.Builder(applicationContext, notificationChannelId)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(notificationTitle)
            .setContentIntent(applicationContext.createNotificationTapAction(Destination.ConnectSpotify.deepLinkUri))
            .setCategory(NotificationCompat.CATEGORY_PROGRESS)
            .setOngoing(true)
            .setShowWhen(false)
            .setTicker(notificationTitle)
            .build()

        return if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
            ForegroundInfo(NotificationIds.initialSyncProgress, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC)
        } else {
            ForegroundInfo(NotificationIds.initialSyncProgress, notification)
        }
    }

    private fun notifyLibrarySyncEnded(
        notificationTitleResId: Int,
        notificationTextResId: Int,
        tapDestinationUri: Uri
    ) {
        if (appInForeground()) return // no need to send notification, user will see that the sync has ended

        val notificationTitle = applicationContext.getString(notificationTitleResId)
        val notificationText = applicationContext.getString(notificationTextResId)
        val notificationChannelId = applicationContext.getString(R.string.library_sync_update_notification_channel_id)

        val notification = NotificationCompat.Builder(applicationContext, notificationChannelId)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(notificationTitle)
            .setContentText(notificationText)
            .setContentIntent(applicationContext.createNotificationTapAction(tapDestinationUri))
            .setAutoCancel(true)
            .setCategory(NotificationCompat.CATEGORY_PROGRESS)
            .setTicker(notificationTitle)
            .build()

        val notificationManager =
            applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(NotificationIds.initialSyncEnd, notification)
    }

    class Factory(
        private val userLibraryRepository: UserLibraryRepository,
        private val sessionRepository: SessionRepository,
        private val analyticsDispatcher: AnalyticsDispatcher,
    ) : WorkerFactory() {

        override fun createWorker(
            appContext: Context,
            workerClassName: String,
            workerParameters: WorkerParameters
        ): ListenableWorker? {

            return when (workerClassName) {
                LibrarySyncWorker::class.java.name -> LibrarySyncWorker(
                    appContext, workerParameters, userLibraryRepository, sessionRepository, analyticsDispatcher
                )
                else -> null
            }
        }
    }
}
