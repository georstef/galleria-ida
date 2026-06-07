package com.galleriaida.ui.screens

import android.util.Log
import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.galleriaida.ui.theme.*
import com.galleriaida.viewmodel.AppViewModel
import com.galleriaida.viewmodel.UiState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.net.URL
import java.net.URLEncoder

data class WordPool(
    val characters: List<String>,
    val actions: List<String>,
    val places: List<String>
)

fun loadWordPool(context: Context): WordPool {
    return try {
        val json = context.assets.open("words.json").bufferedReader().use { it.readText() }
        val obj = JSONObject(json)
        fun parseList(key: String): List<String> {
            val arr = obj.getJSONArray(key)
            return (0 until arr.length()).map { arr.getString(it) }
        }
        WordPool(
            characters = parseList("characters"),
            actions = parseList("actions"),
            places = parseList("places")
        )
    } catch (e: Exception) {
        WordPool(
            characters = listOf("bear", "rabbit", "fox", "dragon", "unicorn"),
            actions = listOf("flying", "dancing", "exploring", "jumping", "swimming"),
            places = listOf("forest", "castle", "beach", "mountain", "cave")
        )
    }
}

fun loadPollinationsKey(context: Context): String? {
    return try {
        Log.d("GALLERIA_AI_BACKUP", "Attempting to open assets/pollinations.ai.keystore...")
        val key = context.assets.open("pollinations.ai.keystore").bufferedReader().use { it.readLine()?.trim() }
        if (key.isNullOrEmpty()) {
            Log.e("GALLERIA_AI_BACKUP", "Keystore file exists but returned an empty string!")
        } else {
            Log.d("GALLERIA_AI_BACKUP", "Successfully loaded key prefix: ${key.take(6)}...")
        }
        key
    } catch (e: Exception) {
        Log.e("GALLERIA_AI_BACKUP", "Failed critical read on pollinations.ai.keystore", e)
        null
    }
}

suspend fun downloadPollinationsImage(
    context: Context,
    englishPrompt: String,     // CRITICAL: Always pass the phrase_en here
    playerPrompt: String,      // Pass the player's language phrase for history logging
    apiKey: String
): File? = withContext(Dispatchers.IO) {
    Log.d("GALLERIA_AI_BACKUP", "==================================================")
    Log.d("GALLERIA_AI_BACKUP", ">>> STARTING DUAL-LANGUAGE PIPELINE")
    Log.d("GALLERIA_AI_BACKUP", "Local Display Phrase: $playerPrompt")

    try {
        // Sanitize the English prompt for the URL query network layer
        val sanitizedPrompt = englishPrompt.replace("\"", " ").replace("'", " ").trim()
        val encodedPrompt = URLEncoder.encode(sanitizedPrompt, "UTF-8").replace("+", "%20")

        val uniqueSeed = (1..999999).random()

        // This targets the exact working API route with the English version
        val urlString = "https://gen.pollinations.ai/image/$encodedPrompt?width=512&height=896&seed=$uniqueSeed&nologo=true&model=flux"
        Log.d("GALLERIA_AI_BACKUP", "TARGET ENGLISH GENERATION URL: $urlString")

        val url = URL(urlString)
        val targetFile = File(context.cacheDir, "generated_image_${System.currentTimeMillis()}.jpg")

        val connection = url.openConnection() as java.net.HttpURLConnection
        connection.connectTimeout = 30000
        connection.readTimeout = 30000
        connection.requestMethod = "GET"

        if (apiKey.isNotEmpty()) {
            connection.setRequestProperty("Authorization", "Bearer $apiKey")
        }

        connection.connect()
        val responseCode = connection.responseCode
        Log.d("GALLERIA_AI_BACKUP", "HTTP RESPONSE: $responseCode")

        if (responseCode == 200) {
            connection.inputStream.use { input ->
                targetFile.outputStream().use { output ->
                    input.copyTo(output)
                }
            }
            connection.disconnect()
            if (targetFile.exists() && targetFile.length() > 0) {
                Log.d("GALLERIA_AI_BACKUP", "SUCCESS: Image generated successfully using English prompt.")
                return@withContext targetFile
            }
        } else {
            val errorResponse = connection.errorStream?.bufferedReader()?.use { it.readText() } ?: "No error body text"
            Log.e("GALLERIA_AI_BACKUP", "FAIL: Code $responseCode. Details:\n$errorResponse")
            connection.disconnect()
        }
        return@withContext null
    } catch (e: Exception) {
        Log.e("GALLERIA_AI_BACKUP", "FAIL: Pipeline execution error", e)
        null
    } finally {
        Log.d("GALLERIA_AI_BACKUP", "==================================================")
    }
}

