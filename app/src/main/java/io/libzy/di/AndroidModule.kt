package io.libzy.di

import android.content.Context
import android.content.SharedPreferences
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore
import androidx.work.WorkManager
import dagger.Module
import dagger.Provides

@Module
class AndroidModule {

    @Provides
    fun provideWorkManager(appContext: Context): WorkManager  = WorkManager.getInstance(appContext)

    @Provides
    fun provideSharedPrefs(appContext: Context): SharedPreferences =
        appContext.getSharedPreferences("libzy_preferences", Context.MODE_PRIVATE)

    @Provides
    fun provideDataStore(appContext: Context): DataStore<Preferences> = appContext.dataStore
}

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "libzy_preferences")