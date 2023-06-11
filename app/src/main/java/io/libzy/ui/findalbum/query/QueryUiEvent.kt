package io.libzy.ui.findalbum.query

import io.libzy.domain.Query

sealed interface QueryUiEvent {

    sealed interface ForViewModel : QueryUiEvent
    data class SelectFamiliarity(val familiarity: Query.Familiarity?) : ForViewModel
    data class SelectInstrumentalness(val instrumental: Boolean?) : ForViewModel
    data class ChangeAcousticness(val acousticness: Float?) : ForViewModel
    data class ChangeValence(val valence: Float?) : ForViewModel
    data class ChangeEnergy(val energy: Float?) : ForViewModel
    data class ChangeDanceability(val danceability: Float?) : ForViewModel
    data class AddGenre(val genre: String) : ForViewModel
    data class RemoveGenre(val genre: String) : ForViewModel
    object StartSearchingGenres : ForViewModel
    data class StopSearchingGenres(val sendAnalytics: Boolean, val delayFirst: Boolean = false) : ForViewModel
    data class UpdateSearchQuery(val searchQuery: String) : ForViewModel
    data class SelectNoPreference(val queryParam: Query.Parameter) : ForViewModel
    object SendDismissKeyboardAnalytics : ForViewModel
    data class SendQuestionViewAnalytics(val queryParam: Query.Parameter) : ForViewModel
    object SendSubmitQueryAnalytics : ForViewModel

    sealed interface ForView : QueryUiEvent
    object OpenSettings : ForView
    object StartOver : ForView
    object SubmitQuery : ForView
}
