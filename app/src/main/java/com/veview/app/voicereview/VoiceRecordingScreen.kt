package com.veview.app.voicereview

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonColors
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.veview.app.R
import com.veview.app.ui.component.AnimatedIllustration
import com.veview.app.ui.component.CustomDialog
import com.veview.app.ui.theme.VeViewTheme

@Composable
fun VoiceRecordingScreen(
    state: MainUiState,
    onStart: (() -> Unit)?,
    onPause: (() -> Unit)?,
    onCancel: (() -> Unit)?,
    onDone: (() -> Unit)?
) {
    when (state) {
        is MainUiState.Error -> CustomDialog(
            title = "Review Failed",
            content = state.message,
            onRetry = null,
            onDismiss = { onCancel?.invoke() },
            illustration = R.raw.animatior_unkown_error
        )

        is MainUiState.Recording -> RecordingStatusDialog(
            status = state.status,
            isRecording = state.isRecording,
            onDone = onPause,
            onCancel = onCancel
        )

        is MainUiState.ReadyToRecord -> VoiceReviewerContent(
            ctaLabel = "Record",
            onStart = { onStart?.invoke() }
        )

        is MainUiState.ReviewNoted -> CustomDialog(
            title = "Success",
            content = "A VoiceReview object is ready. Summary: ${state.result}",
            onRetry = null,
            onDismiss = { onDone?.invoke() },
            illustration = R.raw.animator_success
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VoiceReviewerContent(
    ctaLabel: String,
    modifier: Modifier = Modifier,
    onStart: (() -> Unit)? = null
) {
    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(title = { Text("VeView Reviewer") })
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .padding(paddingValues)
                .fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            AnimatedIllustration(
                illustration = R.raw.animation_user_review,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(375.dp)
                    .padding(16.dp)
            )
            Text(
                text = "* Tell us what you think about this service.",
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
            )
            Text(
                text = "* Mention the rating you would give us.",
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
            )
            Text(
                text = "* Other things you might want to say.",
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
            )
            Spacer(modifier = Modifier.padding(vertical = 32.dp))
            Button(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                colors = ButtonColors(
                    containerColor = Color.Red,
                    contentColor = Color.White,
                    disabledContainerColor = Color.Gray,
                    disabledContentColor = Color.LightGray
                ),
                enabled = onStart != null,
                onClick = { onStart?.invoke() }
            ) {
                Text(ctaLabel)
            }
        }
    }
}

@Composable
fun RecordingStatusDialog(
    status: String?,
    isRecording: Boolean,
    onDone: (() -> Unit)? = null,
    onCancel: (() -> Unit)? = null
) {
    Dialog(
        onDismissRequest = { onCancel?.invoke() }
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .height(375.dp)
                .padding(8.dp),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxSize().padding(8.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "Recording...",
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.titleLarge
                )
                Spacer(modifier = Modifier.padding(vertical = 16.dp))
                AnimatedIllustration(
                    modifier = Modifier.size(120.dp),
                    illustration = R.raw.recording_animation,
                    isPlaying = isRecording
                )
                Spacer(modifier = Modifier.padding(vertical = 8.dp))
                Text(
                    text = status ?: "",
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center
                ) {
                    TextButton(
                        enabled = onCancel != null,
                        onClick = { onCancel?.invoke() },
                        modifier = Modifier.padding(8.dp)
                    ) {
                        Text("Cancel")
                    }
                    TextButton(
                        enabled = onDone != null,
                        onClick = { onDone?.invoke() },
                        modifier = Modifier.padding(8.dp)
                    ) {
                        Text("Done")
                    }
                }
            }
        }
    }
}

@Suppress("UnusedPrivateMember") // DNF: silent in config
@Preview
@Composable
private fun RecordingStatusDialogPreview() {
    VeViewTheme {
        RecordingStatusDialog(
            status = "Recording...",
            isRecording = true,
            onDone = {},
            onCancel = {}
        )
    }
}

@Suppress("UnusedPrivateMember") // DNF: silent in config
@Preview(showBackground = true)
@Composable
private fun VoiceReviewerContentPreview() {
    VeViewTheme {
        VoiceReviewerContent(
            ctaLabel = "Record",
            onStart = {}
        )
    }
}