@Composable
fun ImageCreationScreen(
    viewModel: AppViewModel,
    onBack: () -> Unit,
    onImageCreated: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val uiState by viewModel.uiState.collectAsState()
    val player by viewModel.currentPlayer.collectAsState()
    val uiStrings by viewModel.uiStrings.collectAsState()

    val wordPool = remember { loadWordPool(context) }
    val shownCharacters = remember { wordPool.characters.shuffled().take(4) }
    val shownActions = remember { wordPool.actions.shuffled().take(4) }
    val shownPlaces = remember { wordPool.places.shuffled().take(4) }

    var selectedCharacter by remember { mutableStateOf<String?>(null) }
    var selectedAction by remember { mutableStateOf<String?>(null) }
    var selectedPlace by remember { mutableStateOf<String?>(null) }

    val allSelected = selectedCharacter != null && selectedAction != null && selectedPlace != null
    val stars = player?.stars ?: 0
    val canAfford = (stars >= 100) || (player?.name == "George S.")

    var isFallbackLoading by remember { mutableStateOf(false) }
    val isLoading = uiState is UiState.Loading || isFallbackLoading
    val errorMessage = (uiState as? UiState.Error)?.message

    LaunchedEffect(Unit) { viewModel.clearUiState() }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
    ) {
        Column(modifier = Modifier.fillMaxSize()) {

            // Top bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                IconButton(onClick = onBack, enabled = !isLoading) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = DeepPurple)
                }
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(40.dp))
                        .background(LemonYellow)
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("⭐", style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.width(6.dp))
                    Text(
                        "$stars",
                        style = MaterialTheme.typography.titleMedium,
                        color = DeepPurple
                    )
                }
                Spacer(Modifier.width(48.dp))
            }

            if (isLoading) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(color = ButtonPrimary, strokeWidth = 5.dp)
                        Spacer(Modifier.height(20.dp))
                        Text(
                            if (isFallbackLoading) "Gemini busy... Processing exact prompt through backup creative engine ✨" else uiStrings.imageCreatingMsg,
                            style = MaterialTheme.typography.bodyLarge,
                            textAlign = TextAlign.Center,
                            color = MedText
                        )
                    }
                }
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 24.dp)
                        .padding(bottom = 32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        uiStrings.imageTitle,
                        style = MaterialTheme.typography.titleLarge,
                        textAlign = TextAlign.Center
                    )

                    Spacer(Modifier.height(6.dp))

                    Text(
                        uiStrings.imageSubtitle,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MedText,
                        textAlign = TextAlign.Center
                    )

                    if (errorMessage != null) {
                        Spacer(Modifier.height(12.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(PeachOrange)
                                .padding(12.dp)
                        ) {
                            Text(
                                errorMessage,
                                style = MaterialTheme.typography.bodyMedium,
                                color = ErrorRed,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }

                    Spacer(Modifier.height(24.dp))

                    WordCategory(
                        title = uiStrings.imageCategoryCharacter,
                        words = shownCharacters,
                        selected = selectedCharacter,
                        color = SoftPurple,
                        onSelect = { selectedCharacter = it }
                    )

                    Spacer(Modifier.height(20.dp))

                    WordCategory(
                        title = uiStrings.imageCategoryAction,
                        words = shownActions,
                        selected = selectedAction,
                        color = SkyBlue,
                        onSelect = { selectedAction = it }
                    )

                    Spacer(Modifier.height(20.dp))

                    WordCategory(
                        title = uiStrings.imageCategoryPlace,
                        words = shownPlaces,
                        selected = selectedPlace,
                        color = MintGreen,
                        onSelect = { selectedPlace = it }
                    )

                    Spacer(Modifier.height(32.dp))

                    if (allSelected) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(16.dp))
                                .background(LemonYellow)
                                .padding(16.dp)
                        ) {
                            Text(
                                "✨ $selectedCharacter + $selectedAction + $selectedPlace",
                                style = MaterialTheme.typography.titleMedium,
                                color = DeepPurple,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                        Spacer(Modifier.height(20.dp))
                    }

                    if (!canAfford) {
                        Text(
                            uiStrings.imageNeedStars.format(stars),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MedText,
                            textAlign = TextAlign.Center
                        )
                        Spacer(Modifier.height(8.dp))
                    }

                    Log.d("GALLERIA_AI", "Button state: allSelected=$allSelected canAfford=$canAfford enabled=${allSelected && canAfford}")
                    Button(
                        onClick = {
                            Log.d("GALLERIA_AI", "=== CREATE BUTTON TAPPED ===")
                            if (allSelected && canAfford) {
                                viewModel.generateGalleryImage(
                                    character = selectedCharacter!!,
                                    action = selectedAction!!,
                                    place = selectedPlace!!,
                                    onComplete = { success, exactGeminiPrompt ->
                                        if (success) {
                                            Log.d("GALLERIA_AI", "Gemini Image Model handled request directly.")
                                            onImageCreated()
                                        } else {
                                            Log.d("GALLERIA_AI", "Gemini Image Model failed/quota exceeded. Mirroring exact prompt over to Pollinations fallback engine...")

                                            val apiKey = loadPollinationsKey(context)
                                            if (apiKey.isNullOrEmpty()) {
                                                Log.e("GALLERIA_AI_BACKUP", "Execution aborted. Valid key was not found in keystore file.")
                                                return@generateGalleryImage
                                            }

                                            scope.launch {
                                                isFallbackLoading = true

                                                // CHANGED: We now pass four parameters to match the dual-language downloader layout.
                                                // exactGeminiPrompt holds the sanitized english text forwarded from our ViewModel.
                                                val localBackupLabel = "$selectedCharacter $selectedAction $selectedPlace"
                                                val downloadedFile = downloadPollinationsImage(
                                                    context = context,
                                                    englishPrompt = exactGeminiPrompt,
                                                    playerPrompt = localBackupLabel,
                                                    apiKey = apiKey
                                                )
                                                isFallbackLoading = false

                                                if (downloadedFile != null && downloadedFile.exists()) {
                                                    Log.d("GALLERIA_AI_BACKUP", "Success! Same prompt processed successfully via Pollinations. Saved at: ${downloadedFile.absolutePath}")

                                                    // CHANGED: Match the updated dual phrase parameters required by our repository layer
                                                    viewModel.registerFallbackImage(
                                                        downloadedFile = downloadedFile,
                                                        englishPhrase = exactGeminiPrompt,
                                                        playerPhrase = localBackupLabel,
                                                        character = selectedCharacter!!,
                                                        action = selectedAction!!,
                                                        place = selectedPlace!!
                                                    )

                                                    onImageCreated()
                                                } else {
                                                    Log.e("GALLERIA_AI_BACKUP", "Backup engine failed completely. Image could not be captured.")
                                                }
                                            }
                                        }
                                    }
                                )
                            }
                        },
                        enabled = allSelected && canAfford,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(64.dp),
                        shape = RoundedCornerShape(20.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = ButtonPrimary,
                            disabledContainerColor = DisabledGray
                        )
                    ) {
                        Text(
                            when {
                                !canAfford -> uiStrings.imageButtonNeedStars
                                !allSelected -> uiStrings.imageButtonPickAll
                                else -> uiStrings.imageButtonCreate
                            },
                            style = MaterialTheme.typography.labelLarge
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun WordCategory(
    title: String,
    words: List<String>,
    selected: String?,
    color: androidx.compose.ui.graphics.Color,
    onSelect: (String) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(color.copy(alpha = 0.3f))
            .padding(16.dp)
    ) {
        Text(title, style = MaterialTheme.typography.titleMedium, color = DeepPurple)
        Spacer(Modifier.height(12.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            words.forEach { word ->
                val isSelected = word == selected
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (isSelected) DeepPurple else White)
                        .border(
                            width = if (isSelected) 0.dp else 1.dp,
                            color = if (isSelected) DeepPurple else DisabledGray,
                            shape = RoundedCornerShape(12.dp)
                        )
                        .clickable { onSelect(word) }
                        .padding(vertical = 10.dp, horizontal = 4.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = word,
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (isSelected) White else DarkText,
                        textAlign = TextAlign.Center,
                        maxLines = 2
                    )
                }
            }
        }
    }
}