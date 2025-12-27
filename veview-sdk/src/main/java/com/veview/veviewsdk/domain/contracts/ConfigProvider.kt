package com.veview.veviewsdk.domain.contracts

import com.veview.veviewsdk.data.configs.VoiceReviewConfig
import kotlinx.coroutines.flow.Flow

interface ConfigProvider {
    val voiceReviewConfigFlow: Flow<VoiceReviewConfig>
    suspend fun setConfig(voiceReviewConfig: VoiceReviewConfig)
}
