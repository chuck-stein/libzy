package com.chuckstein.libzy.di

import android.content.Context
import androidx.room.Room
import com.chuckstein.libzy.database.UserLibraryDatabase
import dagger.Module
import dagger.Provides
import javax.inject.Singleton

@Module
class DatabaseModule {

    @Singleton
    @Provides
    fun provideDatabase(applicationContext: Context): UserLibraryDatabase =
        Room.databaseBuilder(
            applicationContext,
            UserLibraryDatabase::class.java,
            "spotify_library"
        )
            .fallbackToDestructiveMigration() // TODO: create an actual migration strategy
            .build()

}