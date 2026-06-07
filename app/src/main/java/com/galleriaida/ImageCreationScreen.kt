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
import org.json.JSONObject

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

@Composable
fun ImageCreationScreen(
    viewModel: AppViewModel,
    onBack: () -> Unit,
    onImageCreated: () -> Unit
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()
    val player by viewModel.currentPlayer.collectAsState()

    // Load word pool and pick 4 random from each category once
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

    val isLoading = uiState is UiState.Loading
    val errorMessage = (uiState as? UiState.Error)?.message

    // Clear error on enter
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
                // Loading state
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(color = ButtonPrimary, strokeWidth = 5.dp)
                        Spacer(Modifier.height(20.dp))
                        Text(
                            "Creating your image... 🎨\nThis may take a moment",
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
                        "Create your image! 🎨",
                        style = MaterialTheme.typography.titleLarge,
                        textAlign = TextAlign.Center
                    )

                    Spacer(Modifier.height(6.dp))

                    Text(
                        "Pick one from each group",
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

                    // Characters
                    WordCategory(
                        title = "🦸 Character",
                        words = shownCharacters,
                        selected = selectedCharacter,
                        color = SoftPurple,
                        onSelect = { selectedCharacter = it }
                    )

                    Spacer(Modifier.height(20.dp))

                    // Actions
                    WordCategory(
                        title = "⚡ Action",
                        words = shownActions,
                        selected = selectedAction,
                        color = SkyBlue,
                        onSelect = { selectedAction = it }
                    )

                    Spacer(Modifier.height(20.dp))

                    // Places
                    WordCategory(
                        title = "🗺️ Place",
                        words = shownPlaces,
                        selected = selectedPlace,
                        color = MintGreen,
                        onSelect = { selectedPlace = it }
                    )

                    Spacer(Modifier.height(32.dp))

                    // Summary of selection
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
                            "You need 100 ⭐ to create an image (you have $stars)",
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
                            Log.d("GALLERIA_AI", "allSelected=$allSelected canAfford=$canAfford")
                            Log.d("GALLERIA_AI", "character=$selectedCharacter action=$selectedAction place=$selectedPlace")
                            if (allSelected && canAfford) {
                                viewModel.generateGalleryImage(
                                    character = selectedCharacter!!,
                                    action = selectedAction!!,
                                    place = selectedPlace!!,
                                    onComplete = { success ->
                                        if (success) onImageCreated()
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
                                !canAfford -> "Need 100 ⭐"
                                !allSelected -> "Pick one from each group"
                                else -> "Create Image! 🎨 (100 ⭐)"
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
