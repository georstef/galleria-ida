package com.galleriaida.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.galleriaida.data.GalleryItem
import com.galleriaida.ui.theme.*
import com.galleriaida.viewmodel.AppViewModel
import kotlinx.coroutines.delay
import java.io.File

// ── Difficulty ────────────────────────────────────────────────────────────────

enum class MemoryMatchDifficulty(
    val cols: Int,
    val rows: Int,
    val pairs: Int,
    val label: String,
    val gridLabel: String
) {
    EASY(3, 4, 6, "Easy", "3×4"),
    MEDIUM(4, 4, 8, "Medium", "4×4"),
    HARD(4, 5, 10, "Hard", "4×5")
}

// ── Phase ─────────────────────────────────────────────────────────────────────

private enum class MemoryMatchPhase { SETUP, PLAYING, WON }

// ── Card model ────────────────────────────────────────────────────────────────

private data class MemoryCard(
    val id: Int,           // unique card index (0..totalCards-1)
    val pairId: Int,       // which pair this card belongs to (0..pairs-1)
    val imageUrl: String,
    var isFaceUp: Boolean = false,
    var isMatched: Boolean = false
)

// ── Root composable ───────────────────────────────────────────────────────────

@Composable
fun MemoryMatchScreen(
    viewModel: AppViewModel,
    onBack: () -> Unit
) {
    val player    by viewModel.currentPlayer.collectAsState()
    val gallery   by viewModel.gallery.collectAsState()
    val uiStrings by viewModel.uiStrings.collectAsState()

    val playerImages = remember(gallery, player) {
        gallery.filter { it.playerId == player?.id }
    }

    var phase      by remember { mutableStateOf(MemoryMatchPhase.SETUP) }
    var difficulty by remember { mutableStateOf(MemoryMatchDifficulty.EASY) }
    var cards      by remember { mutableStateOf<List<MemoryCard>>(emptyList()) }

    val isDev = player?.name?.trim()
        ?.equals(com.galleriaida.AppConstants.DEV_PLAYER_NAME, ignoreCase = true) ?: false

    when (phase) {
        MemoryMatchPhase.SETUP -> {
            MemoryMatchSetupScreen(
                uiStrings  = uiStrings,
                stars      = player?.stars ?: 0,
                isDev      = isDev,
                difficulty = difficulty,
                onDifficultySelected = { difficulty = it },
                onPlay = {
                    val spent = viewModel.spendStars(10)
                    if (spent) {
                        cards = buildCards(playerImages, difficulty)
                        phase = MemoryMatchPhase.PLAYING
                    }
                },
                onBack = onBack
            )
        }
        MemoryMatchPhase.PLAYING -> {
            MemoryMatchGameScreen(
                uiStrings  = uiStrings,
                cards      = cards,
                difficulty = difficulty,
                onCardsChanged = { cards = it },
                onWon      = { phase = MemoryMatchPhase.WON },
                onBack     = onBack
            )
        }
        MemoryMatchPhase.WON -> {
            MemoryMatchWonScreen(
                uiStrings = uiStrings,
                onClose   = onBack
            )
        }
    }
}

// ── Card builder ──────────────────────────────────────────────────────────────

private fun buildCards(
    images: List<GalleryItem>,
    difficulty: MemoryMatchDifficulty
): List<MemoryCard> {
    val picked = images.shuffled().take(difficulty.pairs)
    val cards = mutableListOf<MemoryCard>()
    picked.forEachIndexed { pairIndex, item ->
        // Each image appears twice
        cards += MemoryCard(id = pairIndex * 2,     pairId = pairIndex, imageUrl = item.imageUrl)
        cards += MemoryCard(id = pairIndex * 2 + 1, pairId = pairIndex, imageUrl = item.imageUrl)
    }
    return cards.shuffled()
}

// ── Setup screen ──────────────────────────────────────────────────────────────

// Soft red accent — consistent across setup, game bar, and won screen
private val MemoryMatchAccent = Color(0xFFE57373)

