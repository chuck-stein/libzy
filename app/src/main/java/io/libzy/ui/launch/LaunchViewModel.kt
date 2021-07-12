package io.libzy.ui.launch

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.libzy.R
import io.libzy.database.SharedPrefKeys
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

// TODO: make this a ScreenViewModel
class LaunchViewModel @Inject constructor(appContext: Context) : ViewModel() {

    private val _uiState = MutableStateFlow(LaunchUiState.LOADING)
    val uiState = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            val spotifyPrefs =
                appContext.getSharedPreferences(appContext.getString(R.string.spotify_prefs_name), Context.MODE_PRIVATE)
            val spotifyConnected = spotifyPrefs.getBoolean(SharedPrefKeys.SPOTIFY_CONNECTED, false)

            _uiState.value = if (spotifyConnected) {
                LaunchUiState.SPOTIFY_CONNECTED
            } else {
                LaunchUiState.NEEDS_SPOTIFY_CONNECTION
            }
        }
    }
}
