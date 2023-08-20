package io.libzy.ui.library

sealed interface ExpandLibraryUiEvent {

    sealed interface ForViewModel : ExpandLibraryUiEvent
    object Initialize : ForViewModel
    object Refresh : ForViewModel
    object RecommendAlbums : ForViewModel
    data class SaveAlbum(val id: String) : ForViewModel
    data class RemoveAlbum(val id: String) : ForViewModel
    object AwaitLibrarySync: ForViewModel
    object DismissError : ForViewModel

    sealed interface ForView : ExpandLibraryUiEvent
    object NavToQueryScreen : ForView
    object ExitApp : ForView
}