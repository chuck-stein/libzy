package io.libzy.spotify.api

import com.adamratzman.spotify.SpotifyRestActionPaging
import com.adamratzman.spotify.models.AbstractPagingObject
import kotlinx.coroutines.Dispatchers

suspend fun <Z : Any, T : AbstractPagingObject<Z>> SpotifyRestActionPaging<Z, T>.suspendQueueAll(): List<Z> =
    getAllItems(Dispatchers.IO).suspendQueue().filterNotNull()