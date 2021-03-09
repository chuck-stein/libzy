package io.libzy.util

import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData

class CombinedLiveData<A, B>(source1: LiveData<A>, source2: LiveData<B>) : MediatorLiveData<Pair<A, B>>() {

    init {
        addSource(source1) {
            it?.let { source1Val ->
                source2.value?.let { source2Val ->
                    value = source1Val to source2Val
                }
            }
        }
        addSource(source2) {
            it?.let { source2Val ->
                source1.value?.let { source1Val ->
                    value = source1Val to source2Val
                }
            }
        }
    }
}