package com.chuckstein.libzy.di

import android.content.Context
import com.chuckstein.libzy.common.LibzyApplication
import com.chuckstein.libzy.di.viewmodel.ViewModelModule
import com.chuckstein.libzy.view.MainActivity
import com.chuckstein.libzy.view.connect.ConnectSpotifyFragment
import com.chuckstein.libzy.view.query.QueryFragment
import com.chuckstein.libzy.view.results.ResultsFragment
import dagger.BindsInstance
import dagger.Component
import javax.inject.Singleton

@Singleton
@Component(
    modules = [
        ViewModelModule::class,
        DatabaseModule::class
    ]
)
interface AppComponent {

    @Component.Factory
    interface Factory {
        fun create(@BindsInstance applicationContext: Context): AppComponent
    }

    // TODO: move these to their own subcomponents
    fun inject(libzyApplication: LibzyApplication)
    fun inject(mainActivity: MainActivity)
    fun inject(connectSpotifyFragment: ConnectSpotifyFragment)
    fun inject(queryFragment: QueryFragment)
    fun inject(resultsFragment: ResultsFragment)

}