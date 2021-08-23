package io.libzy.ui.common.component

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.MaterialTheme
import androidx.compose.material.ScaffoldState
import androidx.compose.material.Snackbar
import androidx.compose.material.SnackbarHost
import androidx.compose.material.SnackbarHostState
import androidx.compose.material.Text
import androidx.compose.material.rememberScaffoldState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.google.accompanist.insets.LocalWindowInsets
import com.google.accompanist.insets.navigationBarsHeight
import com.google.accompanist.insets.navigationBarsPadding
import com.google.accompanist.insets.rememberInsetsPaddingValues
import com.google.accompanist.insets.ui.Scaffold
import com.google.accompanist.insets.ui.TopAppBar
import io.libzy.ui.BackgroundGradient
import io.libzy.ui.LibzyContent

/**
 * A custom [Scaffold] implementation which takes care of creating a common [TopAppBar] and [SnackbarHost],
 * adding padding for the system status bar and navigation bar so that they do not overlap screen content,
 * and making the background transparent so that content appears over the [BackgroundGradient] from [LibzyContent].
 *
 * @param title The title for the [TopAppBar].
 * @param navigationIcon The primary nav icon, to place in the top-left corner of the screen inside the [TopAppBar],
 *                       or null for no top-left nav icon.
 */
@Composable
fun LibzyScaffold(
    scaffoldState: ScaffoldState = rememberScaffoldState(),
    title: @Composable () -> Unit = {},
    navigationIcon: @Composable (() -> Unit)? = null,
    actionIcons: @Composable RowScope.() -> Unit = {},
    content: @Composable (BoxScope) -> Unit
) {
    Scaffold(
        scaffoldState = scaffoldState,
        topBar = {
            TopAppBar(
                title = title,
                navigationIcon = navigationIcon,
                actions = actionIcons,
                backgroundColor = Color.Transparent,
                elevation = 0.dp,
                contentPadding = rememberInsetsPaddingValues(
                    insets = LocalWindowInsets.current.statusBars,
                    applyBottom = false,
                )
            )
        },
        bottomBar = {
            Spacer(Modifier.navigationBarsHeight().fillMaxWidth())
        },
        snackbarHost = { LibzySnackbarHost(it) },
        backgroundColor = Color.Transparent
    ) { contentPadding ->
        Box(Modifier.padding(contentPadding).fillMaxSize(), content = content)
    }
}

@Composable
private fun LibzySnackbarHost(state: SnackbarHostState) {
    SnackbarHost(state, modifier = Modifier.navigationBarsPadding()) {
        Snackbar(
            backgroundColor = MaterialTheme.colors.surface,
            contentColor = MaterialTheme.colors.onSurface,
            modifier = Modifier.padding(12.dp)
        ) {
            Text(it.message, style = MaterialTheme.typography.body2.copy(textAlign = TextAlign.Start))
        }
    }
}
