package io.libzy.di.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import dagger.Binds
import dagger.Module
import dagger.multibindings.IntoMap
import io.libzy.ui.SessionViewModel
import io.libzy.ui.connect.ConnectSpotifyViewModel
import io.libzy.ui.findalbum.FindAlbumFlowViewModel
import io.libzy.ui.query.QueryViewModel
import io.libzy.ui.results.ResultsViewModel

@Module
abstract class ViewModelModule {

    @Binds
    abstract fun bindViewModelFactory(factory: GenericViewModelFactory): ViewModelProvider.Factory

    @Binds
    @IntoMap
    @ViewModelKey(SessionViewModel::class)
    abstract fun bindSessionViewModel(viewModel: SessionViewModel): ViewModel

    @Binds
    @IntoMap
    @ViewModelKey(ConnectSpotifyViewModel::class)
    abstract fun bindConnectSpotifyViewModel(viewModel: ConnectSpotifyViewModel): ViewModel

    @Binds
    @IntoMap
    @ViewModelKey(FindAlbumFlowViewModel::class)
    abstract fun bindFindAlbumFlowViewModel(viewModel: FindAlbumFlowViewModel): ViewModel

    @Binds
    @IntoMap
    @ViewModelKey(QueryViewModel::class)
    abstract fun bindQueryViewModel(viewModel: QueryViewModel): ViewModel

    @Binds
    @IntoMap
    @ViewModelKey(ResultsViewModel::class)
    abstract fun bindResultsViewModel(viewModel: ResultsViewModel): ViewModel
}
