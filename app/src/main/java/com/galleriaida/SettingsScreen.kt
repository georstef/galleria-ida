package com.galleriaida.ui.screens

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
import com.galleriaida.data.POLLINATIONS_MODELS
import com.galleriaida.data.MODELSCOPE_MODELS
import com.galleriaida.BuildConfig
import com.galleriaida.ui.theme.*
import com.galleriaida.viewmodel.AppViewModel
import org.json.JSONObject

// ── Helper ───────────────────────────────────────────────────────────────────

fun getModelDescription(name: String, availableModelsJson: String): String {
    if (availableModelsJson.isBlank()) return ""
    return try {
        val arr = JSONObject(availableModelsJson).getJSONArray("models")
        for (i in 0 until arr.length()) {
            val obj = arr.getJSONObject(i)
            if (obj.getString("name") == name) return obj.optString("description", "")
        }
        ""
    } catch (e: Exception) { "" }
}

// ── Settings root ─────────────────────────────────────────────────────────────

@Composable
fun SettingsScreen(viewModel: AppViewModel, onBack: () -> Unit) {
    val settings            by viewModel.settings.collectAsState()
    val apiKeyStatus        by viewModel.apiKeyStatus.collectAsState()
    val pollinationsStatus  by viewModel.pollinationsKeyStatus.collectAsState()
    val modelScopeStatus    by viewModel.modelScopeKeyStatus.collectAsState()

    var keyInput             by remember(settings.geminiApiKey)        { mutableStateOf(settings.geminiApiKey) }
    var pollinationsKeyInput by remember(settings.pollinationsApiKey)  { mutableStateOf(settings.pollinationsApiKey) }
    var modelScopeKeyInput   by remember(settings.modelScopeApiKey)    { mutableStateOf(settings.modelScopeApiKey) }
    var showGeminiKey        by remember { mutableStateOf(false) }
    var showPollinationsKey  by remember { mutableStateOf(false) }
    var showModelScopeKey    by remember { mutableStateOf(false) }
    var showDeleteScreen     by remember { mutableStateOf(false) }
    var showGeminiModels     by remember { mutableStateOf(false) }
    var showPollinationsModels by remember { mutableStateOf(false) }
    var showModelScopeModels   by remember { mutableStateOf(false) }

    when {
        showDeleteScreen       -> DeletePlayersScreen(
            viewModel  = viewModel,
            onBack     = { showDeleteScreen = false },
            onAllDeleted = { onBack() }
        )
        showGeminiModels       -> ModelSelectionScreen(
            viewModel  = viewModel,
            onBack     = { showGeminiModels = false }
        )
        showPollinationsModels -> PollinationsModelScreen(
            viewModel  = viewModel,
            onBack     = { showPollinationsModels = false }
        )
        showModelScopeModels -> ModelScopeModelScreen(
            viewModel  = viewModel,
            onBack     = { showModelScopeModels = false }
        )
        else -> {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background)
                    .statusBarsPadding()
                    .verticalScroll(rememberScrollState())
                    .padding(24.dp)
            ) {
                // ── Title ────────────────────────────────────────────────────
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = DeepPurple)
                    }
                    Spacer(Modifier.width(8.dp))
                    Text("Settings ⚙️", style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f))
                    Text(
                        text  = "v${BuildConfig.VERSION_NAME}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MedText
                    )
                }

                Spacer(Modifier.height(32.dp))

                // ══════════════════════════════════════════════════════════════
                // SECTION 1 – Gemini
                // ══════════════════════════════════════════════════════════════
                SectionHeader("🤖 Gemini API")
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
                    label = { Text("Gemini API Key", style = MaterialTheme.typography.bodyLarge) },
                    singleLine = true,
                    visualTransformation = if (showGeminiKey) VisualTransformation.None else PasswordVisualTransformation(),
                    trailingIcon = {
                        TextButton(onClick = { showGeminiKey = !showGeminiKey }) {
                            Text(if (showGeminiKey) "Hide" else "Show", style = MaterialTheme.typography.bodyMedium)
                        }
                    },
                    modifier  = Modifier.fillMaxWidth(),
                    shape     = RoundedCornerShape(16.dp),
                    textStyle = MaterialTheme.typography.bodyMedium
                )

                Spacer(Modifier.height(12.dp))

                val (geminiStatusText, geminiStatusColor) = when {
                    apiKeyStatus == "testing"      -> "Testing…"         to MedText
                    apiKeyStatus == "valid"        -> "✅ API key is valid" to SuccessGreen
                    apiKeyStatus == "invalid"      -> "❌ API key is invalid" to ErrorRed
                    settings.apiValid              -> "✅ Key saved and valid" to SuccessGreen
                    settings.geminiApiKey.isNotBlank() -> "⚠️ Not yet tested" to MedText
                    else -> "No API key set" to MedText
                }
                Text(geminiStatusText, style = MaterialTheme.typography.bodyMedium, color = geminiStatusColor)

                Spacer(Modifier.height(16.dp))

                Row(
                    modifier              = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Button(
                        onClick  = { viewModel.testApiKey(keyInput) },
                        enabled  = keyInput.isNotBlank() && apiKeyStatus != "testing",
                        modifier = Modifier.weight(1f).height(56.dp),
                        shape    = RoundedCornerShape(16.dp),
                        colors   = ButtonDefaults.buttonColors(containerColor = ButtonPrimary)
                    ) {
                        Text(
                            if (apiKeyStatus == "testing") "Testing…" else "Test & Save",
                            style = MaterialTheme.typography.labelLarge
                        )
                    }
                    Button(
                        onClick  = { showGeminiModels = true },
                        enabled  = settings.apiValid,
                        modifier = Modifier.weight(1f).height(56.dp),
                        shape    = RoundedCornerShape(16.dp),
                        colors   = ButtonDefaults.buttonColors(
                            containerColor         = ButtonSecondary,
                            disabledContainerColor = DisabledGray
                        )
                    ) {
                        Text("🤖 Models", style = MaterialTheme.typography.labelLarge)
                    }
                }

                Spacer(Modifier.height(32.dp))
                HorizontalDivider()
                Spacer(Modifier.height(24.dp))

                // ══════════════════════════════════════════════════════════════
                // SECTION 2 – Pollinations
                // ══════════════════════════════════════════════════════════════
                SectionHeader("🌸 Pollinations.ai API (Backup Image Engine)")
                Spacer(Modifier.height(8.dp))
                Text(
                    "Used as a fallback when Gemini image generation is unavailable. " +
                            "If you leave the key blank, the free tier will be used.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MedText
                )
                Spacer(Modifier.height(16.dp))

                OutlinedTextField(
                    value = pollinationsKeyInput,
                    onValueChange = { pollinationsKeyInput = it },
                    label = { Text("Pollinations API Key (optional)", style = MaterialTheme.typography.bodyLarge) },
                    singleLine = true,
                    visualTransformation = if (showPollinationsKey) VisualTransformation.None else PasswordVisualTransformation(),
                    trailingIcon = {
                        TextButton(onClick = { showPollinationsKey = !showPollinationsKey }) {
                            Text(if (showPollinationsKey) "Hide" else "Show", style = MaterialTheme.typography.bodyMedium)
                        }
                    },
                    modifier  = Modifier.fillMaxWidth(),
                    shape     = RoundedCornerShape(16.dp),
                    textStyle = MaterialTheme.typography.bodyMedium
                )

                Spacer(Modifier.height(12.dp))

                val (pollStatusText, pollStatusColor) = when {
                    pollinationsStatus == "testing" -> "Testing…"                    to MedText
                    pollinationsStatus == "valid"   -> "✅ Connection successful"     to SuccessGreen
                    pollinationsStatus == "invalid" -> "❌ Could not reach Pollinations" to ErrorRed
                    settings.pollinationsKeyValid   -> "✅ Key saved and verified"    to SuccessGreen
                    settings.pollinationsApiKey.isNotBlank() -> "⚠️ Not yet tested"  to MedText
                    else -> "Using free tier (no key)" to MedText
                }
                Text(pollStatusText, style = MaterialTheme.typography.bodyMedium, color = pollStatusColor)

                Spacer(Modifier.height(16.dp))

                Row(
                    modifier              = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Button(
                        onClick  = { viewModel.testPollinationsKey(pollinationsKeyInput) },
                        enabled  = pollinationsStatus != "testing",
                        modifier = Modifier.weight(1f).height(56.dp),
                        shape    = RoundedCornerShape(16.dp),
                        colors   = ButtonDefaults.buttonColors(containerColor = ButtonPrimary)
                    ) {
                        Text(
                            if (pollinationsStatus == "testing") "Testing…" else "Test & Save",
                            style = MaterialTheme.typography.labelLarge
                        )
                    }
                    Button(
                        onClick  = { showPollinationsModels = true },
                        enabled  = true,
                        modifier = Modifier.weight(1f).height(56.dp),
                        shape    = RoundedCornerShape(16.dp),
                        colors   = ButtonDefaults.buttonColors(
                            containerColor         = ButtonSecondary,
                            disabledContainerColor = DisabledGray
                        )
                    ) {
                        Text("🌸 Models", style = MaterialTheme.typography.labelLarge)
                    }
                }

                Spacer(Modifier.height(32.dp))
                HorizontalDivider()
                Spacer(Modifier.height(24.dp))

                // ══════════════════════════════════════════════════════════════
                // SECTION 2b – ModelScope (async provider)
                // ══════════════════════════════════════════════════════════════
                SectionHeader("🧩 ModelScope AI (Async Image Engine)")
                Spacer(Modifier.height(8.dp))
                Text(
                    "An additional image provider. It works asynchronously — the app submits a " +
                            "request, then waits for the image (up to 4 minutes). Requires a token.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MedText
                )
                Spacer(Modifier.height(16.dp))

                OutlinedTextField(
                    value = modelScopeKeyInput,
                    onValueChange = { modelScopeKeyInput = it },
                    label = { Text("ModelScope Token", style = MaterialTheme.typography.bodyLarge) },
                    singleLine = true,
                    visualTransformation = if (showModelScopeKey) VisualTransformation.None else PasswordVisualTransformation(),
                    trailingIcon = {
                        TextButton(onClick = { showModelScopeKey = !showModelScopeKey }) {
                            Text(if (showModelScopeKey) "Hide" else "Show", style = MaterialTheme.typography.bodyMedium)
                        }
                    },
                    modifier  = Modifier.fillMaxWidth(),
                    shape     = RoundedCornerShape(16.dp),
                    textStyle = MaterialTheme.typography.bodyMedium
                )

                Spacer(Modifier.height(12.dp))

                val (msStatusText, msStatusColor) = when {
                    modelScopeStatus == "testing" -> "Testing…"                     to MedText
                    modelScopeStatus == "valid"   -> "✅ Token valid"                to SuccessGreen
                    modelScopeStatus == "invalid" -> "❌ Invalid token"             to ErrorRed
                    settings.modelScopeKeyValid   -> "✅ Token saved and verified"  to SuccessGreen
                    settings.modelScopeApiKey.isNotBlank() -> "⚠️ Not yet tested"   to MedText
                    else -> "No token set" to MedText
                }
                Text(msStatusText, style = MaterialTheme.typography.bodyMedium, color = msStatusColor)

                Spacer(Modifier.height(16.dp))

                Row(
                    modifier              = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Button(
                        onClick  = { viewModel.testModelScopeKey(modelScopeKeyInput) },
                        enabled  = modelScopeKeyInput.isNotBlank() && modelScopeStatus != "testing",
                        modifier = Modifier.weight(1f).height(56.dp),
                        shape    = RoundedCornerShape(16.dp),
                        colors   = ButtonDefaults.buttonColors(containerColor = ButtonPrimary)
                    ) {
                        Text(
                            if (modelScopeStatus == "testing") "Testing…" else "Test & Save",
                            style = MaterialTheme.typography.labelLarge
                        )
                    }
                    Button(
                        onClick  = { showModelScopeModels = true },
                        enabled  = settings.modelScopeKeyValid,
                        modifier = Modifier.weight(1f).height(56.dp),
                        shape    = RoundedCornerShape(16.dp),
                        colors   = ButtonDefaults.buttonColors(
                            containerColor         = ButtonSecondary,
                            disabledContainerColor = DisabledGray
                        )
                    ) {
                        Text("🧩 Models", style = MaterialTheme.typography.labelLarge)
                    }
                }

                Spacer(Modifier.height(32.dp))
                HorizontalDivider()
                Spacer(Modifier.height(24.dp))

                // ══════════════════════════════════════════════════════════════
                // SECTION 3 – Danger zone
                // ══════════════════════════════════════════════════════════════
                OutlinedButton(
                    onClick  = { showDeleteScreen = true },
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    shape    = RoundedCornerShape(16.dp),
                    colors   = ButtonDefaults.outlinedButtonColors(contentColor = ErrorRed),
                    border   = androidx.compose.foundation.BorderStroke(1.5.dp, ErrorRed)
                ) {
                    Icon(Icons.Default.Delete, contentDescription = null, tint = ErrorRed)
                    Spacer(Modifier.width(8.dp))
                    Text("Manage / Delete Players", style = MaterialTheme.typography.labelLarge, color = ErrorRed)
                }

                Spacer(Modifier.height(24.dp))
                HorizontalDivider()
                Spacer(Modifier.height(16.dp))

                Text(
                    "🔒 All API keys are stored only on this device and are never shared.",
                    style     = MaterialTheme.typography.bodyMedium,
                    color     = MedText,
                    textAlign = TextAlign.Center,
                    modifier  = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

// ── Small helpers ────────────────────────────────────────────────────────────

@Composable
private fun SectionHeader(text: String) {
    Text(text, style = MaterialTheme.typography.titleMedium, color = DeepPurple)
}

@Composable
private fun ImageModelToggle(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable { onCheckedChange(!checked) }
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Checkbox(checked = checked, onCheckedChange = onCheckedChange)
        Spacer(Modifier.width(4.dp))
        Text(label, style = MaterialTheme.typography.bodyMedium)
    }
}

