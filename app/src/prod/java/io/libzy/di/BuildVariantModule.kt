package io.libzy.di

import android.content.Context
import dagger.Module
import dagger.Provides
import io.libzy.config.ApiKeys
import io.libzy.config.ProdApiKeys
import javax.inject.Singleton

@Module
class BuildVariantModule {

    @Singleton
    @Provides
    fun provideApiKeys(applicationContext: Context): ApiKeys = ProdApiKeys(applicationContext)
    
}
