package com.veview.veview_sdk.configs

import android.content.Context
import android.media.AudioFormat
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlin.time.Duration.Companion.milliseconds

// At the top level of your kotlin file:
val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "voice_review_settings")

/**
 * DataStore to emit flows of config data for easy testing without needing to rebundle the app.
 * Will implement data store later
 */
internal class LocalConfigProviderImpl constructor(
    val context: Context
) : ConfigProvider {

    private val recordDurationPreferences = longPreferencesKey(VR_RECORD_DURATION_KEY)
    private val audioFormatPreferences = intPreferencesKey(VR_AUDIO_FORMAT_KEY)
    private val storageFilePreferences = intPreferencesKey(VR_STORAGE_DIR_KEY)

    override val configFlow: Flow<VoiceReviewConfig>
        get() = combine(
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

    fun recordDurationMillisFlow(): Flow<Long> = context.dataStore.data.map { preferences ->
        preferences[recordDurationPreferences] ?: VR_RECORD_DURATION_DEFAULT
    }

    fun audioFormat(): Flow<Int> = context.dataStore.data.map { preferences ->
        preferences[audioFormatPreferences] ?: VR_AUDIO_FORMAT_KEY_DEFAULT
    }

    fun storageDir(): Flow<Int> = context.dataStore.data.map { preferences ->
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