package com.veview.veviewsdk.domain.contracts

import com.veview.veviewsdk.data.configs.VoiceReviewConfig
import kotlinx.coroutines.flow.Flow

/**
 * A contract for providing and managing the SDK's configuration.
 * This interface allows for a flexible configuration strategy, enabling settings to be sourced
 * locally, remotely, or from a combination of providers.
 */
interface ConfigProvider {

    /**
     * A [Flow] that emits the current [VoiceReviewConfig].
     * Clients can collect this flow to receive real-time updates to the configuration.
     */
    val voiceReviewConfigFlow: Flow<VoiceReviewConfig>

    /**
     * Updates the current configuration with the provided [VoiceReviewConfig].
     *
     * @param voiceReviewConfig The new configuration to apply.
     */
    suspend fun setConfig(voiceReviewConfig: VoiceReviewConfig)
}
