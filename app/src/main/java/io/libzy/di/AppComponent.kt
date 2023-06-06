package io.libzy.di

import android.content.Context
import dagger.BindsInstance
import dagger.Component
import io.libzy.LibzyApplication
import io.libzy.di.viewmodel.ViewModelModule
import io.libzy.ui.MainActivity
import javax.inject.Singleton

@Singleton
@Component(
    modules = [
        AndroidModule::class,
        ViewModelModule::class,
        DatabaseModule::class,
        CoroutineModule::class
    ]
)
interface AppComponent {

    @Component.Factory
    interface Factory {
        fun create(@BindsInstance applicationContext: Context): AppComponent
    }

    fun inject(libzyApplication: LibzyApplication)
    fun inject(mainActivity: MainActivity)
}
