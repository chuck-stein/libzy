package io.libzy.di.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import dagger.Binds
import dagger.Module
import dagger.multibindings.IntoMap
import io.libzy.ui.SessionViewModel
import io.libzy.ui.connect.ConnectSpotifyViewModel
import io.libzy.ui.findalbum.FindAlbumFlowViewModel
import io.libzy.ui.findalbum.query.QueryViewModel
import io.libzy.ui.findalbum.results.ResultsViewModel
import io.libzy.ui.library.ExpandLibraryViewModel
import io.libzy.ui.onboarding.OnboardingViewModel
import io.libzy.ui.settings.SettingsViewModel

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

    @Binds
    @IntoMap
    @ViewModelKey(SettingsViewModel::class)
    abstract fun bindSettingsViewModel(viewModel: SettingsViewModel): ViewModel

    @Binds
    @IntoMap
    @ViewModelKey(OnboardingViewModel::class)
    abstract fun bindOnboardingViewModel(viewModel: OnboardingViewModel): ViewModel

    @Binds
    @IntoMap
    @ViewModelKey(ExpandLibraryViewModel::class)
    abstract fun bindExpandLibraryViewModel(viewModel: ExpandLibraryViewModel): ViewModel
}
