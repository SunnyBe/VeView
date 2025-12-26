package com.veview.veview_sdk.configs

import android.content.Context
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

interface ConfigProvider {
    val configFlow: Flow<VoiceReviewConfig>

    companion object {
        internal fun defaultProvider(
            context: Context,
            config: VoiceReviewConfig? = null
        ): ConfigProvider =
            object : ConfigProvider {
                override val configFlow: Flow<VoiceReviewConfig>
                    get() = flowOf(
                        config ?: VoiceReviewConfig.Builder()
                            .setStorageDirectory(context.cacheDir)
                            .build()
                    )
            }

        internal fun localProvider(context: Context): ConfigProvider =
            LocalConfigProviderImpl(context)
    }
}