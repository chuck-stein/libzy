package io.libzy.di.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import dagger.Binds
import dagger.Module
import dagger.multibindings.IntoMap
import io.libzy.ui.MainViewModel
import io.libzy.ui.connect.ConnectSpotifyViewModel
import io.libzy.ui.launch.LaunchViewModel
import io.libzy.ui.query.QueryViewModel
import io.libzy.ui.results.ResultsViewModel

@Module
abstract class ViewModelModule {

    @Binds
    abstract fun bindViewModelFactory(factory: GenericViewModelFactory): ViewModelProvider.Factory

    @Binds
    @IntoMap
    @ViewModelKey(MainViewModel::class)
    abstract fun bindMainViewModel(viewModel: MainViewModel): ViewModel

    @Binds
    @IntoMap
    @ViewModelKey(ConnectSpotifyViewModel::class)
    abstract fun bindConnectSpotifyViewModel(viewModel: ConnectSpotifyViewModel): ViewModel

    @Binds
    @IntoMap
    @ViewModelKey(LaunchViewModel::class)
    abstract fun bindLaunchViewModel(viewModel: LaunchViewModel): ViewModel

    @Binds
    @IntoMap
    @ViewModelKey(QueryViewModel::class)
    abstract fun bindQueryViewModel(viewModel: QueryViewModel): ViewModel

    @Binds
    @IntoMap
    @ViewModelKey(ResultsViewModel::class)
    abstract fun bindResultsViewModel(viewModel: ResultsViewModel): ViewModel
}
