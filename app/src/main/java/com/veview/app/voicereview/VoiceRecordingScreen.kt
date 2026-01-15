package com.veview.app.voicereview

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
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
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.veview.app.R
import com.veview.app.ui.component.AnimatedIllustration
import com.veview.app.ui.component.CustomAlertDialog
import com.veview.app.ui.theme.VeViewTheme

@OptIn(ExperimentalMaterial3Api::class)
@Suppress("ModifierMissing")
@Composable
fun VoiceRecordingScreen(
    state: VoiceReviewUiState,
    instructionItems: List<ReviewInstructionItem>,
    onEvent: (VoiceReviewEvent) -> Unit
) {
    var showConfirmationDialog by remember { mutableStateOf(false) }

    BackHandler(enabled = !showConfirmationDialog) { showConfirmationDialog = true }

    if (showConfirmationDialog) {
        CustomAlertDialog(
            title = stringResource(R.string.cancel_recording_title),
            description = stringResource(
                if (state is VoiceReviewUiState.VoiceReviewState && state.recordingState.isRecording) {
                    R.string.confirmation_ongoing_recording_content
                } else {
                    R.string.confirmation_exit_review
                }
            ),
            positiveButtonLabel = stringResource(R.string.yes_label),
            negativeButtonLabel = stringResource(R.string.no_label),
            onPositive = {
                showConfirmationDialog = false
                onEvent(VoiceReviewEvent.ConfirmCancel)
            },
            onNegative = { showConfirmationDialog = false },
            illustration = R.raw.animatior_unkown_error
        )
    }
    Scaffold(
        topBar = {
            TopAppBar(title = { Text(stringResource(R.string.app_name)) })
        }
    ) { paddingValues ->
        val context = LocalContext.current
        val baseModifier = Modifier.padding(paddingValues)
        when (state) {
            is VoiceReviewUiState.Error -> {
                CustomAlertDialog(
                    modifier = baseModifier,
                    title = "Review Failed",
                    description = stringResource(state.message),
                    positiveButtonLabel = stringResource(R.string.retry_label),
                    negativeButtonLabel = stringResource(R.string.dismiss_label),
                    onPositive = null,
                    onNegative = { onEvent(VoiceReviewEvent.CancelRecording) },
                    illustration = R.raw.animatior_unkown_error
                )
            }

            is VoiceReviewUiState.VoiceReviewState -> {
                if (state.recordingState.isRecording) {
                    RecordingStatusDialog(
                        modifier = baseModifier,
                        status = stringResource(
                            state.recordingState.status ?: R.string.recording_label
                        ),
                        isSpeaking = state.recordingState.isSpeaking,
                        onDone = { onEvent(VoiceReviewEvent.PauseRecording) },
                        onCancel = { onEvent(VoiceReviewEvent.CancelRecording) }
                    )
                }

                VoiceReviewerContent(
                    modifier = baseModifier,
                    ctaLabel = stringResource(R.string.record_label),
                    reviewInstructionToDetail = instructionItems,
                    onStart = {
                        onEvent(
                            VoiceReviewEvent.StartRecording(context.getString(R.string.default_business_name))
                        )
                    }
                )
            }

            is VoiceReviewUiState.ReviewNoted -> CustomAlertDialog(
                modifier = Modifier.padding(paddingValues),
                title = stringResource(R.string.reveiw_sucess_title),
                description = stringResource(R.string.review_completed_message, state.result),
                positiveButtonLabel = stringResource(R.string.retry_label),
                negativeButtonLabel = stringResource(R.string.dismiss_label),
                onPositive = null,
                onNegative = { onEvent(VoiceReviewEvent.DoneRecording) },
                illustration = R.raw.animator_success
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VoiceReviewerContent(
    ctaLabel: String,
    reviewInstructionToDetail: List<ReviewInstructionItem>,
    modifier: Modifier = Modifier,
    onStart: (() -> Unit)? = null
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
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
        reviewInstructionToDetail.forEach { instruction ->
            InstructionItem(instruction)
        }
        Spacer(modifier = Modifier.padding(vertical = 32.dp))
        Button(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            enabled = onStart != null,
            onClick = { onStart?.invoke() }
        ) {
            Text(ctaLabel)
        }
    }
}

@Composable
private fun InstructionItem(
    item: ReviewInstructionItem,
    modifier: Modifier = Modifier
) {
    var showDetail by remember { mutableStateOf(false) }

    if (showDetail) {
        CustomDetailDialog(
            description = stringResource(item.detailRes),
            onDismiss = { showDetail = false }
        )
    }
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp)
    ) {
        Icon(
            painter = painterResource(R.drawable.rounded_help_24),
            contentDescription = stringResource(R.string.detail_label),
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier
                .size(16.dp)
                .clickable(onClick = { showDetail = true })
                .align(Alignment.CenterVertically)
        )
        Spacer(modifier = Modifier.padding(2.dp))
        Text(text = stringResource(item.descriptionRes), fontSize = 12.sp)
    }
}

@Composable
private fun RecordingStatusDialog(
    status: String?,
    isSpeaking: Boolean,
    modifier: Modifier = Modifier,
    onDone: (() -> Unit)? = null,
    onCancel: (() -> Unit)? = null
) {
    Dialog(
        onDismissRequest = { onCancel?.invoke() }
    ) {
        Card(
            modifier = modifier
                .fillMaxWidth()
                .height(375.dp)
                .padding(8.dp),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(8.dp),
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
                    isPlaying = isSpeaking
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

@Composable
private fun CustomDetailDialog(
    description: String,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    Dialog(
        onDismissRequest = { onDismiss() }
    ) {
        Card(
            modifier = modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(16.dp)
        ) {
            Text(
                text = description,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                fontSize = 12.sp,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Suppress("UnusedPrivateMember") // DNF: silent in config
@Preview
@Composable
private fun CustomDetailDialogPreview() {
    VeViewTheme {
        CustomDetailDialog(
            description = "This is a detailed description for the instruction item.",
            onDismiss = {}
        )
    }
}

@Suppress("UnusedPrivateMember") // DNF: silent in config
@Preview
@Composable
private fun RecordingStatusDialogPreview() {
    VeViewTheme {
        RecordingStatusDialog(
            status = "Recording...",
            isSpeaking = true,
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
            reviewInstructionToDetail = listOf(
                ReviewInstructionItem(
                    0,
                    R.string.recording_instruction_1,
                    R.string.recording_instruction_1
                ),
                ReviewInstructionItem(
                    1,
                    R.string.recording_instruction_2,
                    R.string.recording_instruction_2
                ),
                ReviewInstructionItem(
                    2,
                    R.string.recording_instruction_3,
                    R.string.recording_instruction_3
                )
            ),
            onStart = {}
        )
    }
}
