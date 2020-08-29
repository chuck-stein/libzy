package com.chuckstein.libzy.di.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.chuckstein.libzy.view.browseresults.BrowseResultsViewModel
import com.chuckstein.libzy.view.query.QueryViewModel
import com.chuckstein.libzy.view.selectgenres.SelectGenresViewModel
import dagger.Binds
import dagger.Module
import dagger.multibindings.IntoMap

@Module
abstract class ViewModelModule {

    @Binds
    abstract fun bindViewModelFactory(factory: GenericViewModelFactory): ViewModelProvider.Factory

    @Binds
    @IntoMap
    @ViewModelKey(QueryViewModel::class)
    abstract fun bindQueryViewModel(viewModel: QueryViewModel): ViewModel

    @Binds
    @IntoMap
    @ViewModelKey(SelectGenresViewModel::class)
    abstract fun bindSelectGenresViewModel(viewModel: SelectGenresViewModel): ViewModel

    @Binds
    @IntoMap
    @ViewModelKey(BrowseResultsViewModel::class)
    abstract fun bindBrowseResultsViewModel(viewModel: BrowseResultsViewModel): ViewModel

}