@Composable
private fun MemoryMatchSetupScreen(
    uiStrings: com.galleriaida.ui.UiStrings,
    stars: Int,
    isDev: Boolean,
    difficulty: MemoryMatchDifficulty,
    onDifficultySelected: (MemoryMatchDifficulty) -> Unit,
    onPlay: () -> Unit,
    onBack: () -> Unit
) {
    val canAfford = isDev || stars >= 10

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        // ── Top bar ──────────────────────────────────────────────────────────
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
                text      = uiStrings.memoryMatchTitle,
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

        Spacer(Modifier.height(24.dp))

        // ── Emoji + explainer ────────────────────────────────────────────────
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp)
        ) {
            Text("🧠", fontSize = 56.sp)
            Spacer(Modifier.height(16.dp))
            Text(
                text      = uiStrings.memoryMatchDesc,
                style     = MaterialTheme.typography.bodyLarge,
                color     = MedText,
                textAlign = TextAlign.Center
            )
        }

        Spacer(Modifier.height(28.dp))

        // ── Difficulty label ─────────────────────────────────────────────────
        Text(
            text       = uiStrings.memoryMatchSelectDifficulty,
            style      = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Bold,
            color      = DeepPurple,
            modifier   = Modifier.padding(horizontal = 24.dp)
        )
        Spacer(Modifier.height(10.dp))

        // ── Difficulty buttons — equal-width row ─────────────────────────────
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            MemoryMatchDifficulty.entries.forEach { diff ->
                val selected = diff == difficulty
                val label = when (diff) {
                    MemoryMatchDifficulty.EASY   -> uiStrings.memoryMatchEasy
                    MemoryMatchDifficulty.MEDIUM -> uiStrings.memoryMatchMedium
                    MemoryMatchDifficulty.HARD   -> uiStrings.memoryMatchHard
                }
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(16.dp))
                        .background(if (selected) MemoryMatchAccent else CardBg)
                        .clickable { onDifficultySelected(diff) }
                        .padding(vertical = 14.dp),
                    contentAlignment = Alignment.Center
                ) {
                    // Label + grid size on one line e.g. "Easy  3×4"
                    Text(
                        text       = "$label  ${diff.gridLabel}",
                        style      = MaterialTheme.typography.bodySmall,
                        color      = if (selected) Color.White else DeepPurple,
                        fontWeight = FontWeight.Bold,
                        textAlign  = TextAlign.Center
                    )
                }
            }
        }

        Spacer(Modifier.height(24.dp))

        // ── Play button ──────────────────────────────────────────────────────
        Button(
            onClick  = onPlay,
            enabled  = canAfford,
            modifier = Modifier
                .fillMaxWidth()
                .height(60.dp)
                .padding(horizontal = 24.dp),
            shape  = RoundedCornerShape(20.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor         = MemoryMatchAccent,
                disabledContainerColor = DisabledGray
            )
        ) {
            Text(
                text  = if (canAfford) uiStrings.memoryMatchPlay
                else uiStrings.memoryMatchNotEnoughStars,
                style = MaterialTheme.typography.labelLarge,
                color = Color.White
            )
        }
    }
}

// ── Game screen ───────────────────────────────────────────────────────────────

@Composable
private fun MemoryMatchGameScreen(
    uiStrings: com.galleriaida.ui.UiStrings,
    cards: List<MemoryCard>,
    difficulty: MemoryMatchDifficulty,
    onCardsChanged: (List<MemoryCard>) -> Unit,
    onWon: () -> Unit,
    onBack: () -> Unit
) {
    // Exit confirmation dialog
    var showExitDialog by remember { mutableStateOf(false) }
    // Block input while evaluating a pair
    var inputLocked by remember { mutableStateOf(false) }

    BackHandler { showExitDialog = true }

    // The two currently face-up (unmatched) card IDs
    val faceUpIds = remember(cards) {
        cards.filter { it.isFaceUp && !it.isMatched }.map { it.id }
    }

    // Separate win-check: fires whenever cards changes, safely after onCardsChanged settles
    LaunchedEffect(cards) {
        if (cards.isNotEmpty() && cards.all { it.isMatched }) {
            delay(300)
            onWon()
        }
    }

    // Check for a completed pair whenever two cards are face-up
    LaunchedEffect(faceUpIds) {
        if (faceUpIds.size == 2) {
            inputLocked = true
            val a = cards.first { it.id == faceUpIds[0] }
            val b = cards.first { it.id == faceUpIds[1] }
            if (a.pairId == b.pairId) {
                // Match — keep them visible briefly then mark matched (disappear)
                delay(700)
                val updated = cards.map { c ->
                    if (c.id == a.id || c.id == b.id)
                        c.copy(isFaceUp = false, isMatched = true)
                    else c
                }
                onCardsChanged(updated)
            } else {
                // No match — flip back after a short pause
                delay(900)
                val updated = cards.map { c ->
                    if (c.id == a.id || c.id == b.id) c.copy(isFaceUp = false) else c
                }
                onCardsChanged(updated)
            }
            inputLocked = false
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        Column(modifier = Modifier.fillMaxSize()) {

            // Top bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { showExitDialog = true }) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = DeepPurple)
                }
                Text(
                    text  = uiStrings.memoryMatchTitle,
                    style = MaterialTheme.typography.titleLarge,
                    color = DeepPurple,
                    fontWeight = FontWeight.Bold
                )
                // Pairs remaining indicator
                val remaining = cards.count { !it.isMatched } / 2
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(40.dp))
                        .background(SoftPurple)
                        .padding(horizontal = 14.dp, vertical = 8.dp)
                ) {
                    Text(
                        text  = "🃏 $remaining",
                        style = MaterialTheme.typography.titleMedium,
                        color = DeepPurple
                    )
                }
            }

            // Grid
            BoxWithConstraints(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 12.dp, vertical = 8.dp)
            ) {
                val cols       = difficulty.cols
                val rows       = difficulty.rows
                val spacing    = 8.dp
                val totalSpacingW = spacing * (cols - 1)
                val totalSpacingH = spacing * (rows - 1)
                val cardW = (maxWidth  - totalSpacingW) / cols
                val cardH = (maxHeight - totalSpacingH) / rows

                Column(verticalArrangement = Arrangement.spacedBy(spacing)) {
                    for (row in 0 until rows) {
                        Row(horizontalArrangement = Arrangement.spacedBy(spacing)) {
                            for (col in 0 until cols) {
                                val cardIndex = row * cols + col
                                val card = cards.getOrNull(cardIndex)
                                if (card != null) {
                                    MemoryCardView(
                                        card    = card,
                                        width   = cardW,
                                        height  = cardH,
                                        onClick = {
                                            if (!inputLocked && !card.isFaceUp && !card.isMatched) {
                                                // Only allow flipping if fewer than 2 are face-up
                                                val currentFaceUp = cards.count { it.isFaceUp && !it.isMatched }
                                                if (currentFaceUp < 2) {
                                                    onCardsChanged(
                                                        cards.map { c ->
                                                            if (c.id == card.id) c.copy(isFaceUp = true) else c
                                                        }
                                                    )
                                                }
                                            }
                                        }
                                    )
                                } else {
                                    Spacer(Modifier.size(cardW, cardH))
                                }
                            }
                        }
                    }
                }
            }
        }

        // Exit dialog
        if (showExitDialog) {
            AlertDialog(
                onDismissRequest = { showExitDialog = false },
                title   = { Text(uiStrings.memoryMatchExitTitle) },
                text    = { Text(uiStrings.memoryMatchExitBody) },
                confirmButton = {
                    TextButton(onClick = { showExitDialog = false; onBack() }) {
                        Text(
                            uiStrings.memoryMatchExitConfirm,
                            color      = MaterialTheme.colorScheme.error,
                            fontWeight = FontWeight.Bold
                        )
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showExitDialog = false }) {
                        Text(uiStrings.memoryMatchExitCancel, color = DeepPurple)
                    }
                }
            )
        }
    }
}

