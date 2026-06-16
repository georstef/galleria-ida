package com.galleriaida.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.galleriaida.data.GalleryItem
import com.galleriaida.ui.theme.*
import com.galleriaida.viewmodel.AppViewModel
import java.io.File
import kotlin.random.Random

private const val TOTAL_ROUNDS = 5
private const val PLAY_COST    = 10

enum class FillBlankDifficulty(val blankRatio: Float) {
    EASY(0.3f),
    MEDIUM(0.5f),
    HARD(0.7f)
}

private data class BlankSlot(
    val char: Char,         // the actual character that belongs here (letters only have entries)
    val isBlank: Boolean,   // true if this position needs to be filled by the player
    var filledWith: Char? = null
)

private data class FillBlankRound(
    val image: GalleryItem,
    val title: String,
    val slots: List<BlankSlot>,
    val letterTray: List<Char>   // shuffled tray of letters needed to fill the blanks
)

private fun buildRound(image: GalleryItem, difficulty: FillBlankDifficulty): FillBlankRound {
    val title = image.titleLocal.ifBlank { image.titleEn }
    val letterIndices = title.indices.filter { title[it].isLetter() }
    val blankCount = (letterIndices.size * difficulty.blankRatio).toInt().coerceAtLeast(1)
    val blankIndices = letterIndices.shuffled(Random(System.nanoTime())).take(blankCount).toSet()

    val slots = title.mapIndexed { i, c ->
        BlankSlot(char = c, isBlank = i in blankIndices)
    }
    val tray = blankIndices.map { title[it] }.shuffled(Random(System.nanoTime() + 1))

    return FillBlankRound(image = image, title = title, slots = slots, letterTray = tray)
}

@Composable
fun FillTheBlankScreen(
    viewModel: AppViewModel,
    onBack: () -> Unit
) {
    val player    by viewModel.currentPlayer.collectAsState()
    val gallery   by viewModel.gallery.collectAsState()
    val uiStrings by viewModel.uiStrings.collectAsState()

    val playerImages = remember(gallery, player) {
        gallery.filter { it.playerId == player?.id }
    }

    val isDev = player?.name?.trim()?.equals(com.galleriaida.AppConstants.DEV_PLAYER_NAME, ignoreCase = true) ?: false

    var phase by remember { mutableStateOf(FillBlankPhase.SETUP) }
    var difficulty by remember { mutableStateOf(FillBlankDifficulty.EASY) }
    var roundImages by remember { mutableStateOf<List<GalleryItem>>(emptyList()) }
    var correctCount by remember { mutableStateOf(0) }

    when (phase) {
        FillBlankPhase.SETUP -> {
            FillBlankSetupScreen(
                uiStrings   = uiStrings,
                difficulty  = difficulty,
                stars       = player?.stars ?: 0,
                isDev       = isDev,
                imageCount  = playerImages.size,
                onDifficultySelected = { difficulty = it },
                onPlay = {
                    val spent = viewModel.spendStars(PLAY_COST)
                    if (spent) {
                        roundImages = playerImages.shuffled().take(TOTAL_ROUNDS)
                        correctCount = 0
                        phase = FillBlankPhase.PLAYING
                    }
                },
                onBack = onBack
            )
        }
        FillBlankPhase.PLAYING -> {
            FillBlankGameScreen(
                images      = roundImages,
                difficulty  = difficulty,
                uiStrings   = uiStrings,
                onRoundResult = { correct -> if (correct) correctCount++ },
                onAllRoundsDone = { phase = FillBlankPhase.SUMMARY },
                onExit = onBack
            )
        }
        FillBlankPhase.SUMMARY -> {
            FillBlankSummaryScreen(
                images      = roundImages,
                correctCount = correctCount,
                uiStrings   = uiStrings,
                onClose     = onBack
            )
        }
    }
}

private enum class FillBlankPhase { SETUP, PLAYING, SUMMARY }

// ── Setup screen ──────────────────────────────────────────────────────────────

