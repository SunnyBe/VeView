package com.veview.app.voicereview

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.veview.app.ui.theme.VeViewTheme

class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels { MainViewModel.FACTORY }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            VeViewTheme {
                val uiState by viewModel.uiState.collectAsStateWithLifecycle()
                VoiceRecordingScreen(
                    state = uiState,
                    onStart = { viewModel.startReview("Restaurant") },
                    onPause = { viewModel.pauseReview() },
                    onCancel = { viewModel.cancelReview() },
                    onDone = { viewModel.onReviewDone() }
                )
            }
        }
    }
}
