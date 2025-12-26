package com.veview.veview_sdk.domain.contracts

import com.veview.veview_sdk.data.configs.VoiceReviewConfig
import kotlinx.coroutines.flow.Flow

interface ConfigProvider {
    val voiceReviewConfigFlow: Flow<VoiceReviewConfig>
    suspend fun setConfig(voiceReviewConfig: VoiceReviewConfig)
}