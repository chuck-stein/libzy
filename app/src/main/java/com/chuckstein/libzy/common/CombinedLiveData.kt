package com.chuckstein.libzy.common

import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData

class CombinedLiveData<A, B>(source1: LiveData<A>, source2: LiveData<B>) : MediatorLiveData<Pair<A, B>>() {

    init {
        addSource(source1) {
            it.let { source1Val ->
                source2.value.let { source2Val ->
                    if (source1Val != null && source2Val != null) {
                        value = source1Val to source2Val
                    }
                }
            }
        }
        addSource(source2) {
            it.let { source2Val ->
                source1.value.let { source1Val ->
                    if (source1Val != null && source2Val != null) {
                        value = source1Val to source2Val
                    }
                }
            }
        }
    }
}