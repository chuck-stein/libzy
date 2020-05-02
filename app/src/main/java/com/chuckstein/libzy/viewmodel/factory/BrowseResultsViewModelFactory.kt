package com.chuckstein.libzy.viewmodel.factory

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.chuckstein.libzy.viewmodel.BrowseResultsViewModel
import java.lang.IllegalArgumentException

class BrowseResultsViewModelFactory(private val selectedGenres: Array<String>) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST") // cast is checked, using isAssignableFrom
    override fun <T : ViewModel?> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(BrowseResultsViewModel::class.java)) {
            return BrowseResultsViewModel(selectedGenres) as T
        }
        throw IllegalArgumentException(
            "Cannot assign a ${BrowseResultsViewModelFactory::class.java.simpleName} " +
                    "to the given type ${modelClass.simpleName}"
        )
    }

}