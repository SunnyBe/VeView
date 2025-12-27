package com.veview.veviewsdk.domain.contracts

import kotlinx.coroutines.CoroutineDispatcher

internal interface DispatcherProvider {
    val main: CoroutineDispatcher
    val io: CoroutineDispatcher
    val default: CoroutineDispatcher
}