// ── Gemini model selection screen ────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ModelSelectionScreen(viewModel: AppViewModel, onBack: () -> Unit) {
    val settings    by viewModel.settings.collectAsState()
    val allModels   = remember(settings.availableModelsJson) { viewModel.parseAvailableModels() }
    var showJsonDialog by remember { mutableStateOf(false) }

    val categories = listOf(
        Triple("questions",       "📐 Math Questions",      allModels),
        Triple("translation",     "🌍 Translation",          allModels),
        Triple("imagePrompt",     "✏️ Image Description",    allModels),
        Triple("imageGeneration", "🎨 Image Generation",     allModels)
    )

    val currentModels = mapOf(
        "questions"       to settings.modelQuestions,
        "translation"     to settings.modelTranslation,
        "imagePrompt"     to settings.modelImagePrompt,
        "imageGeneration" to settings.modelImageGeneration
    )

    if (showJsonDialog) {
        val prettyJson = remember(settings.availableModelsJson) {
            try { JSONObject(settings.availableModelsJson).toString(2) }
            catch (e: Exception) { settings.availableModelsJson }
        }
        AlertDialog(
            onDismissRequest = { showJsonDialog = false },
            title   = { Text("Available Models JSON", style = MaterialTheme.typography.titleMedium) },
            text    = {
                Box(modifier = Modifier.fillMaxWidth().heightIn(max = 400.dp).verticalScroll(rememberScrollState())) {
                    Text(
                        text  = prettyJson,
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontFamily = FontFamily.Monospace, fontSize = 11.sp
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
        Row(
            modifier              = Modifier.fillMaxWidth(),
            verticalAlignment     = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = DeepPurple)
                }
                Spacer(Modifier.width(8.dp))
                Text("Gemini Models 🤖", style = MaterialTheme.typography.titleLarge)
            }
            TextButton(onClick = { showJsonDialog = true }) {
                Text("View JSON", style = MaterialTheme.typography.bodyMedium, color = ButtonSecondary)
            }
        }

        Spacer(Modifier.height(8.dp))
        Text("Choose which Gemini model is used for each task.",
            style = MaterialTheme.typography.bodyMedium, color = MedText)
        Spacer(Modifier.height(24.dp))

        LazyColumn(verticalArrangement = Arrangement.spacedBy(20.dp)) {
            items(categories) { (key, label, models) ->
                var expanded by remember { mutableStateOf(false) }
                val current        = currentModels[key] ?: ""
                val currentDisplay = allModels.firstOrNull { it.name == current }?.displayName
                    ?: current.removePrefix("models/").ifBlank { "Not set" }
                val currentDesc = remember(current, settings.availableModelsJson) {
                    getModelDescription(current, settings.availableModelsJson)
                }

                Column(
                    modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp))
                        .background(CardBg).padding(16.dp)
                ) {
                    Text(label, style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(8.dp))
                    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = !expanded }) {
                        OutlinedTextField(
                            value        = currentDisplay,
                            onValueChange = {},
                            readOnly     = true,
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                            modifier     = Modifier.fillMaxWidth().menuAnchor(),
                            shape        = RoundedCornerShape(12.dp),
                            textStyle    = MaterialTheme.typography.bodyMedium
                        )
                        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false },
                            modifier = Modifier.heightIn(max = 280.dp)) {
                            models.forEach { model ->
                                val desc = remember(model.name, settings.availableModelsJson) {
                                    getModelDescription(model.name, settings.availableModelsJson)
                                }
                                DropdownMenuItem(
                                    text = {
                                        Column(modifier = Modifier.padding(vertical = 4.dp)) {
                                            Text(model.displayName, style = MaterialTheme.typography.bodyLarge)
                                            if (desc.isNotBlank()) {
                                                Spacer(Modifier.height(2.dp))
                                                Text(desc, style = MaterialTheme.typography.bodySmall,
                                                    color = MedText, lineHeight = 14.sp)
                                            }
                                        }
                                    },
                                    onClick = { viewModel.updateModelSelection(key, model.name); expanded = false }
                                )
                            }
                        }
                    }
                    if (currentDesc.isNotBlank()) {
                        Spacer(Modifier.height(8.dp))
                        Text(currentDesc, style = MaterialTheme.typography.bodySmall,
                            color = MedText, lineHeight = 16.sp)
                    }

                    // Only the image-generation slot can be toggled on/off for generation
                    if (key == "imageGeneration") {
                        Spacer(Modifier.height(8.dp))
                        ImageModelToggle(
                            label   = "Use this model for image generation",
                            checked = settings.enableGeminiImage,
                            onCheckedChange = { viewModel.setImageModelEnabled("gemini", it) }
                        )
                    }
                }
            }
        }
    }
}

