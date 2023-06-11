package io.libzy.ui.common.util

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.pager.PagerState

@OptIn(ExperimentalFoundationApi::class)
suspend fun PagerState.goToNextPage() = animateScrollToPage((currentPage + 1).coerceAtMost(pageCount - 1))

@OptIn(ExperimentalFoundationApi::class)
suspend fun PagerState.goToPreviousPage() = animateScrollToPage((currentPage - 1).coerceAtLeast(0))