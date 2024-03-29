package io.libzy.ui

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContract
import androidx.activity.viewModels
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.flowWithLifecycle
import androidx.lifecycle.lifecycleScope
import com.spotify.sdk.android.auth.AuthorizationClient
import com.spotify.sdk.android.auth.AuthorizationRequest
import com.spotify.sdk.android.auth.AuthorizationResponse
import io.libzy.LibzyApplication
import io.libzy.R
import io.libzy.analytics.AnalyticsDispatcher
import io.libzy.analytics.LocalAnalytics
import io.libzy.ui.common.component.LoadedContent
import io.libzy.util.toTextResource
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

    @Inject
    lateinit var analytics: AnalyticsDispatcher

    private val viewModel by viewModels<SessionViewModel> { viewModelFactory }

    private val requestSpotifyAuth = registerForActivityResult(RequestSpotifyAuth()) { response: AuthorizationResponse ->
        viewModel.handleSpotifyAuthResponse(response)
    }

    @OptIn(ExperimentalAnimationApi::class, ExperimentalFoundationApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        (applicationContext as LibzyApplication).appComponent.inject(this)
        super.onCreate(savedInstanceState)
        lifecycle.addObserver(viewModel)
        collectSpotifyAuthRequests()

        setContent {
            LibzyContent {
                CompositionLocalProvider(LocalAnalytics provides analytics) {
                    val uiState by viewModel.uiStateFlow.collectAsState()
                    LoadedContent(uiState.loading, progressIndicatorCaption = R.string.connecting_to_spotify.toTextResource()) {
                        LibzyNavGraph(uiState, viewModelFactory, exitApp = ::finish)
                    }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel.onNewSpotifyAuthAvailable()
    }

    override fun onDestroy() {
        super.onDestroy()
        lifecycle.removeObserver(viewModel)
    }

    private fun collectSpotifyAuthRequests() {
        lifecycleScope.launch {
            viewModel.uiEvents.flowWithLifecycle(lifecycle, Lifecycle.State.CREATED).collect {
                if (it is SessionUiEvent.SpotifyAuthRequest) {
                    requestSpotifyAuth.launch(it.request)
                }
            }
        }
    }

    inner class RequestSpotifyAuth : ActivityResultContract<AuthorizationRequest, AuthorizationResponse>() {

        override fun createIntent(context: Context, input: AuthorizationRequest): Intent =
            AuthorizationClient.createLoginActivityIntent(this@MainActivity, input)

        override fun parseResult(resultCode: Int, intent: Intent?): AuthorizationResponse =
            AuthorizationClient.getResponse(resultCode, intent)
    }
}
