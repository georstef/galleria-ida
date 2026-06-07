package com.gelleriaida.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gelleriaida.ui.theme.*
import com.gelleriaida.viewmodel.AppViewModel
import org.json.JSONObject

@Composable
fun SettingsScreen(viewModel: AppViewModel, onBack: () -> Unit) {
    val settings by viewModel.settings.collectAsState()
    val apiKeyStatus by viewModel.apiKeyStatus.collectAsState()
    var keyInput by remember(settings.geminiApiKey) { mutableStateOf(settings.geminiApiKey) }
    var showKey by remember { mutableStateOf(false) }
    var showDeleteScreen by remember { mutableStateOf(false) }
    var showModelScreen by remember { mutableStateOf(false) }

    when {
        showDeleteScreen -> DeletePlayersScreen(
            viewModel = viewModel,
            onBack = { showDeleteScreen = false },
            onAllDeleted = { onBack() }
        )
        showModelScreen -> ModelSelectionScreen(
            viewModel = viewModel,
            onBack = { showModelScreen = false }
        )
        else -> {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background)
                    .statusBarsPadding()
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

                Button(
                    onClick = { viewModel.testApiKey(keyInput) },
                    enabled = keyInput.isNotBlank() && apiKeyStatus != "testing",
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = ButtonPrimary)
                ) {
                    Text(
                        if (apiKeyStatus == "testing") "Testing..." else "Test & Save Key",
                        style = MaterialTheme.typography.labelLarge
                    )
                }

                Spacer(Modifier.height(16.dp))

                Button(
                    onClick = { showModelScreen = true },
                    enabled = settings.apiValid,
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = ButtonSecondary,
                        disabledContainerColor = DisabledGray
                    )
                ) {
                    Text("🤖 AI Models", style = MaterialTheme.typography.labelLarge)
                }

                Spacer(Modifier.height(32.dp))
                HorizontalDivider()
                Spacer(Modifier.height(24.dp))

                OutlinedButton(
                    onClick = { showDeleteScreen = true },
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = ErrorRed),
                    border = androidx.compose.foundation.BorderStroke(1.5.dp, ErrorRed)
                ) {
                    Icon(Icons.Default.Delete, contentDescription = null, tint = ErrorRed)
                    Spacer(Modifier.width(8.dp))
                    Text("Manage / Delete Players", style = MaterialTheme.typography.labelLarge, color = ErrorRed)
                }

                Spacer(Modifier.height(24.dp))
                HorizontalDivider()
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
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ModelSelectionScreen(viewModel: AppViewModel, onBack: () -> Unit) {
    val settings by viewModel.settings.collectAsState()
    val allModels = remember(settings.availableModelsJson) { viewModel.parseAvailableModels() }
    val textModels = allModels.filter { it.isTextModel }
    val imageModels = allModels.filter { it.isImageModel }
    var showJsonDialog by remember { mutableStateOf(false) }

    val categories = listOf(
        Triple("questions", "📐 Math Questions", textModels),
        Triple("translation", "🌍 Translation", textModels),
        Triple("imagePrompt", "✏️ Image Description", textModels),
        Triple("imageGeneration", "🎨 Image Generation", imageModels)
    )

    val currentModels = mapOf(
        "questions" to settings.modelQuestions,
        "translation" to settings.modelTranslation,
        "imagePrompt" to settings.modelImagePrompt,
        "imageGeneration" to settings.modelImageGeneration
    )

    // JSON viewer dialog
    if (showJsonDialog) {
        val prettyJson = remember(settings.availableModelsJson) {
            try {
                JSONObject(settings.availableModelsJson).toString(2)
            } catch (e: Exception) {
                settings.availableModelsJson
            }
        }
        AlertDialog(
            onDismissRequest = { showJsonDialog = false },
            title = { Text("Available Models JSON", style = MaterialTheme.typography.titleMedium) },
            text = {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 400.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    Text(
                        text = prettyJson,
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontFamily = FontFamily.Monospace,
                            fontSize = 11.sp
                        ),
                        color = DarkText
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { showJsonDialog = false }) {
                    Text("Close", style = MaterialTheme.typography.bodyLarge)
                }
            }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
            .padding(24.dp)
    ) {
        // Top bar
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = DeepPurple)
                }
                Spacer(Modifier.width(8.dp))
                Text("AI Models 🤖", style = MaterialTheme.typography.titleLarge)
            }
            TextButton(onClick = { showJsonDialog = true }) {
                Text("View JSON", style = MaterialTheme.typography.bodyMedium, color = ButtonSecondary)
            }
        }

        Spacer(Modifier.height(8.dp))
        Text(
            "Choose which AI model is used for each task.",
            style = MaterialTheme.typography.bodyMedium,
            color = MedText
        )
        Spacer(Modifier.height(24.dp))

        LazyColumn(verticalArrangement = Arrangement.spacedBy(20.dp)) {
            items(categories) { (key, label, models) ->
                var expanded by remember { mutableStateOf(false) }
                val current = currentModels[key] ?: ""
                val currentDisplay = allModels.firstOrNull { it.name == current }?.displayName
                    ?: current.removePrefix("models/").ifBlank { "Not set" }

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(CardBg)
                        .padding(16.dp)
                ) {
                    Text(label, style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(8.dp))

                    ExposedDropdownMenuBox(
                        expanded = expanded,
                        onExpandedChange = { expanded = !expanded }
                    ) {
                        OutlinedTextField(
                            value = currentDisplay,
                            onValueChange = {},
                            readOnly = true,
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                            modifier = Modifier.fillMaxWidth().menuAnchor(),
                            shape = RoundedCornerShape(12.dp),
                            textStyle = MaterialTheme.typography.bodyMedium
                        )
                        ExposedDropdownMenu(
                            expanded = expanded,
                            onDismissRequest = { expanded = false }
                        ) {
                            models.forEach { model ->
                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            model.displayName,
                                            style = MaterialTheme.typography.bodyLarge
                                        )
                                    },
                                    onClick = {
                                        viewModel.updateModelSelection(key, model.name)
                                        expanded = false
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun DeletePlayersScreen(
    viewModel: AppViewModel,
    onBack: () -> Unit,
    onAllDeleted: () -> Unit = {}
) {
    val players by viewModel.players.collectAsState()
    val selected = remember { mutableStateListOf<String>() }
    var showConfirm by remember { mutableStateOf(false) }

    LaunchedEffect(players) {
        if (players.isEmpty()) onAllDeleted()
    }

    if (showConfirm) {
        AlertDialog(
            onDismissRequest = { showConfirm = false },
            title = { Text("Delete players?", style = MaterialTheme.typography.titleMedium) },
            text = {
                Text(
                    "This will permanently delete ${selected.size} player(s) and all their stars and gallery images.",
                    style = MaterialTheme.typography.bodyLarge
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deletePlayers(selected.toList())
                        selected.clear()
                        showConfirm = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = ErrorRed),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Delete", style = MaterialTheme.typography.labelLarge)
                }
            },
            dismissButton = {
                TextButton(onClick = { showConfirm = false }) {
                    Text("Cancel", style = MaterialTheme.typography.bodyLarge)
                }
            }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
            .padding(24.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = DeepPurple)
            }
            Spacer(Modifier.width(8.dp))
            Text("Delete Players 🗑️", style = MaterialTheme.typography.titleLarge)
        }

        Spacer(Modifier.height(8.dp))
        Text("Select the players you want to delete.", style = MaterialTheme.typography.bodyMedium, color = MedText)
        Spacer(Modifier.height(24.dp))

        if (players.isEmpty()) {
            Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                Text("No players found.", style = MaterialTheme.typography.bodyLarge, color = MedText)
            }
        } else {
            LazyColumn(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                items(players) { player ->
                    val isSelected = player.id in selected
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(if (isSelected) PeachOrange else CardBg)
                            .border(
                                width = if (isSelected) 2.dp else 0.dp,
                                color = if (isSelected) ErrorRed else androidx.compose.ui.graphics.Color.Transparent,
                                shape = RoundedCornerShape(16.dp)
                            )
                            .clickable {
                                if (isSelected) selected.remove(player.id) else selected.add(player.id)
                            }
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(player.name, style = MaterialTheme.typography.titleMedium)
                            Text(
                                "${player.stars} ⭐  •  ${player.schoolClass.takeIf { it != "—" } ?: "No class"}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MedText
                            )
                        }
                        Checkbox(
                            checked = isSelected,
                            onCheckedChange = {
                                if (isSelected) selected.remove(player.id) else selected.add(player.id)
                            },
                            colors = CheckboxDefaults.colors(checkedColor = ErrorRed, uncheckedColor = MedText)
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(24.dp))

        Button(
            onClick = { if (selected.isNotEmpty()) showConfirm = true },
            enabled = selected.isNotEmpty(),
            modifier = Modifier.fillMaxWidth().height(60.dp),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = ErrorRed,
                disabledContainerColor = DisabledGray
            )
        ) {
            Text(
                if (selected.isEmpty()) "Select players to delete" else "Delete ${selected.size} player(s)",
                style = MaterialTheme.typography.labelLarge
            )
        }
    }
}
