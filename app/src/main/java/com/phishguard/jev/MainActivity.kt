package com.phishguard.jev

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Color as AndroidColor
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.phishguard.jev.ui.GuardState
import com.phishguard.jev.ui.PhishGuardScreen
import com.phishguard.jev.ui.ScannedMessage as UiScannedMessage
import com.phishguard.jev.ui.Verdict
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainActivity : ComponentActivity() {

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { /* Recomposition on next launch/resume reflects the result; nothing to do here. */ }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // The status band is dark whether it's calm or alerting, so the system
        // status bar icons should always be light — not tied to system theme.
        enableEdgeToEdge(statusBarStyle = SystemBarStyle.dark(AndroidColor.TRANSPARENT))
        requestNeededPermissions()
        setContent {
            MaterialTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    PhishGuardRoot()
                }
            }
        }
    }

    private fun requestNeededPermissions() {
        val needed = mutableListOf(Manifest.permission.RECEIVE_SMS, Manifest.permission.READ_SMS)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            needed.add(Manifest.permission.POST_NOTIFICATIONS)
        }
        val missing = needed.filter { checkSelfPermission(it) != PackageManager.PERMISSION_GRANTED }
        if (missing.isNotEmpty()) {
            permissionLauncher.launch(missing.toTypedArray())
        }
    }
}

private val receivedAtFormat = SimpleDateFormat("h:mm a", Locale.getDefault())

private fun ScannedMessage.toUiVerdict(): Verdict = when {
    verdict == "likely_phishing" && confidence >= 0.7 -> Verdict.PHISHING
    verdict == "likely_phishing" || verdict == "uncertain" -> Verdict.CAUTION
    else -> Verdict.CLEAR
}

private fun ScannedMessage.toUi(): UiScannedMessage = UiScannedMessage(
    id = "$sender-$timestamp",
    sender = sender,
    text = snippet,
    receivedAt = receivedAtFormat.format(Date(timestamp)),
    verdict = toUiVerdict(),
    latencyMs = latencyMs.toInt()
)

@Composable
private fun PhishGuardRoot() {
    val context = LocalContext.current
    val messages by MessageStore.messages.collectAsState()
    val stats = MessageStore.stats()
    var showSettings by remember { mutableStateOf(false) }

    val guardState = GuardState(
        scanned = stats.count,
        medianMs = if (stats.count == 0) null else stats.medianLatencyMs.toInt(),
        spentUsd = stats.totalCostUsd,
        messages = messages.map { it.toUi() }
    )

    PhishGuardScreen(
        state = guardState,
        onOpenSettings = { showSettings = true },
        onSendTest = {
            Toast.makeText(
                context,
                "Send this phone any SMS from another number to see it scanned here.",
                Toast.LENGTH_LONG
            ).show()
        },
        onSimulate = {
            Toast.makeText(
                context,
                "Emulator: adb emu sms send <number> \"<text>\"",
                Toast.LENGTH_LONG
            ).show()
        }
    )

    if (showSettings) {
        ApiKeyDialog(onDismiss = { showSettings = false })
    }
}

@Composable
private fun ApiKeyDialog(onDismiss: () -> Unit) {
    val context = LocalContext.current
    var apiKeyInput by remember { mutableStateOf(SettingsStore.getApiKey(context) ?: "") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("TypeSafe API key") },
        text = {
            OutlinedTextField(
                value = apiKeyInput,
                onValueChange = { apiKeyInput = it },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
        },
        confirmButton = {
            Button(onClick = {
                SettingsStore.setApiKey(context, apiKeyInput.trim())
                onDismiss()
            }) { Text("Save") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
