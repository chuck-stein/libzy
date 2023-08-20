package io.libzy.ui.common.component

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsBottomHeight
import androidx.compose.foundation.layout.windowInsetsTopHeight
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.FabPosition
import androidx.compose.material.MaterialTheme
import androidx.compose.material.ScaffoldState
import androidx.compose.material.Snackbar
import androidx.compose.material.SnackbarHost
import androidx.compose.material.SnackbarHostState
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.material.rememberScaffoldState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.google.accompanist.insets.ui.Scaffold
import com.google.accompanist.insets.ui.TopAppBar
import io.libzy.ui.LibzyContent
import io.libzy.ui.theme.LibzyColors

/**
 * A custom [Scaffold] implementation which takes care of creating a common [TopAppBar] and [SnackbarHost],
 * adding padding for the system status bar and navigation bar so that they do not overlap screen content,
 * and making the background transparent so that content appears over the [Shimmer] from [LibzyContent].
 */
@Composable
fun LibzyScaffold(
    scaffoldState: ScaffoldState = rememberScaffoldState(),
    showTopBar: Boolean = true,
    title: @Composable () -> Unit = {},
    navigationIcon: @Composable (() -> Unit)? = null,
    actionIcons: @Composable RowScope.() -> Unit = {},
    floatingActionButton: @Composable () -> Unit = {},
    floatingActionButtonPosition: FabPosition = FabPosition.Center,
    content: @Composable (BoxScope) -> Unit
) {
    Scaffold(
        scaffoldState = scaffoldState,
        topBar = {
            if (showTopBar) {
                TopAppBar(
                    title = title,
                    navigationIcon = navigationIcon,
                    actions = actionIcons,
                    backgroundColor = Color.Transparent,
                    elevation = 0.dp,
                    contentPadding = WindowInsets.statusBars.asPaddingValues()
                )
            } else {
                Spacer(Modifier.windowInsetsTopHeight(WindowInsets.statusBars).fillMaxWidth())
            }
        },
        bottomBar = {
            Spacer(Modifier.windowInsetsBottomHeight(WindowInsets.navigationBars).fillMaxWidth())
        },
        floatingActionButton = floatingActionButton,
        floatingActionButtonPosition = floatingActionButtonPosition,
        snackbarHost = { LibzySnackbarHost(it) },
        backgroundColor = Color.Transparent
    ) { contentPadding ->
        Box(Modifier.padding(contentPadding).fillMaxSize(), content = content)
    }
}

@Composable
private fun LibzySnackbarHost(state: SnackbarHostState, modifier: Modifier = Modifier) {
    SnackbarHost(hostState = state, modifier = modifier.navigationBarsPadding()) { snackbarData ->
        Snackbar(
            modifier = Modifier.padding(horizontal = 12.dp),
            backgroundColor = LibzyColors.OffWhite,
            contentColor = Color.Black,
            action = snackbarData.actionLabel?.let {
                {
                    TextButton(
                        colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colors.primary),
                        onClick = { snackbarData.performAction() },
                        content = { Text(it) }
                    )
                }
            }
        ) {
            Text(snackbarData.message, style = MaterialTheme.typography.body2.copy(textAlign = TextAlign.Start))
        }
    }
}
