package com.veview.veviewsdk.domain.contracts

import kotlinx.coroutines.CoroutineDispatcher

/**
 * A contract for providing standardized CoroutineDispatchers.
 * This internal interface is used for dependency injection to make classes that use coroutines
 * more easily testable. By providing dispatchers, tests can replace the standard dispatchers
 * (e.g., `Dispatchers.IO`) with a `TestDispatcher`.
 */
internal interface DispatcherProvider {
    /** The main UI thread dispatcher. */
    val main: CoroutineDispatcher

    /** The dispatcher for I/O-bound operations (e.g., network, disk). */
    val io: CoroutineDispatcher

    /** The dispatcher for CPU-intensive work. */
    val default: CoroutineDispatcher
}
