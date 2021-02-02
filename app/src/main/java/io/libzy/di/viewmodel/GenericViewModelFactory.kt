package io.libzy.di.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import javax.inject.Inject
import javax.inject.Provider

class GenericViewModelFactory @Inject constructor(
    private val modelMap: @JvmSuppressWildcards Map<Class<out ViewModel>, Provider<ViewModel>>
) : ViewModelProvider.Factory {

    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        val modelProvider = modelMap[modelClass]
            ?: modelMap.asIterable().firstOrNull { modelClass.isAssignableFrom(it.key) }?.value
            ?: throw IllegalArgumentException("Unknown ViewModel class: $modelClass")
        return try {
            @Suppress("UNCHECKED_CAST")
            modelProvider.get() as T
        } catch (e: Exception) {
            throw RuntimeException(e)
        }
    }

}