@Composable
private fun FillBlankSetupScreen(
    uiStrings: com.galleriaida.ui.UiStrings,
    difficulty: FillBlankDifficulty,
    stars: Int,
    isDev: Boolean,
    imageCount: Int,
    onDifficultySelected: (FillBlankDifficulty) -> Unit,
    onPlay: () -> Unit,
    onBack: () -> Unit
) {
    val canAfford     = isDev || stars >= PLAY_COST
    val enoughImages  = imageCount >= TOTAL_ROUNDS

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = DeepPurple)
            }
            Text(
                text      = uiStrings.fillBlankTitle,
                style     = MaterialTheme.typography.titleLarge,
                modifier  = Modifier.weight(1f),
                textAlign = TextAlign.Center
            )
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(40.dp))
                    .background(LemonYellow)
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Text("⭐ $stars", style = MaterialTheme.typography.bodyMedium, color = DeepPurple)
            }
            Spacer(Modifier.width(8.dp))
        }

        Spacer(Modifier.weight(1f))

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp)
        ) {
            Text("🔡", fontSize = 64.sp)
            Spacer(Modifier.height(16.dp))
            Text(
                text  = "${TOTAL_ROUNDS} rounds — guess the title to reveal each hidden picture!",
                style = MaterialTheme.typography.bodyLarge,
                color = MedText,
                textAlign = TextAlign.Center
            )
        }

        Spacer(Modifier.height(32.dp))

        Text(
            text       = uiStrings.fillBlankSelectDifficulty,
            style      = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Bold,
            color      = DeepPurple,
            modifier   = Modifier.padding(horizontal = 24.dp)
        )
        Spacer(Modifier.height(10.dp))
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            FillBlankDifficulty.values().forEach { d ->
                val selected = d == difficulty
                val label = when (d) {
                    FillBlankDifficulty.EASY   -> uiStrings.fillBlankEasy
                    FillBlankDifficulty.MEDIUM -> uiStrings.fillBlankMedium
                    FillBlankDifficulty.HARD   -> uiStrings.fillBlankHard
                }
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(16.dp))
                        .background(if (selected) ButtonSecondary else CardBg)
                        .clickable { onDifficultySelected(d) }
                        .padding(vertical = 14.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text  = label,
                        style = MaterialTheme.typography.bodySmall,
                        color = if (selected) Color.White else DeepPurple,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        Spacer(Modifier.height(24.dp))

        if (!enoughImages) {
            Text(
                text      = uiStrings.fillBlankNotEnoughImages,
                style     = MaterialTheme.typography.bodyMedium,
                color     = MaterialTheme.colorScheme.error,
                textAlign = TextAlign.Center,
                modifier  = Modifier.fillMaxWidth().padding(horizontal = 24.dp)
            )
            Spacer(Modifier.height(16.dp))
        }

        Button(
            onClick  = onPlay,
            enabled  = canAfford && enoughImages,
            modifier = Modifier
                .fillMaxWidth()
                .height(60.dp)
                .padding(horizontal = 24.dp),
            shape  = RoundedCornerShape(20.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor         = ButtonSecondary,
                disabledContainerColor = DisabledGray
            )
        ) {
            Text(
                text  = if (!canAfford) uiStrings.fillBlankNotEnoughStars else uiStrings.fillBlankPlay,
                style = MaterialTheme.typography.labelLarge
            )
        }

        Spacer(Modifier.height(24.dp))
    }
}

// ── Game screen ───────────────────────────────────────────────────────────────

