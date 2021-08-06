package io.libzy.ui.findalbum

import io.libzy.domain.Query
import io.libzy.ui.Destination
import io.libzy.ui.common.StateOnlyViewModel
import io.libzy.ui.query.QueryScreen
import io.libzy.ui.results.ResultsScreen
import javax.inject.Inject

/**
 * ViewModel for the "find album" flow, which includes entering query information and seeing the results for that query.
 *
 * The query must be accessible from both [QueryScreen] and [ResultsScreen], so this ViewModel should be scoped to
 * [Destination.FindAlbumFlow], a nested nav graph which encapsulates those two screens.
 */
class FindAlbumFlowViewModel @Inject constructor() : StateOnlyViewModel<FindAlbumFlowUiState>() {
    override val initialUiState = FindAlbumFlowUiState()

    fun setQuery(query: Query) {
        updateUiState {
            copy(query = query)
        }
    }
}