// ── Pollinations model selection screen ──────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PollinationsModelScreen(viewModel: AppViewModel, onBack: () -> Unit) {
    val settings by viewModel.settings.collectAsState()

    // The 2 slot values come straight from settings
    val slotValues = listOf(
        settings.pollinationsModel1,
        settings.pollinationsModel2
    )
    val slotLabels = listOf(
        "🥇 Primary model (tried first)",
        "🥈 Second fallback"
    )

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
            Text("Pollinations Models 🌸", style = MaterialTheme.typography.titleLarge)
        }

        Spacer(Modifier.height(8.dp))
        Text(
            "When Gemini image generation fails, the app tries these models in order (1 → 2).",
            style = MaterialTheme.typography.bodyMedium,
            color = MedText
        )
        Spacer(Modifier.height(24.dp))

        slotValues.forEachIndexed { index, currentValue ->
            val slot = index + 1          // 1-based
            var expanded by remember { mutableStateOf(false) }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(CardBg)
                    .padding(16.dp)
            ) {
                Text(slotLabels[index], style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(8.dp))

                ExposedDropdownMenuBox(
                    expanded        = expanded,
                    onExpandedChange = { expanded = !expanded }
                ) {
                    OutlinedTextField(
                        value         = currentValue.ifBlank { "— not set —" },
                        onValueChange = {},
                        readOnly      = true,
                        trailingIcon  = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                        modifier      = Modifier.fillMaxWidth().menuAnchor(),
                        shape         = RoundedCornerShape(12.dp),
                        textStyle     = MaterialTheme.typography.bodyMedium
                    )
                    ExposedDropdownMenu(
                        expanded          = expanded,
                        onDismissRequest  = { expanded = false },
                        modifier          = Modifier.heightIn(max = 280.dp)
                    ) {
                        POLLINATIONS_MODELS.forEach { model ->
                            DropdownMenuItem(
                                text    = { Text(model, style = MaterialTheme.typography.bodyLarge) },
                                onClick = {
                                    viewModel.updatePollinationsModel(slot, model)
                                    expanded = false
                                }
                            )
                        }
                    }
                }

                Spacer(Modifier.height(8.dp))
                val enabled = when (slot) {
                    1 -> settings.enablePollinations1
                    else -> settings.enablePollinations2
                }
                ImageModelToggle(
                    label   = "Use this model",
                    checked = enabled,
                    onCheckedChange = { viewModel.setImageModelEnabled("pollinations$slot", it) }
                )
            }

            if (index < 1) Spacer(Modifier.height(20.dp))
        }

        Spacer(Modifier.height(32.dp))
        Text(
            "💡 Tip: keep different models in each slot so if one fails, the next is likely to succeed.",
            style     = MaterialTheme.typography.bodySmall,
            color     = MedText,
            textAlign = TextAlign.Center,
            modifier  = Modifier.fillMaxWidth()
        )
    }
}

