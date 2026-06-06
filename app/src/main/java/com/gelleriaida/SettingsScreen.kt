package com.gelleriaida.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.gelleriaida.ui.theme.*
import com.gelleriaida.viewmodel.AppViewModel

@Composable
fun SettingsScreen(viewModel: AppViewModel, onBack: () -> Unit) {
    val settings by viewModel.settings.collectAsState()
    val apiKeyStatus by viewModel.apiKeyStatus.collectAsState()
    var keyInput by remember(settings.geminiApiKey) { mutableStateOf(settings.geminiApiKey) }
    var showKey by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(24.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = DeepPurple)
            }
            Spacer(Modifier.width(8.dp))
            Text("Settings ⚙️", style = MaterialTheme.typography.titleLarge)
        }

        Spacer(Modifier.height(32.dp))

        Text("Gemini API Key", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(8.dp))
        Text(
            "Ask a parent or teacher to enter the Gemini API key here.",
            style = MaterialTheme.typography.bodyMedium,
            color = MedText
        )

        Spacer(Modifier.height(16.dp))

        OutlinedTextField(
            value = keyInput,
            onValueChange = { keyInput = it },
            label = { Text("API Key", style = MaterialTheme.typography.bodyLarge) },
            singleLine = true,
            visualTransformation = if (showKey) VisualTransformation.None else PasswordVisualTransformation(),
            trailingIcon = {
                TextButton(onClick = { showKey = !showKey }) {
                    Text(if (showKey) "Hide" else "Show", style = MaterialTheme.typography.bodyMedium)
                }
            },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            textStyle = MaterialTheme.typography.bodyMedium
        )

        Spacer(Modifier.height(16.dp))

        // Status indicator
        val (statusText, statusColor) = when {
            apiKeyStatus == "testing" -> "Testing..." to MedText
            apiKeyStatus == "valid" -> "✅ API key is valid" to SuccessGreen
            apiKeyStatus == "invalid" -> "❌ API key is invalid" to ErrorRed
            settings.apiValid -> "✅ Key saved and valid" to SuccessGreen
            settings.geminiApiKey.isNotBlank() -> "⚠️ Not yet tested" to MedText
            else -> "No API key set" to MedText
        }
        Text(statusText, style = MaterialTheme.typography.bodyMedium, color = statusColor)

        Spacer(Modifier.height(24.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Button(
                onClick = { viewModel.saveApiKey(keyInput) },
                modifier = Modifier.weight(1f).height(56.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = ButtonSecondary)
            ) {
                Text("Save", style = MaterialTheme.typography.labelLarge)
            }
            Button(
                onClick = { viewModel.testApiKey(keyInput) },
                enabled = keyInput.isNotBlank() && apiKeyStatus != "testing",
                modifier = Modifier.weight(1f).height(56.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = ButtonPrimary)
            ) {
                Text("Test Key", style = MaterialTheme.typography.labelLarge)
            }
        }

        Spacer(Modifier.height(32.dp))
        Divider()
        Spacer(Modifier.height(16.dp))

        Text(
            "🔒 The API key is stored only on this device and is never shared.",
            style = MaterialTheme.typography.bodyMedium,
            color = MedText,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )
    }
}
