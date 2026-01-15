package com.veview.app.ui.component

import androidx.annotation.RawRes
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
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.veview.app.R
import com.veview.app.ui.theme.VeViewTheme

@Suppress("LongParameterList")
@Composable
fun CustomAlertDialog(
    title: String,
    @RawRes illustration: Int,
    positiveButtonLabel: String,
    negativeButtonLabel: String,
    onNegative: () -> Unit,
    modifier: Modifier = Modifier,
    description: String? = null,
    onPositive: (() -> Unit)? = null
) {
    Dialog(
        onDismissRequest = onNegative
    ) {
        Card(
            modifier = modifier
                .fillMaxWidth()
                .height(375.dp)
                .padding(16.dp),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = title,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.titleLarge
                )
                Spacer(modifier = Modifier.padding(vertical = 16.dp))
                AnimatedIllustration(
                    modifier = Modifier.size(120.dp),
                    illustration = illustration
                )
                Spacer(modifier = Modifier.padding(vertical = 8.dp))
                description?.let {
                    Text(
                        text = description,
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Center
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center
                ) {
                    TextButton(
                        enabled = onPositive != null,
                        onClick = { onPositive?.invoke() },
                        modifier = Modifier.padding(8.dp)
                    ) {
                        Text(positiveButtonLabel)
                    }
                    TextButton(
                        onClick = onNegative,
                        modifier = Modifier.padding(8.dp)
                    ) {
                        Text(negativeButtonLabel)
                    }
                }
            }
        }
    }
}

@Suppress("UnusedPrivateMember") // DNF: silent in config later
@Preview
@Composable
private fun CustomAlertDialogPreview() {
    VeViewTheme {
        CustomAlertDialog(
            title = "Error Title",
            description = "Error content goes here!",
            positiveButtonLabel = "Retry",
            negativeButtonLabel = "Dismiss",
            onPositive = {},
            onNegative = {},
            illustration = R.raw.animator_success
        )
    }
}
