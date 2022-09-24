package io.libzy.di

import android.content.Context
import androidx.work.WorkManager
import dagger.Module
import dagger.Provides
import io.libzy.persistence.prefs.getSharedPrefs

@Module
class AndroidModule {

    @Provides
    fun provideWorkManager(appContext: Context) = WorkManager.getInstance(appContext)

    @Provides
    fun provideSharedPrefs(appContext: Context) = appContext.getSharedPrefs()
}