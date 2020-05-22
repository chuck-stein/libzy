package com.chuckstein.libzy.di

import android.content.Context
import com.chuckstein.libzy.di.viewmodel.ViewModelModule
import com.chuckstein.libzy.view.MainActivity
import com.chuckstein.libzy.view.browseresults.BrowseResultsFragment
import com.chuckstein.libzy.view.connectspotify.ConnectSpotifyFragment
import com.chuckstein.libzy.view.selectgenres.SelectGenresFragment
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
    fun inject(mainActivity: MainActivity)
    fun inject(connectSpotifyFragment: ConnectSpotifyFragment)
    fun inject(selectGenresFragment: SelectGenresFragment)
    fun inject(browseResultsFragment: BrowseResultsFragment)

}