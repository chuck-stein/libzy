package io.libzy.ui.common.component

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import io.libzy.R
import io.libzy.ui.theme.LibzyDimens.CIRCULAR_PROGRESS_INDICATOR_SIZE
import io.libzy.ui.theme.LibzyDimens.HORIZONTAL_INSET

// TODO: replace CircularProgressIndicator with LinearProgressIndicator(progress = X) where X is a float representing approximate progress,
//  based on number of albums synced (could also show some text indicating this) and number of other network operations completed
@Composable
fun LibrarySyncProgress() {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = HORIZONTAL_INSET.dp)
    ) {
        Text(
            stringResource(R.string.syncing_library_heading),
            style = MaterialTheme.typography.h3,
            modifier = Modifier.weight(0.45f).padding(top = 64.dp)
        )
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(0.55f)) {
            CircularProgressIndicator(Modifier.size(CIRCULAR_PROGRESS_INDICATOR_SIZE.dp))
            Spacer(Modifier.height(36.dp))
            Text(stringResource(R.string.syncing_library_subheading), style = MaterialTheme.typography.h6)
        }
    }
}