// ── ModelScope model selection screen ────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ModelScopeModelScreen(viewModel: AppViewModel, onBack: () -> Unit) {
    val settings by viewModel.settings.collectAsState()
    var expanded by remember { mutableStateOf(false) }
    var showJsonDialog by remember { mutableStateOf(false) }

    if (showJsonDialog) {
        val prettyJson = remember(settings.modelScopeModelsJson) {
            try { JSONObject(settings.modelScopeModelsJson).toString(2) }
            catch (e: Exception) { settings.modelScopeModelsJson }
        }
        AlertDialog(
            onDismissRequest = { showJsonDialog = false },
            title   = { Text("ModelScope Models JSON", style = MaterialTheme.typography.titleMedium) },
            text    = {
                Column(Modifier.verticalScroll(rememberScrollState())) {
                    Text(
                        prettyJson.ifBlank { "No data — test the token first." },
                        style      = MaterialTheme.typography.bodySmall,
                        fontFamily = FontFamily.Monospace
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
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = DeepPurple)
            }
            Spacer(Modifier.width(8.dp))
            Text("ModelScope Model 🧩", style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f))
            TextButton(onClick = { showJsonDialog = true }) {
                Text("View JSON", style = MaterialTheme.typography.bodyMedium, color = ButtonSecondary)
            }
        }

        Spacer(Modifier.height(8.dp))
        Text(
            "Choose which ModelScope model to use for image generation.",
            style = MaterialTheme.typography.bodyMedium,
            color = MedText
        )
        Spacer(Modifier.height(24.dp))

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(CardBg)
                .padding(16.dp)
        ) {
            Text("Model", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(8.dp))

            ExposedDropdownMenuBox(
                expanded         = expanded,
                onExpandedChange = { expanded = !expanded }
            ) {
                OutlinedTextField(
                    value         = settings.modelScopeModel.ifBlank { "— not set —" },
                    onValueChange = {},
                    readOnly      = true,
                    trailingIcon  = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                    modifier      = Modifier.fillMaxWidth().menuAnchor(),
                    shape         = RoundedCornerShape(12.dp),
                    textStyle     = MaterialTheme.typography.bodyMedium
                )
                ExposedDropdownMenu(
                    expanded         = expanded,
                    onDismissRequest = { expanded = false },
                    modifier         = Modifier.heightIn(max = 280.dp)
                ) {
                    MODELSCOPE_MODELS.forEach { model ->
                        DropdownMenuItem(
                            text    = { Text(model, style = MaterialTheme.typography.bodyLarge) },
                            onClick = {
                                viewModel.updateModelScopeModel(model)
                                expanded = false
                            }
                        )
                    }
                }
            }

            Spacer(Modifier.height(8.dp))
            ImageModelToggle(
                label   = "Use ModelScope for image generation",
                checked = settings.enableModelScope,
                onCheckedChange = { viewModel.setImageModelEnabled("modelscope", it) }
            )
        }

        Spacer(Modifier.height(32.dp))
        Text(
            "💡 ModelScope is tried last, after Gemini and Pollinations, and only if enabled.",
            style     = MaterialTheme.typography.bodySmall,
            color     = MedText,
            textAlign = TextAlign.Center,
            modifier  = Modifier.fillMaxWidth()
        )
    }
}