@Composable
private fun FillBlankGameScreen(
    images: List<GalleryItem>,
    difficulty: FillBlankDifficulty,
    uiStrings: com.galleriaida.ui.UiStrings,
    onRoundResult: (Boolean) -> Unit,
    onAllRoundsDone: () -> Unit,
    onExit: () -> Unit
) {
    var roundIndex by remember { mutableStateOf(0) }
    var showExitDialog by remember { mutableStateOf(false) }
    var revealed by remember { mutableStateOf(false) }
    var wrongFlash by remember { mutableStateOf(false) }

    val currentImage = images.getOrNull(roundIndex)

    var round by remember(roundIndex, currentImage) {
        mutableStateOf(currentImage?.let { buildRound(it, difficulty) })
    }
    // tray slot -> consumed flag, parallel to round.letterTray
    var trayUsed by remember(roundIndex) { mutableStateOf(List(round?.letterTray?.size ?: 0) { false }) }
    // ordered list of blank slot positions in title order
    val blankPositions = remember(round) { round?.slots?.indices?.filter { round!!.slots[it].isBlank } ?: emptyList() }
    var nextBlankCursor by remember(roundIndex) { mutableStateOf(0) }

    BackHandler { showExitDialog = true }

    if (currentImage == null || round == null) {
        // Safety: nothing to show
        return
    }

    val isComplete = blankPositions.isNotEmpty() && blankPositions.all { round!!.slots[it].filledWith != null }
    val isCorrect = isComplete && blankPositions.all { round!!.slots[it].filledWith == round!!.slots[it].char }

    LaunchedEffect(isComplete) {
        if (isComplete) {
            if (isCorrect) {
                revealed = true
            } else {
                wrongFlash = true
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        // Top bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = { showExitDialog = true }) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = DeepPurple)
            }
            Text(
                text      = uiStrings.fillBlankRoundCounter.format(roundIndex + 1, TOTAL_ROUNDS),
                style     = MaterialTheme.typography.titleMedium,
                modifier  = Modifier.weight(1f),
                textAlign = TextAlign.Center,
                color     = DeepPurple
            )
            Spacer(Modifier.width(48.dp))
        }

        Spacer(Modifier.height(8.dp))

        // Hidden image placeholder / revealed image
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(horizontal = 24.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(CardBg),
            contentAlignment = Alignment.Center
        ) {
            if (revealed) {
                val imageModel = remember(currentImage.imageUrl) {
                    val f = File(currentImage.imageUrl); if (f.exists()) f else currentImage.imageUrl
                }
                AsyncImage(
                    model              = imageModel,
                    contentDescription = round!!.title,
                    contentScale       = ContentScale.Fit,
                    modifier           = Modifier.fillMaxSize().clip(RoundedCornerShape(20.dp))
                )
            } else {
                Text("🖼️", fontSize = 72.sp, modifier = Modifier.alpha(0.3f))
            }
        }

        Spacer(Modifier.height(20.dp))

        if (revealed) {
            // Result + next button
            Text(
                text       = uiStrings.fillBlankCorrect,
                style      = MaterialTheme.typography.titleLarge,
                color      = DeepPurple,
                textAlign  = TextAlign.Center,
                modifier   = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(16.dp))
            Button(
                onClick = {
                    onRoundResult(true)
                    if (roundIndex + 1 >= images.size) {
                        onAllRoundsDone()
                    } else {
                        roundIndex++
                        revealed = false
                        wrongFlash = false
                        nextBlankCursor = 0
                    }
                },
                modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp).height(56.dp),
                shape    = RoundedCornerShape(16.dp),
                colors   = ButtonDefaults.buttonColors(containerColor = ButtonSecondary)
            ) {
                Text(uiStrings.fillBlankNext, style = MaterialTheme.typography.labelLarge)
            }
            Spacer(Modifier.height(24.dp))
        } else {
            // Title slots
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                round!!.slots.forEach { slot ->
                    if (slot.char == ' ') {
                        Spacer(Modifier.width(8.dp))
                    } else {
                        val displayChar = if (slot.isBlank) (slot.filledWith?.toString() ?: "_") else slot.char.toString()
                        Box(
                            modifier = Modifier
                                .size(width = 28.dp, height = 36.dp)
                                .clip(RoundedCornerShape(6.dp))
                                .background(
                                    if (slot.isBlank) {
                                        if (wrongFlash) MaterialTheme.colorScheme.error.copy(alpha = 0.2f)
                                        else SoftPurple
                                    } else Color.Transparent
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text  = displayChar,
                                style = MaterialTheme.typography.titleMedium,
                                color = DeepPurple,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            if (wrongFlash) {
                Spacer(Modifier.height(8.dp))
                Text(
                    text      = uiStrings.fillBlankWrong,
                    style     = MaterialTheme.typography.bodyMedium,
                    color     = MaterialTheme.colorScheme.error,
                    textAlign = TextAlign.Center,
                    modifier  = Modifier.fillMaxWidth()
                )
            }

            Spacer(Modifier.height(28.dp))

            // Letter tray
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.CenterHorizontally)
            ) {
                round!!.letterTray.forEachIndexed { i, letter ->
                    val used = trayUsed.getOrElse(i) { false }
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (used) DisabledGray.copy(alpha = 0.3f) else ButtonSecondary)
                            .clickable(enabled = !used && nextBlankCursor < blankPositions.size) {
                                val targetSlotIndex = blankPositions[nextBlankCursor]
                                val updatedSlots = round!!.slots.toMutableList()
                                updatedSlots[targetSlotIndex] = updatedSlots[targetSlotIndex].copy(filledWith = letter)
                                round = round!!.copy(slots = updatedSlots)
                                trayUsed = trayUsed.toMutableList().also { it[i] = true }
                                nextBlankCursor++
                                wrongFlash = false
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text  = letter.toString(),
                            style = MaterialTheme.typography.titleMedium,
                            color = if (used) MedText else Color.White,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            // Undo button
            TextButton(
                onClick = {
                    if (nextBlankCursor > 0) {
                        nextBlankCursor--
                        val targetSlotIndex = blankPositions[nextBlankCursor]
                        val clearedChar = round!!.slots[targetSlotIndex].filledWith
                        val updatedSlots = round!!.slots.toMutableList()
                        updatedSlots[targetSlotIndex] = updatedSlots[targetSlotIndex].copy(filledWith = null)
                        round = round!!.copy(slots = updatedSlots)
                        // free up the matching tray tile (last matching used one)
                        trayUsed = trayUsed.toMutableList().also { list ->
                            val idxToFree = round!!.letterTray.indices.lastOrNull { idx ->
                                list[idx] && round!!.letterTray[idx] == clearedChar
                            }
                            if (idxToFree != null) list[idxToFree] = false
                        }
                        wrongFlash = false
                    }
                },
                enabled  = nextBlankCursor > 0,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            ) {
                Text(uiStrings.fillBlankUndo, color = DeepPurple)
            }

            Spacer(Modifier.height(16.dp))
        }
    }

    if (showExitDialog) {
        AlertDialog(
            onDismissRequest = { showExitDialog = false },
            title = { Text(uiStrings.fillBlankExitTitle, fontWeight = FontWeight.Bold) },
            text  = { Text(uiStrings.fillBlankExitBody) },
            confirmButton = {
                TextButton(onClick = {
                    showExitDialog = false
                    onExit()
                }) {
                    Text(uiStrings.fillBlankExitConfirm, color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showExitDialog = false }) {
                    Text(uiStrings.fillBlankExitCancel, color = DeepPurple)
                }
            }
        )
    }
}

// ── Summary screen ───────────────────────────────────────────────────────────

@Composable
private fun FillBlankSummaryScreen(
    images: List<GalleryItem>,
    correctCount: Int,
    uiStrings: com.galleriaida.ui.UiStrings,
    onClose: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        Spacer(Modifier.height(24.dp))
        Text(
            text      = uiStrings.fillBlankSummaryTitle,
            style     = MaterialTheme.typography.displayLarge,
            textAlign = TextAlign.Center,
            color     = DeepPurple,
            modifier  = Modifier.fillMaxWidth().padding(horizontal = 24.dp)
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text      = uiStrings.fillBlankSummaryScore.format(correctCount, images.size),
            style     = MaterialTheme.typography.titleLarge,
            textAlign = TextAlign.Center,
            color     = MedText,
            modifier  = Modifier.fillMaxWidth()
        )

        Spacer(Modifier.height(24.dp))

        LazyVerticalGrid(
            columns  = GridCells.Fixed(2),
            modifier = Modifier.weight(1f).padding(horizontal = 24.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement   = Arrangement.spacedBy(12.dp)
        ) {
            items(images) { img ->
                val imageModel = remember(img.imageUrl) {
                    val f = File(img.imageUrl); if (f.exists()) f else img.imageUrl
                }
                AsyncImage(
                    model              = imageModel,
                    contentDescription = img.titleLocal,
                    contentScale       = ContentScale.Crop,
                    modifier           = Modifier
                        .fillMaxWidth()
                        .height(140.dp)
                        .clip(RoundedCornerShape(16.dp))
                )
            }
        }

        Button(
            onClick  = onClose,
            modifier = Modifier.fillMaxWidth().padding(24.dp).height(56.dp),
            shape    = RoundedCornerShape(16.dp),
            colors   = ButtonDefaults.buttonColors(containerColor = ButtonSecondary)
        ) {
            Text(uiStrings.fillBlankSummaryClose, style = MaterialTheme.typography.labelLarge)
        }
    }
}
