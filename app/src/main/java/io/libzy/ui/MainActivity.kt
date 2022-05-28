package io.libzy.ui

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.core.view.WindowCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.flowWithLifecycle
import androidx.lifecycle.lifecycleScope
import com.spotify.sdk.android.auth.AuthorizationClient
import io.libzy.LibzyApplication
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * This is Libzy's one central Activity, as it is a single activity application.
 *
 * Sets the UI content to [LibzyNavGraph], which encapsulates the app's various composable screens
 * as well as navigation between them.
 *
 * Interacts with the Spotify SDK's auth library by opening an authorization Activity when requested,
 * and saving the response via [SessionViewModel].
 */
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var viewModelFactory: ViewModelProvider.Factory
    private val viewModel by viewModels<SessionViewModel> { viewModelFactory }


    @OptIn(ExperimentalAnimationApi::class, ExperimentalFoundationApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        (applicationContext as LibzyApplication).appComponent.inject(this)
        super.onCreate(savedInstanceState)
        collectSpotifyAuthRequests()
        WindowCompat.setDecorFitsSystemWindows(window, false)

        setContent {
            LibzyContent {
                LibzyNavGraph(viewModelFactory, viewModel::isSpotifyConnected, ::finish)
            }
        }
    }

    private fun collectSpotifyAuthRequests() {
        lifecycleScope.launch {
            viewModel.uiEvents.flowWithLifecycle(lifecycle, Lifecycle.State.CREATED).collect {
                if (it is SessionUiEvent.SpotifyAuthRequest) {
                    AuthorizationClient.openLoginActivity(this@MainActivity, SPOTIFY_AUTH_REQUEST_CODE, it.request)
                }
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, intent: Intent?) {
        super.onActivityResult(requestCode, resultCode, intent)

        if (requestCode == SPOTIFY_AUTH_REQUEST_CODE) {
            viewModel.handleSpotifyAuthResponse(AuthorizationClient.getResponse(resultCode, intent))
        }
    }

    companion object {
        private const val SPOTIFY_AUTH_REQUEST_CODE = 1104
    }
}