// ── Delete players screen (unchanged logic) ──────────────────────────────────

@Composable
fun DeletePlayersScreen(
    viewModel: AppViewModel,
    onBack: () -> Unit,
    onAllDeleted: () -> Unit = {}
) {
    val players  by viewModel.players.collectAsState()
    val selected = remember { mutableStateListOf<String>() }
    var showConfirm by remember { mutableStateOf(false) }

    LaunchedEffect(players) { if (players.isEmpty()) onAllDeleted() }

    if (showConfirm) {
        AlertDialog(
            onDismissRequest = { showConfirm = false },
            title   = { Text("Delete players?", style = MaterialTheme.typography.titleMedium) },
            text    = {
                Text(
                    "This will permanently delete ${selected.size} player(s) and all their stars and gallery images.",
                    style = MaterialTheme.typography.bodyLarge
                )
            },
            confirmButton = {
                Button(
                    onClick = { viewModel.deletePlayers(selected.toList()); selected.clear(); showConfirm = false },
                    colors  = ButtonDefaults.buttonColors(containerColor = ErrorRed),
                    shape   = RoundedCornerShape(12.dp)
                ) { Text("Delete", style = MaterialTheme.typography.labelLarge) }
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
                                width  = if (isSelected) 2.dp else 0.dp,
                                color  = if (isSelected) ErrorRed else androidx.compose.ui.graphics.Color.Transparent,
                                shape  = RoundedCornerShape(16.dp)
                            )
                            .clickable { if (isSelected) selected.remove(player.id) else selected.add(player.id) }
                            .padding(16.dp),
                        verticalAlignment     = Alignment.CenterVertically,
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
                            checked         = isSelected,
                            onCheckedChange = { if (isSelected) selected.remove(player.id) else selected.add(player.id) },
                            colors          = CheckboxDefaults.colors(checkedColor = ErrorRed, uncheckedColor = MedText)
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(24.dp))
        Button(
            onClick  = { if (selected.isNotEmpty()) showConfirm = true },
            enabled  = selected.isNotEmpty(),
            modifier = Modifier.fillMaxWidth().height(60.dp),
            shape    = RoundedCornerShape(16.dp),
            colors   = ButtonDefaults.buttonColors(
                containerColor         = ErrorRed,
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