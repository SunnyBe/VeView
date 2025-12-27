package com.veview.veviewsdk.data.configs

import android.content.Context
import android.media.AudioFormat
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.veview.veviewsdk.domain.contracts.ConfigProvider
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlin.time.Duration.Companion.milliseconds

// At the top level of your kotlin file:
val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "voice_review_settings")

/**
 * DataStore to emit flows of config data for easy testing without needing to rebundle the app.
 * If voiceReviewConfig is set by the client, it will always return the config set by the client.
 * For developer testing, [voiceReviewConfig] should not be set, to allow live config changes.
 */
internal class LocalConfigProviderImpl(
    val context: Context,
    val voiceReviewConfig: VoiceReviewConfig? = null
) : ConfigProvider {

    private val recordDurationPreferences = longPreferencesKey(VR_RECORD_DURATION_KEY)
    private val audioFormatPreferences = intPreferencesKey(VR_AUDIO_FORMAT_KEY)
    private val storageFilePreferences = intPreferencesKey(VR_STORAGE_DIR_KEY)

    override val voiceReviewConfigFlow: Flow<VoiceReviewConfig>
        get() = voiceReviewConfig?.let { flowOf(it) } ?: combine(
            recordDurationMillisFlow(),
            audioFormat(),
            storageDir()
        ) { recordDurationMillis, audioFormatInt, storageDirInt ->
            VoiceReviewConfig.Builder()
                .setRecordDuration(recordDurationMillis.milliseconds)
                .setStorageDirectory(storageDirInt.storageDir())
                .setAudioFormat(audioFormatInt)
                .build()
        }

    // TODO complete this for all voice review configs
    override suspend fun setConfig(voiceReviewConfig: VoiceReviewConfig) {
        context.dataStore.updateData { preferences ->
            preferences.toMutablePreferences().also { preference ->
                preference[recordDurationPreferences] =
                    voiceReviewConfig.recordDuration.inWholeMilliseconds
            }
        }
    }

    private fun recordDurationMillisFlow(): Flow<Long> = context.dataStore.data.map { preferences ->
        preferences[recordDurationPreferences] ?: VR_RECORD_DURATION_DEFAULT
    }

    private fun audioFormat(): Flow<Int> = context.dataStore.data.map { preferences ->
        preferences[audioFormatPreferences] ?: VR_AUDIO_FORMAT_KEY_DEFAULT
    }

    private fun storageDir(): Flow<Int> = context.dataStore.data.map { preferences ->
        preferences[storageFilePreferences] ?: VR_STORAGE_DIR_DEFAULT
    }

    private fun Int.storageDir() = when (this) {
        0 -> context.cacheDir
        1 -> context.filesDir
        else -> context.cacheDir
    }

    companion object {
        private const val VR_RECORD_DURATION_KEY = "vr_record_duration_key"
        private const val VR_AUDIO_FORMAT_KEY = "vr_audio_format_key"
        private const val VR_STORAGE_DIR_KEY = "vr_storage_dir_key"

        private const val VR_RECORD_DURATION_DEFAULT = 5000L
        private const val VR_AUDIO_FORMAT_KEY_DEFAULT = AudioFormat.ENCODING_PCM_16BIT
        private const val VR_STORAGE_DIR_DEFAULT = 1
    }
}
