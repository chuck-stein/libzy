package io.libzy.ui.common.util

import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.remember

/**
 * Safely access this grid's `layoutInfo` from within a Composable.
 *
 * (Calling `layoutInfo` directly causes performance issues because `layoutInfo` updates continuously while scrolling,
 * resulting in lots of recompositions of the calling composable.)
 */
val LazyGridState.rememberedLayoutInfo
    @Composable
    get() = remember { derivedStateOf { layoutInfo } }.value

/**
 * Safely access this grid's [numItemsSeen] from within a Composable.
 *
 * (Calling `numItemsSeen` directly causes performance issues because `numItemsSeen` updates continuously while scrolling,
 * resulting in lots of recompositions of the calling composable.)
 */
val LazyGridState.rememberedNumItemsSeen
    @Composable
    get() = remember { derivedStateOf { numItemsSeen } }.value

/**
 * The number of items currently visible in the grid.
 *
 * Accessing this value from a Composable may cause performance issues because it updates continuously while scrolling,
 * resulting in lots of recompositions of the calling composable.
 */
val LazyGridState.numVisibleItems
    get() = layoutInfo.visibleItemsInfo.size

/**
 * The number of items in the grid that are positioned before the current scroll position, or are currently visible.
 * Assuming the user has manually scrolled to the current scroll position, this will be the number of items they have seen.
 *
 * Accessing this value from a Composable may cause performance issues because it updates continuously while scrolling,
 * resulting in lots of recompositions of the calling composable. Use [rememberedNumItemsSeen] for this purpose instead.
 */
val LazyGridState.numItemsSeen
    get() = firstVisibleItemIndex + numVisibleItems