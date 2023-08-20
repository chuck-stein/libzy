package io.libzy.di

import android.content.Context
import androidx.room.Room
import dagger.Module
import dagger.Provides
import io.libzy.R
import io.libzy.persistence.database.UserLibraryDatabase
import javax.inject.Singleton

@Module
class DatabaseModule {

    @Singleton
    @Provides
    fun provideDatabase(applicationContext: Context): UserLibraryDatabase =
        Room.databaseBuilder(
            applicationContext,
            UserLibraryDatabase::class.java,
            applicationContext.getString(R.string.database_file_name)
        ).build()

}
