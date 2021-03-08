package io.libzy.di

import android.content.Context
import dagger.Module
import dagger.Provides
import io.libzy.common.ApiKeys
import io.libzy.common.DevApiKeys
import javax.inject.Singleton

@Module
class BuildVariantModule {

    @Singleton
    @Provides
    fun provideApiKeys(applicationContext: Context): ApiKeys = DevApiKeys(applicationContext)
    
}
