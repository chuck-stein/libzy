package io.libzy.di

import dagger.Module
import dagger.Provides
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import javax.inject.Singleton

@Module
class CoroutineModule {

    @Singleton
    @Provides
    fun provideCoroutineScope() = CoroutineScope(SupervisorJob() + Dispatchers.Default)
}