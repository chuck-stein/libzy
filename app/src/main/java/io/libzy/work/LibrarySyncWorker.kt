package io.libzy.work

import android.app.NotificationManager
import android.content.Context
import android.content.pm.ServiceInfo
import androidx.core.app.NotificationCompat
import androidx.core.content.edit
import androidx.work.*
import com.adamratzman.spotify.SpotifyException
import com.google.firebase.analytics.FirebaseAnalytics.Param.SUCCESS
import com.google.firebase.analytics.ktx.analytics
import com.google.firebase.analytics.ktx.logEvent
import com.google.firebase.ktx.Firebase
import io.libzy.R
import io.libzy.analytics.Analytics
import io.libzy.analytics.Analytics.EventProperties.LIBRARY_SYNC_TIME
import io.libzy.analytics.Analytics.EventProperties.NUM_ALBUMS_SYNCED
import io.libzy.analytics.Analytics.Events.RETRY_LIBRARY_SYNC
import io.libzy.analytics.Analytics.Events.SYNC_LIBRARY_DATA
import io.libzy.repository.UserLibraryRepository
import io.libzy.spotify.auth.SpotifyAuthException
import io.libzy.util.appInForeground
import io.libzy.util.createNotificationTapAction
import io.libzy.util.currentTimeSeconds
import io.libzy.util.param
import timber.log.Timber
import kotlin.time.TimedValue
import kotlin.time.measureTimedValue

class LibrarySyncWorker(
    appContext: Context,
    params: WorkerParameters,
    private val userLibraryRepository: UserLibraryRepository
) : CoroutineWorker(appContext, params) {

    companion object {
        const val WORK_NAME = "io.libzy.work.LibrarySyncWorker"

        // the parameter key for this worker's input data, representing whether this is the first-time library scan,
        // which is initiated when the user first connects their spotify account
        const val IS_INITIAL_SCAN = "is_initial_scan"
    }

    private val isInitialScan by lazy { inputData.getBoolean(IS_INITIAL_SCAN, false) }
    private val sharedPrefs by lazy {
        applicationContext.getSharedPreferences(
            applicationContext.getString(R.string.spotify_prefs_name),
            Context.MODE_PRIVATE
        )
    }

    override suspend fun doWork(): Result {
        beforeLibrarySync()

        val numAlbumsSynced = try {
            measureTimedValue {
                userLibraryRepository.syncLibraryData()
            }
        } catch (e: SpotifyException.BadRequestException) {
            e.statusCode.let { statusCode ->
                // TODO: find a better way to always catch all server errors (may have to forgo the Spotify API wrapper library)
                return if (!isInitialScan && isServerError(statusCode)) {
                    Timber.e(e, "Failed to sync Spotify library data due to a server error. Retrying...")
                    Firebase.analytics.logEvent(RETRY_LIBRARY_SYNC) {
                        param(Analytics.EventProperties.IS_INITIAL_SCAN, isInitialScan)
                    }
                    Result.retry()
                } else fail(e)
            }
        } catch (e: SpotifyException) {
            return fail(e)
        } catch (e: SpotifyAuthException) {
            return fail(e)
        }

        afterLibrarySync(numAlbumsSynced)
        return Result.success()
    }

    private suspend fun beforeLibrarySync() {
        Timber.i("Initiating Spotify library data sync...")

        if (isInitialScan) {
            setForeground(createInitialScanForegroundInfo())
            sharedPrefs.edit {
                putBoolean(applicationContext.getString(R.string.spotify_initial_scan_in_progress_key), true)
            }
        }
    }

    private fun afterLibrarySync(numAlbumsSynced: TimedValue<Int>) {
        if (isInitialScan) {
            sharedPrefs.edit {
                putBoolean(applicationContext.getString(R.string.spotify_connected_key), true)
                putBoolean(applicationContext.getString(R.string.spotify_initial_scan_in_progress_key), false)
            }
            notifyLibraryScanEnded(
                notificationTitleResId = R.string.initial_library_scan_succeeded_notification_title,
                notificationTextResId = R.string.initial_library_scan_succeeded_notification_text,
                tapDestinationResId = R.id.queryFragment
            )
        }
        Timber.i("Successfully synced Spotify library data")

        Firebase.analytics.logEvent(SYNC_LIBRARY_DATA) {
            param(SUCCESS, true)
            param(Analytics.EventProperties.IS_INITIAL_SCAN, isInitialScan)
            param(NUM_ALBUMS_SYNCED, numAlbumsSynced.value)
            param(LIBRARY_SYNC_TIME, numAlbumsSynced.duration.inSeconds)
        }
    }

    private fun isServerError(statusCode: Int?) = statusCode != null && statusCode >= 500 && statusCode < 600

    private fun fail(exception: Exception): Result {
        Timber.e(exception, "Failed to sync Spotify library data")
        Firebase.analytics.logEvent(SYNC_LIBRARY_DATA) {
            param(SUCCESS, false)
            param(Analytics.EventProperties.IS_INITIAL_SCAN, isInitialScan)
        }
        if (isInitialScan) {
            sharedPrefs.edit {
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

    private fun notifyLibraryScanEnded(
        notificationTitleResId: Int,
        notificationTextResId: Int,
        tapDestinationResId: Int
    ) {
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
                LibrarySyncWorker::class.java.name ->
                    LibrarySyncWorker(appContext, workerParameters, userLibraryRepository)
                else -> null
            }
        }
    }
}
