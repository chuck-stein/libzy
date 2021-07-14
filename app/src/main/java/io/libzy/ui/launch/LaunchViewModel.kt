package io.libzy.ui.launch

import android.content.Context
import androidx.lifecycle.viewModelScope
import io.libzy.persistence.prefs.SharedPrefKeys
import io.libzy.persistence.prefs.getSharedPrefs
import io.libzy.ui.common.StateOnlyViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * A [StateOnlyViewModel] providing the state for [LaunchScreen] that determines which "home screen" the app
 * should launch to. This ViewModel produces persistent state rather than instantaneous navigation events so that
 * the loading time is reduced (collecting events has a higher overhead than reading state), and also because it is
 * more fitting for the ViewModel's purpose. Once we determine what the home screen should be, that should be considered
 * a persistent state, not a one-time event, because the navigation is not user-initiated, and will always be the same
 * if [LaunchScreen] is ever recreated from the back stack, making it useful to already have the correct state set in
 * the ViewModel rather than having to wait for another navigation event.
 */
class LaunchViewModel @Inject constructor(appContext: Context) : StateOnlyViewModel<LaunchUiState>() {

    override val initialUiState: LaunchUiState = LaunchUiState.LOADING

    init {
        viewModelScope.launch {
            val spotifyConnected = appContext.getSharedPrefs().getBoolean(SharedPrefKeys.SPOTIFY_CONNECTED, false)

            updateUiState {
                if (spotifyConnected) {
                    LaunchUiState.SPOTIFY_CONNECTED
                } else {
                    LaunchUiState.NEEDS_SPOTIFY_CONNECTION
                }
            }
        }
    }
}
