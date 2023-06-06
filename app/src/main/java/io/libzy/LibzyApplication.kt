package io.libzy

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.work.Configuration
import androidx.work.DelegatingWorkerFactory
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import io.libzy.analytics.AnalyticsDispatcher
import io.libzy.analytics.CrashlyticsTree
import io.libzy.di.AndroidModule
import io.libzy.di.AppComponent
import io.libzy.di.DaggerAppComponent
import io.libzy.repository.PreferencesRepository
import io.libzy.repository.UserLibraryRepository
import io.libzy.work.LibrarySyncWorker
import io.libzy.work.LibrarySyncWorker.Companion.LIBRARY_SYNC_INTERVAL
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import timber.log.Timber
import timber.log.Timber.DebugTree
import javax.inject.Inject

class LibzyApplication : Application(), Configuration.Provider {

    val appComponent: AppComponent by lazy {
        DaggerAppComponent.factory().create(applicationContext)
    }

    @Inject
    lateinit var userLibraryRepository: UserLibraryRepository

    @Inject
    lateinit var preferencesRepository: PreferencesRepository

    @Inject
    lateinit var analyticsDispatcher: AnalyticsDispatcher

    @Inject
    lateinit var workManager: WorkManager

    @Inject
    lateinit var sharedPrefs: SharedPreferences

    @Inject
    lateinit var applicationScope: CoroutineScope

    override fun onCreate() {
        super.onCreate()
        appComponent.inject(this)
        createNotificationChannels()
        scheduleLibrarySync()
        initLogging()
        analyticsDispatcher.initialize(this, BuildConfig.AMPLITUDE_API_KEY)
    }

    override fun getWorkManagerConfiguration(): Configuration {
        val workerFactory = DelegatingWorkerFactory()
        // can't use the injected sharedPrefs because it will not have been injected yet when this method is called
        val sharedPrefs = AndroidModule().provideSharedPrefs(this)
        workerFactory.addFactory(
            LibrarySyncWorker.Factory(userLibraryRepository, preferencesRepository, analyticsDispatcher, sharedPrefs)
        )

        return Configuration.Builder()
            .setMinimumLoggingLevel(Log.VERBOSE)
            .setWorkerFactory(workerFactory)
            .build()
    }

    /**
     * Create all notification channels used by Libzy
     */
    private fun createNotificationChannels() {
        val notificationManager =
            applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        fun createNotificationChannel(idResId: Int, nameResId: Int, descriptionResId: Int, importance: Int) {
            val id = applicationContext.getString(idResId)
            val name = applicationContext.getString(nameResId)
            val description = applicationContext.getString(descriptionResId)

            val channel = NotificationChannel(id, name, importance).apply {
                this.description = description
            }
            notificationManager.createNotificationChannel(channel)
        }

        // Library Sync Progress channel
        createNotificationChannel(
            R.string.library_sync_progress_notification_channel_id,
            R.string.library_sync_progress_notification_channel_name,
            R.string.library_sync_progress_notification_channel_description,
            NotificationManager.IMPORTANCE_LOW
        )

        // Library Sync Update channel
        createNotificationChannel(
            R.string.library_sync_update_notification_channel_id,
            R.string.library_sync_update_notification_channel_name,
            R.string.library_sync_update_notification_channel_description,
            NotificationManager.IMPORTANCE_HIGH
        )
    }

    /**
     * Setup a WorkManager background job to sync Spotify library data every 15 minutes.
     *
     * If there is no Spotify account connected to Libzy, do not launch the job.
     */
    private fun scheduleLibrarySync() {
        applicationScope.launch {
            if (preferencesRepository.spotifyConnectedState.value) {
                val workRequest =
                    PeriodicWorkRequestBuilder<LibrarySyncWorker>(LIBRARY_SYNC_INTERVAL)
                        .build()

                workManager.enqueueUniquePeriodicWork(
                    LibrarySyncWorker.WORK_NAME,
                    ExistingPeriodicWorkPolicy.KEEP,
                    workRequest
                )
            }
        }
    }
    
    private fun initLogging() {
        if (BuildConfig.DEBUG) Timber.plant(DebugTree())
        Timber.plant(CrashlyticsTree())
    }
}
