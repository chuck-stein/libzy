package io.libzy.ui.common.component

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyGridScope
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import io.libzy.persistence.database.tuple.LibraryAlbum
import io.libzy.ui.theme.LibzyColors
import io.libzy.ui.theme.LibzyDimens
import io.libzy.util.TextResource
import io.libzy.util.resolveText
import io.libzy.util.toTextResource

data class AlbumUiState(
    val title: TextResource,
    val artists: TextResource,
    val spotifyId: String,
    val spotifyUri: String? = null,
    val artworkUrl: String? = null,
    val clickEvent: Any? = null,
    val icon: ImageVector? = null,
    val iconContentDescription: TextResource? = null,
    val isHighlighted: Boolean = false,
    val placeholderShimmer: Boolean = false
)

fun LibraryAlbum.toUiState(clickEvent: Any? = null) =
    AlbumUiState(title.toTextResource(), artists.toTextResource(), spotifyId, spotifyUri, artworkUrl, clickEvent)

@Composable
fun AlbumGrid(
    albums: List<AlbumUiState>,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(),
    onUiEvent: (Any) -> Unit,
    state: LazyGridState = rememberLazyGridState(),
    userScrollEnabled: Boolean = true,
    extraContent: LazyGridScope.() -> Unit = {}
) {
    val density = LocalDensity.current

    LazyVerticalGrid(
        columns = GridCells.Adaptive(minSize = DEFAULT_ALBUM_LIST_ITEM_WIDTH.dp),
        modifier = modifier.padding(horizontal = (LibzyDimens.HORIZONTAL_INSET - ALBUM_LIST_ITEM_PADDING).dp),
        state = state,
        userScrollEnabled = userScrollEnabled,
        contentPadding = contentPadding
    ) {
        itemsIndexed(albums, key = { _, album -> album.spotifyId }) { index, album ->
            var height by remember { mutableStateOf(Dp.Unspecified) }

            AlbumListItem(
                album = album,
                onAlbumClick = onUiEvent,
                modifier = Modifier
                    .sizeIn(minWidth = DEFAULT_ALBUM_LIST_ITEM_WIDTH.dp)
                    .height(height)
                    .onGloballyPositioned {
                        val row = state.layoutInfo.visibleItemsInfo.find { it.index == index }?.row
                        val itemsInRow = state.layoutInfo.visibleItemsInfo.filter { it.row == row }
                        val maxHeightInRow = itemsInRow.maxOfOrNull { it.size.height }
                        height = with(density) { maxHeightInRow?.toDp() } ?: Dp.Unspecified
                    }
            )
        }

        extraContent()
    }
}

@Composable
fun AlbumListItem(
    album: AlbumUiState,
    onAlbumClick: (Any) -> Unit = {},
    modifier: Modifier = Modifier,
    maxLinesPerLabel: Int = 3
) = with(album) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .padding((ALBUM_LIST_ITEM_PADDING / 4).dp)
            .background(
                color = MaterialTheme.colors.secondary.copy(alpha = if (isHighlighted) 0.5f else 0f),
                shape = RoundedCornerShape(4.dp)
            )
            .clip(RoundedCornerShape(4.dp))
            .clickable(enabled = clickEvent != null) { clickEvent?.let(onAlbumClick) }
            .padding((ALBUM_LIST_ITEM_PADDING * 3 / 4).dp)
    ) {
        AlbumArtwork(artworkUrl, Modifier.fillMaxWidth(), placeholderShimmer)
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
            Column(Modifier.weight(1f)) {
                val textAlign = if (icon == null) TextAlign.Center else TextAlign.Start
                Text(
                    text = title.resolveText(),
                    style = MaterialTheme.typography.body2,
                    textAlign = textAlign,
                    maxLines = maxLinesPerLabel,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier
                        .padding(top = 6.dp)
                        .fillMaxWidth()
                )
                Text(
                    text = artists.resolveText(),
                    textAlign = textAlign,
                    style = MaterialTheme.typography.body2,
                    maxLines = maxLinesPerLabel,
                    overflow = TextOverflow.Ellipsis,
                    color = LibzyColors.Gray,
                    modifier = Modifier.fillMaxWidth()
                )
            }
            if (icon != null) {
                LibzyIcon(
                    imageVector = icon,
                    contentDescription = iconContentDescription?.resolveText(),
                    modifier = Modifier.padding(top = 4.dp, start = 4.dp)
                )
            }
        }
    }
}

const val ALBUM_LIST_ITEM_PADDING = 8
const val DEFAULT_ALBUM_LIST_ITEM_WIDTH = 160
