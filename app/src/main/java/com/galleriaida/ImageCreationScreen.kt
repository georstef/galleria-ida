package com.galleriaida.ui.screens

import android.util.Log
import android.content.Context
import androidx.compose.foundation.background
import com.galleriaida.AppConstants
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
import coil.compose.AsyncImage
import com.galleriaida.R
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
        val obj  = JSONObject(json)
        fun parseList(key: String): List<String> {
            val arr = obj.getJSONArray(key)
            return (0 until arr.length()).map { arr.getString(it) }
        }
        WordPool(
            characters = parseList("characters"),
            actions    = parseList("actions"),
            places     = parseList("places")
        )
    } catch (e: Exception) {
        WordPool(
            characters = listOf("bear", "rabbit", "fox", "dragon", "unicorn"),
            actions    = listOf("flying", "dancing", "exploring", "jumping", "swimming"),
            places     = listOf("forest", "castle", "beach", "mountain", "cave")
        )
    }
}

@Composable
fun ImageCreationScreen(
    viewModel: AppViewModel,
    onBack: () -> Unit,
    onImageCreated: () -> Unit
) {
    val context          = LocalContext.current
    val uiState          by viewModel.uiState.collectAsState()
    val player           by viewModel.currentPlayer.collectAsState()
    val uiStrings        by viewModel.uiStrings.collectAsState()
    val wordTranslations by viewModel.wordTranslations.collectAsState()

    // Fallback state lives in the ViewModel — safe to observe from any thread
    val isFallbackLoading    by viewModel.isFallbackLoading.collectAsState()
    val fallbackModelMessage by viewModel.fallbackModelMessage.collectAsState()

    // One-shot navigation event from the ViewModel
    LaunchedEffect(Unit) {
        viewModel.navigateToGallery.collect { onImageCreated() }
    }

    // Load full English word pool once
    val wordPool = remember { loadWordPool(context) }

    // Fixed random subset indices (stable across recompositions)
    val charIndices  = remember { wordPool.characters.indices.shuffled().take(4) }
    val actionIndices = remember { wordPool.actions.indices.shuffled().take(4) }
    val placeIndices  = remember { wordPool.places.indices.shuffled().take(4) }

    // Trigger translation on first entry
    LaunchedEffect(Unit) {
        viewModel.clearUiState()
        viewModel.ensureWordTranslations(
            characters = wordPool.characters,
            actions    = wordPool.actions,
            places     = wordPool.places
        )
    }

    // While translations are loading show logo + spinner
    if (wordTranslations == null) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                // App logo — same as PlayerLoadingScreen
                AsyncImage(
                    model              = R.mipmap.galleria_ida_logo,
                    contentDescription = "Logo",
                    modifier           = Modifier.size(120.dp)
                )
                Spacer(Modifier.height(32.dp))
                CircularProgressIndicator(color = ButtonPrimary, strokeWidth = 5.dp)
                Spacer(Modifier.height(16.dp))
                Text(
                    text      = "Preparing words…",
                    style     = MaterialTheme.typography.bodyLarge,
                    color     = MedText,
                    textAlign = TextAlign.Center
                )
            }
        }
        return
    }

    // Translated subset to show in the grids
    val shownCharactersLocal = charIndices.map   { wordTranslations!!.characters[it] }
    val shownActionsLocal    = actionIndices.map  { wordTranslations!!.actions[it] }
    val shownPlacesLocal     = placeIndices.map   { wordTranslations!!.places[it] }

    // Corresponding English words (same indices)
    val shownCharactersEn = charIndices.map   { wordPool.characters[it] }
    val shownActionsEn    = actionIndices.map  { wordPool.actions[it] }
    val shownPlacesEn     = placeIndices.map   { wordPool.places[it] }

    // Selection state — track index so we can look up both EN and Local
    var selectedCharIdx by remember { mutableStateOf<Int?>(null) }
    var selectedActionIdx by remember { mutableStateOf<Int?>(null) }
    var selectedPlaceIdx  by remember { mutableStateOf<Int?>(null) }

    val allSelected = selectedCharIdx != null && selectedActionIdx != null && selectedPlaceIdx != null
    val stars       = player?.stars ?: 0
    val canAfford   = (stars >= 100) || (player?.name == AppConstants.DEV_PLAYER_NAME)

    val isLoading    = uiState is UiState.Loading || isFallbackLoading
    val errorMessage = (uiState as? UiState.Error)?.message

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
    ) {
        Column(modifier = Modifier.fillMaxSize()) {

            // ── Top bar ──────────────────────────────────────────────────────
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
                    Text("$stars", style = MaterialTheme.typography.titleMedium, color = DeepPurple)
                }
                Spacer(Modifier.width(48.dp))
            }

            // ── Loading state ────────────────────────────────────────────────
            if (isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(color = ButtonPrimary, strokeWidth = 5.dp)
                        Spacer(Modifier.height(20.dp))
                        Text(
                            text = if (isFallbackLoading) "✨ $fallbackModelMessage"
                            else uiStrings.imageCreatingMsg,
                            style     = MaterialTheme.typography.bodyLarge,
                            textAlign = TextAlign.Center,
                            color     = MedText
                        )
                    }
                }
            } else {
                // ── Main content ─────────────────────────────────────────────
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
                        style     = MaterialTheme.typography.titleLarge,
                        textAlign = TextAlign.Center
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        uiStrings.imageSubtitle,
                        style     = MaterialTheme.typography.bodyMedium,
                        color     = MedText,
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
                                style     = MaterialTheme.typography.bodyMedium,
                                color     = ErrorRed,
                                textAlign = TextAlign.Center,
                                modifier  = Modifier.fillMaxWidth()
                            )
                        }
                    }

                    Spacer(Modifier.height(24.dp))

                    WordCategory(
                        title        = uiStrings.imageCategoryCharacter,
                        words        = shownCharactersLocal,
                        selectedIdx  = selectedCharIdx,
                        color        = SoftPurple,
                        onSelect     = { selectedCharIdx = it }
                    )
                    Spacer(Modifier.height(20.dp))
                    WordCategory(
                        title        = uiStrings.imageCategoryAction,
                        words        = shownActionsLocal,
                        selectedIdx  = selectedActionIdx,
                        color        = SkyBlue,
                        onSelect     = { selectedActionIdx = it }
                    )
                    Spacer(Modifier.height(20.dp))
                    WordCategory(
                        title        = uiStrings.imageCategoryPlace,
                        words        = shownPlacesLocal,
                        selectedIdx  = selectedPlaceIdx,
                        color        = MintGreen,
                        onSelect     = { selectedPlaceIdx = it }
                    )

                    Spacer(Modifier.height(32.dp))

                    if (!canAfford) {
                        Text(
                            uiStrings.imageNeedStars.format(stars),
                            style     = MaterialTheme.typography.bodyMedium,
                            color     = MedText,
                            textAlign = TextAlign.Center
                        )
                        Spacer(Modifier.height(8.dp))
                    }

                    Button(
                        onClick = {
                            if (!allSelected || !canAfford) return@Button

                            val charEn    = shownCharactersEn[selectedCharIdx!!]
                            val actionEn  = shownActionsEn[selectedActionIdx!!]
                            val placeEn   = shownPlacesEn[selectedPlaceIdx!!]
                            val charLocal = shownCharactersLocal[selectedCharIdx!!]
                            val actLocal  = shownActionsLocal[selectedActionIdx!!]
                            val plcLocal  = shownPlacesLocal[selectedPlaceIdx!!]

                            viewModel.generateGalleryImage(
                                characterEn    = charEn,
                                actionEn       = actionEn,
                                placeEn        = placeEn,
                                characterLocal = charLocal,
                                actionLocal    = actLocal,
                                placeLocal     = plcLocal
                            )
                        },
                        enabled = allSelected && canAfford,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(64.dp),
                        shape  = RoundedCornerShape(20.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor         = ButtonPrimary,
                            disabledContainerColor = DisabledGray
                        )
                    ) {
                        Text(
                            when {
                                !canAfford   -> uiStrings.imageButtonNeedStars
                                !allSelected -> uiStrings.imageButtonPickAll
                                else         -> uiStrings.imageButtonCreate
                            },
                            style = MaterialTheme.typography.labelLarge
                        )
                    }
                }
            }
        }
    }
}

// ── WordCategory — now index-based ───────────────────────────────────────────

@Composable
fun WordCategory(
    title: String,
    words: List<String>,
    selectedIdx: Int?,
    color: androidx.compose.ui.graphics.Color,
    onSelect: (Int) -> Unit
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
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf(0..1, 2..3).forEach { range ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    range.forEach { idx ->
                        val word = words.getOrNull(idx) ?: return@forEach
                        val isSelected = idx == selectedIdx
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
                                .clickable { onSelect(idx) }
                                .padding(vertical = 14.dp, horizontal = 12.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text      = word,
                                style     = MaterialTheme.typography.bodyMedium,
                                color     = if (isSelected) White else DarkText,
                                textAlign = TextAlign.Center,
                                maxLines  = 2
                            )
                        }
                    }
                }
            }
        }
    }
}