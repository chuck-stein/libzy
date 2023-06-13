package io.libzy.repository

import io.libzy.domain.Query
import io.libzy.persistence.prefs.PrefsStore
import io.libzy.persistence.prefs.PrefsStore.Keys.ENABLED_QUERY_PARAMS
import javax.inject.Inject

class SettingsRepository @Inject constructor(private val prefsStore: PrefsStore) {

    val enabledQueryParams = prefsStore.getFlowOf(ENABLED_QUERY_PARAMS)

    suspend fun setEnabledQueryParams(params: Set<Query.Parameter>) {
        prefsStore.edit { prefs ->
            prefs[ENABLED_QUERY_PARAMS] = params.map { it.stringValue }.toSet()
        }
    }
}