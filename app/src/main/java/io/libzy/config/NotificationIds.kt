package io.libzy.config

import java.util.concurrent.atomic.AtomicInteger

/**
 * Centralizes all notification IDs such that all instances of the same event share the same unique notification ID.
 */
object NotificationIds {

    private val nextId = AtomicInteger(0)

    val initialScanProgress = getNextId()
    val initialScanEnd = getNextId()

    private fun getNextId() = nextId.getAndIncrement()
}
