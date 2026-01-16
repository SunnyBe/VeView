package com.veview.app.ui.component

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import androidx.activity.compose.LocalActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContract
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.veview.app.R
import timber.log.Timber

@Composable
fun PermissionsChecker(
    onGrant: () -> Unit,
    onDeny: () -> Unit
) {
    val context = LocalContext.current
    val activity = LocalActivity.current ?: error("No activity found")

    var showRationale by remember { mutableStateOf(false) }
    var showSettings by remember { mutableStateOf(false) }

    val currentOnGrant by rememberUpdatedState(onGrant)
    val currentOnDeny by rememberUpdatedState(onDeny)

    val permissionLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
            if (isGranted) {
                currentOnGrant()
                showRationale = false
                showSettings = false
            } else if (activity.shouldShowRationale()) {
                showRationale = true
                showSettings = false
            } else {
                showSettings = true
                showRationale = false
            }
        }

    val settingsPermissionLauncher = rememberLauncherForActivityResult(OpenSettingsContract()) {
        if (context.isPermissionEnabled()) {
            currentOnGrant()
            showRationale = false
            showSettings = false
        } else {
            currentOnDeny()
        }
    }

    LaunchedEffect(Unit) { // Triggered when PermissionsChecker enters composition
        Timber.tag("PermissionsChecker").d("Checking permission status")
        when {
            context.isPermissionEnabled() -> { currentOnGrant() }
            else -> { showRationale = true }
        }
    }

    when {
        showRationale -> {
            PermissionDialog(
                title = R.string.permission_dialog_title,
                description = R.string.permission_dialog_message,
                onPositiveAction = {
                    showRationale = false
                    permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                },
                onNegativeAction = {
                    showRationale = false
                    showSettings = true
                }
            )
        }
        showSettings -> {
            PermissionDialog(
                title = R.string.permission_dialog_title,
                description = R.string.permission_denied_dialog_message,
                positiveLabel = R.string.settings_label,
                onPositiveAction = {
                    showSettings = false
                    settingsPermissionLauncher.launch(Unit)
                },
                negativeLabel = R.string.exit_label,
                onNegativeAction = {
                    showSettings = false
                    currentOnDeny()
                }
            )
        }
    }
}

private fun Activity.shouldShowRationale(): Boolean {
    return ActivityCompat.shouldShowRequestPermissionRationale(
        this,
        Manifest.permission.RECORD_AUDIO
    )
}

fun Context.isPermissionEnabled(): Boolean {
    return ContextCompat.checkSelfPermission(
        this,
        Manifest.permission.RECORD_AUDIO
    ) == PackageManager.PERMISSION_GRANTED
}

class OpenSettingsContract : ActivityResultContract<Unit, Boolean>() {
    override fun createIntent(context: Context, input: Unit): Intent {
        return Intent(
            android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS
        ).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            data = android.net.Uri.fromParts("package", context.packageName, null)
        }
    }

    override fun parseResult(
        resultCode: Int,
        intent: Intent?
    ): Boolean {
        return resultCode == Activity.RESULT_OK
    }
}
