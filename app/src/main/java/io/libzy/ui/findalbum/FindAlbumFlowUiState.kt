package io.libzy.ui.findalbum

import android.os.Parcelable
import io.libzy.domain.Query
import kotlinx.parcelize.Parcelize

@Parcelize
data class FindAlbumFlowUiState(val query: Query = Query()) : Parcelable
