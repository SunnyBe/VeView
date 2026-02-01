package com.veview.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.veview.app.ui.component.PermissionsChecker
import com.veview.app.ui.theme.VeViewTheme
import com.veview.app.voicereview.VoiceRecordingScreen
import com.veview.app.voicereview.VoiceReviewEffect
import kotlinx.coroutines.flow.onEach
import timber.log.Timber

class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels { MainViewModel.FACTORY }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Timber.tag(LOG_TAG).d("onCreate called")
        enableEdgeToEdge()

        setContent {
            VeViewTheme {
                LaunchedEffect(Unit) {
                    viewModel.effects
                        .onEach { effect -> Timber.tag(LOG_TAG).d("Received effect: [$effect]") }
                        .collect { effect ->
                            when (effect) {
                                is VoiceReviewEffect.CloseApplication -> {
                                    Timber.tag(LOG_TAG).d("closing application as per effect")
                                    finish()
                                }
                            }
                        }
                }
                val uiState by viewModel.uiState.collectAsStateWithLifecycle()

                if (!uiState.hasAudioPermission) {
                    PermissionsChecker(
                        onGrant = viewModel::onPermissionGranted,
                        onDeny = viewModel::onPermissionDenied
                    )
                } else {
                    VoiceRecordingScreen(
                        businessName = uiState.businessName
                            ?: getString(R.string.default_business_name),
                        state = uiState.voiceReviewState,
                        instructionItems = uiState.instructionItems,
                        onEvent = viewModel::handleEvent
                    )
                }
            }
        }
    }

    companion object {
        private const val LOG_TAG = "MainActivity"
    }
}