// ── Single card ───────────────────────────────────────────────────────────────

@Composable
private fun MemoryCardView(
    card: MemoryCard,
    width: androidx.compose.ui.unit.Dp,
    height: androidx.compose.ui.unit.Dp,
    onClick: () -> Unit
) {
    // Flip animation: 0f = face-down, 180f = face-up
    val targetRotation = if (card.isFaceUp || card.isMatched) 180f else 0f
    val rotation by animateFloatAsState(
        targetValue    = targetRotation,
        animationSpec  = tween(durationMillis = 300),
        label          = "cardFlip"
    )

    // Fade out when matched
    val alpha by animateFloatAsState(
        targetValue   = if (card.isMatched) 0f else 1f,
        animationSpec = tween(durationMillis = 400),
        label         = "cardFade"
    )

    Box(
        modifier = Modifier
            .size(width, height)
            .graphicsLayer { this.alpha = alpha }
            .clip(RoundedCornerShape(12.dp))
            .clickable(enabled = !card.isMatched) { onClick() }
    ) {
        if (rotation <= 90f) {
            // Face-down side
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(ButtonPrimary)
                    .graphicsLayer { rotationY = rotation },
                contentAlignment = Alignment.Center
            ) {
                Text("🧠", fontSize = 28.sp)
            }
        } else {
            // Face-up side (mirror the rotation so it reads correctly)
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer { rotationY = rotation - 180f }
            ) {
                val imageFile = File(card.imageUrl)
                AsyncImage(
                    model              = if (imageFile.exists()) imageFile else card.imageUrl,
                    contentDescription = null,
                    contentScale       = ContentScale.Crop,
                    modifier           = Modifier.fillMaxSize()
                )
            }
        }
    }
}

// ── Won screen ────────────────────────────────────────────────────────────────

@Composable
private fun MemoryMatchWonScreen(
    uiStrings: com.galleriaida.ui.UiStrings,
    onClose: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
            .navigationBarsPadding(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(40.dp)
        ) {
            Text("🎉", fontSize = 72.sp)
            Spacer(Modifier.height(24.dp))
            Text(
                text      = uiStrings.memoryMatchWonTitle,
                style     = MaterialTheme.typography.displayLarge,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(12.dp))
            Text(
                text      = uiStrings.memoryMatchWonBody,
                style     = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center,
                color     = MedText
            )
            Spacer(Modifier.height(48.dp))
            Button(
                onClick  = onClose,
                modifier = Modifier.fillMaxWidth().height(64.dp),
                shape    = RoundedCornerShape(20.dp),
                colors   = ButtonDefaults.buttonColors(containerColor = ButtonPrimary)
            ) {
                Text(uiStrings.memoryMatchWonClose, style = MaterialTheme.typography.labelLarge)
            }
        }
    }
}