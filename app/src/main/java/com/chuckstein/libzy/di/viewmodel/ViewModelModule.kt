package com.chuckstein.libzy.di.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.chuckstein.libzy.view.connect.ConnectSpotifyViewModel
import com.chuckstein.libzy.view.query.QueryResultsViewModel
import dagger.Binds
import dagger.Module
import dagger.multibindings.IntoMap

@Module
abstract class ViewModelModule {

    @Binds
    abstract fun bindViewModelFactory(factory: GenericViewModelFactory): ViewModelProvider.Factory

    @Binds
    @IntoMap
    @ViewModelKey(ConnectSpotifyViewModel::class)
    abstract fun bindConnectSpotifyViewModel(viewModel: ConnectSpotifyViewModel): ViewModel

    @Binds
    @IntoMap
    @ViewModelKey(QueryResultsViewModel::class)
    abstract fun bindQueryResultsViewModel(viewModel: QueryResultsViewModel): ViewModel

}