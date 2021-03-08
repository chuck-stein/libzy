package io.libzy.di

import android.content.Context
import dagger.BindsInstance
import dagger.Component
import io.libzy.LibzyApplication
import io.libzy.di.viewmodel.ViewModelModule
import io.libzy.view.MainActivity
import io.libzy.view.connect.ConnectSpotifyFragment
import io.libzy.view.query.QueryFragment
import io.libzy.view.query.ResultsFragment
import javax.inject.Singleton

@Singleton
@Component(
    modules = [
        ViewModelModule::class,
        DatabaseModule::class,
        BuildVariantModule::class
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
