package io.libzy.common

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.SharedPreferences
import android.os.Handler
import android.util.Log
import androidx.work.*
import com.amplitude.api.Amplitude
import io.libzy.BuildConfig
import io.libzy.R
import io.libzy.analytics.CrashlyticsTree
import io.libzy.config.ApiKeys
import io.libzy.di.AppComponent
import io.libzy.di.DaggerAppComponent
import io.libzy.repository.UserLibraryRepository
import io.libzy.work.LibrarySyncWorker
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import timber.log.Timber
import timber.log.Timber.DebugTree
import java.time.Duration
import javax.inject.Inject

class LibzyApplication : Application(), Configuration.Provider {

    companion object {
        private val LIBRARY_SYNC_INTERVAL = Duration.ofMinutes(15L) // time to wait between Spotify library syncs
    }

    val appComponent: AppComponent by lazy {
        DaggerAppComponent.factory().create(applicationContext)
    }

    @Inject
    lateinit var userLibraryRepository: UserLibraryRepository
    
    @Inject
    lateinit var apiKeys: ApiKeys

    private var sharedPrefsListener: SharedPreferences.OnSharedPreferenceChangeListener? = null

    private val applicationScope = CoroutineScope(Dispatchers.Default)

    override fun onCreate() {
        super.onCreate()
        appComponent.inject(this)
        createNotificationChannels()
        scheduleLibrarySync()
        initLogging()
        initAnalytics()
    }

    override fun getWorkManagerConfiguration(): Configuration {
        val workerFactory = DelegatingWorkerFactory()
        workerFactory.addFactory(LibrarySyncWorker.Factory(userLibraryRepository))

        return Configuration.Builder()
            .setMinimumLoggingLevel(Log.DEBUG)
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

        // Library Scan Progress channel
        createNotificationChannel(
            R.string.library_scan_progress_notification_channel_id,
            R.string.library_scan_progress_notification_channel_name,
            R.string.library_scan_progress_notification_channel_description,
            NotificationManager.IMPORTANCE_LOW
        )

        // Library Scan Update channel
        createNotificationChannel(
            R.string.library_scan_update_notification_channel_id,
            R.string.library_scan_update_notification_channel_name,
            R.string.library_scan_update_notification_channel_description,
            NotificationManager.IMPORTANCE_HIGH
        )
    }

    /**
     * Setup a WorkManager background job to sync Spotify library data every 15 minutes.
     *
     * If there is no Spotify account connected to Libzy, set a callback to start the job upon connection.
     */
    private fun scheduleLibrarySync() {

        fun enqueueWorkRequest() {
            applicationScope.launch {
                val workRequest = PeriodicWorkRequestBuilder<LibrarySyncWorker>(LIBRARY_SYNC_INTERVAL).build()

                WorkManager.getInstance(this@LibzyApplication).enqueueUniquePeriodicWork(
                    LibrarySyncWorker.WORK_NAME,
                    ExistingPeriodicWorkPolicy.KEEP,
                    workRequest
                )
            }
        }

        val sharedPrefs = getSharedPreferences(getString(R.string.spotify_prefs_name), Context.MODE_PRIVATE)
        val spotifyConnectedKey = getString(R.string.spotify_connected_key)
        val spotifyConnected = sharedPrefs.getBoolean(spotifyConnectedKey, false)

        if (spotifyConnected) enqueueWorkRequest()
        else {
            sharedPrefsListener = SharedPreferences.OnSharedPreferenceChangeListener { prefs, key ->
                if (key == spotifyConnectedKey && prefs.getBoolean(key, false)) {

                    // Spotify was just connected, meaning the first library scan just completed,
                    // so schedule the next one in 15 minutes, which will recur every subsequent 15 minutes
                    Handler().postDelayed({
                        enqueueWorkRequest()
                    }, 5000)

                    prefs.unregisterOnSharedPreferenceChangeListener(sharedPrefsListener)
                    sharedPrefsListener = null
                }
            }
            sharedPrefs.registerOnSharedPreferenceChangeListener(sharedPrefsListener)
        }
    }
    
    private fun initLogging() {
        if (BuildConfig.DEBUG) {
            Timber.plant(DebugTree())
        } else {
            Timber.plant(CrashlyticsTree())
        }
    }
    
    private fun initAnalytics() {
        Amplitude.getInstance()
            .initialize(this, apiKeys.amplitudeApiKey)
            .enableForegroundTracking(this)
    }
}
