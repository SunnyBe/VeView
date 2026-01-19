package com.veview.app.ui.component

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.veview.app.R

@Composable
fun PermissionDialog(
    @StringRes title: Int,
    @StringRes description: Int,
    onPositiveAction: () -> Unit,
    onNegativeAction: () -> Unit,
    modifier: Modifier = Modifier,
    positiveLabel: Int = R.string.permission_dialog_accept,
    negativeLabel: Int = R.string.permission_dialog_dismiss
) {
    Dialog(onDismissRequest = { onNegativeAction() }) {
        Card(modifier = modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
            Column(
                modifier = Modifier,
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = stringResource(title),
                    modifier = Modifier.padding(16.dp),
                    color = MaterialTheme.colorScheme.onSurface,
                    style = MaterialTheme.typography.titleMedium
                )

                Spacer(modifier = Modifier.padding(8.dp))

                Text(
                    text = stringResource(description),
                    modifier = Modifier.padding(horizontal = 16.dp),
                    color = MaterialTheme.colorScheme.onSurface,
                    style = MaterialTheme.typography.bodyMedium
                )

                Row(
                    modifier = Modifier.padding(16.dp)
                ) {
                    TextButton(onClick = onPositiveAction) {
                        Text(text = stringResource(positiveLabel))
                    }
                    TextButton(onClick = onNegativeAction) {
                        Text(text = stringResource(negativeLabel))
                    }
                }
            }
        }
    }
}

@SuppressWarnings("UnusedPrivateMember")
@Preview
@Composable
private fun PermissionsDialogPreview() {
    PermissionDialog(
        title = R.string.permission_dialog_title,
        description = R.string.permission_dialog_message,
        onPositiveAction = {},
        onNegativeAction = {}
    )
}
