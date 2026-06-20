package com.galleriaida.ui.screens

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
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
import kotlin.math.roundToInt
import kotlin.random.Random

private const val TOTAL_ROUNDS = 5
private const val PLAY_COST    = 5
private const val LINEUP_SIZE  = 4

private val VividGreen = Color(0xFF00C853)
private val VividRed   = Color(0xFFFF1744)

enum class OnePieceDifficulty(val gridSize: Int) {
    EASY(7),     // 1/49
    MEDIUM(8),   // 1/64
    HARD(10)     // 1/100
}

private data class OnePieceRound(
    val correctImage: GalleryItem,
    val pieceBitmap: ImageBitmap,
    val lineup: List<GalleryItem>   // size = LINEUP_SIZE, contains correctImage once at a random position
)

private fun buildRound(allImages: List<GalleryItem>, difficulty: OnePieceDifficulty): OnePieceRound? {
    if (allImages.size < LINEUP_SIZE) return null
    val correct = allImages.random()
    val file = File(correct.imageUrl)
    if (!file.exists()) return null
    val bmp = try { BitmapFactory.decodeFile(file.absolutePath) } catch (e: Exception) { null } ?: return null

    val grid = difficulty.gridSize
    val pieceCol = Random.nextInt(grid)
    val pieceRow = Random.nextInt(grid)
    val pieceW = bmp.width / grid
    val pieceH = bmp.height / grid
    val cropped = try {
        Bitmap.createBitmap(
            bmp,
            (pieceCol * pieceW).coerceIn(0, bmp.width - 1),
            (pieceRow * pieceH).coerceIn(0, bmp.height - 1),
            pieceW.coerceAtLeast(1).coerceAtMost(bmp.width - pieceCol * pieceW),
            pieceH.coerceAtLeast(1).coerceAtMost(bmp.height - pieceRow * pieceH)
        )
    } catch (e: Exception) { return null }

    val decoys = allImages.filter { it.id != correct.id }.shuffled().take(LINEUP_SIZE - 1)
    if (decoys.size < LINEUP_SIZE - 1) return null
    val lineup = (decoys + correct).shuffled()

    return OnePieceRound(correctImage = correct, pieceBitmap = cropped.asImageBitmap(), lineup = lineup)
}

@Composable
fun OnePieceScreen(
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

    var phase by remember { mutableStateOf(OnePiecePhase.SETUP) }
    var difficulty by remember { mutableStateOf(OnePieceDifficulty.EASY) }
    var correctCount by remember { mutableStateOf(0) }
    var roundsPlayedResults by remember { mutableStateOf<List<Pair<GalleryItem, Boolean>>>(emptyList()) }

    when (phase) {
        OnePiecePhase.SETUP -> {
            OnePieceSetupScreen(
                uiStrings  = uiStrings,
                difficulty = difficulty,
                stars      = player?.stars ?: 0,
                isDev      = isDev,
                imageCount = playerImages.size,
                onDifficultySelected = { difficulty = it },
                onPlay = {
                    val spent = viewModel.spendStars(PLAY_COST)
                    if (spent) {
                        correctCount = 0
                        roundsPlayedResults = emptyList()
                        phase = OnePiecePhase.PLAYING
                    }
                },
                onBack = onBack
            )
        }
        OnePiecePhase.PLAYING -> {
            OnePieceGameScreen(
                allImages   = playerImages,
                difficulty  = difficulty,
                uiStrings   = uiStrings,
                onRoundDone = { image, correct ->
                    if (correct) correctCount++
                    roundsPlayedResults = roundsPlayedResults + (image to correct)
                },
                onAllRoundsDone = { phase = OnePiecePhase.SUMMARY },
                onExit = onBack
            )
        }
        OnePiecePhase.SUMMARY -> {
            OnePieceSummaryScreen(
                results      = roundsPlayedResults,
                correctCount = correctCount,
                uiStrings    = uiStrings,
                onClose      = onBack
            )
        }
    }
}

private enum class OnePiecePhase { SETUP, PLAYING, SUMMARY }

// ── Setup screen ──────────────────────────────────────────────────────────────

@Composable
private fun OnePieceSetupScreen(
    uiStrings: com.galleriaida.ui.UiStrings,
    difficulty: OnePieceDifficulty,
    stars: Int,
    isDev: Boolean,
    imageCount: Int,
    onDifficultySelected: (OnePieceDifficulty) -> Unit,
    onPlay: () -> Unit,
    onBack: () -> Unit
) {
    val canAfford    = isDev || stars >= PLAY_COST
    val enoughImages = imageCount >= LINEUP_SIZE

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
                text      = uiStrings.onePieceTitle,
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

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp)
        ) {
            Text("🔲", fontSize = 56.sp)
            Spacer(Modifier.height(16.dp))
            Text(
                text      = uiStrings.onePieceExplainer.format(TOTAL_ROUNDS),
                style     = MaterialTheme.typography.bodyLarge,
                color     = MedText,
                textAlign = TextAlign.Center
            )
        }

        Spacer(Modifier.height(28.dp))

        Text(
            text       = uiStrings.onePieceSelectDifficulty,
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
            OnePieceDifficulty.values().forEach { d ->
                val selected = d == difficulty
                val label = when (d) {
                    OnePieceDifficulty.EASY   -> uiStrings.onePieceEasy
                    OnePieceDifficulty.MEDIUM -> uiStrings.onePieceMedium
                    OnePieceDifficulty.HARD   -> uiStrings.onePieceHard
                }
                val fraction = "1/${d.gridSize * d.gridSize}"
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(16.dp))
                        .background(if (selected) Color(0xFFFF6F00) else CardBg)
                        .clickable { onDifficultySelected(d) }
                        .padding(vertical = 14.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text  = label,
                            style = MaterialTheme.typography.bodySmall,
                            color = if (selected) Color.White else DeepPurple,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text  = fraction,
                            style = MaterialTheme.typography.bodySmall,
                            color = if (selected) Color.White.copy(alpha = 0.85f) else MedText
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(24.dp))

        if (!enoughImages) {
            Text(
                text      = uiStrings.onePieceNotEnoughImages,
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
                containerColor         = Color(0xFFFF6F00),
                disabledContainerColor = DisabledGray
            )
        ) {
            Text(
                text  = if (!canAfford) uiStrings.onePieceNotEnoughStars else uiStrings.onePiecePlay,
                style = MaterialTheme.typography.labelLarge
            )
        }

        Spacer(Modifier.height(24.dp))
    }
}

// ── Game screen ───────────────────────────────────────────────────────────────

@Composable
private fun OnePieceGameScreen(
    allImages: List<GalleryItem>,
    difficulty: OnePieceDifficulty,
    uiStrings: com.galleriaida.ui.UiStrings,
    onRoundDone: (GalleryItem, Boolean) -> Unit,
    onAllRoundsDone: () -> Unit,
    onExit: () -> Unit
) {
    var roundIndex by remember { mutableStateOf(0) }
    var showExitDialog by remember { mutableStateOf(false) }
    var selectedId by remember(roundIndex) { mutableStateOf<String?>(null) }
    var answered by remember(roundIndex) { mutableStateOf(false) }

    val round = remember(roundIndex) { buildRound(allImages, difficulty) }

    BackHandler { showExitDialog = true }

    if (round == null) {
        // Not enough images / failed to build a round — exit safely
        LaunchedEffect(Unit) { onExit() }
        return
    }

    val isCorrectSelection = selectedId == round.correctImage.id

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
            IconButton(onClick = { showExitDialog = true }) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = DeepPurple)
            }
            Text(
                text      = uiStrings.onePieceRoundCounter.format(roundIndex + 1, TOTAL_ROUNDS),
                style     = MaterialTheme.typography.titleMedium,
                modifier  = Modifier.weight(1f),
                textAlign = TextAlign.Center,
                color     = DeepPurple
            )
            Spacer(Modifier.width(48.dp))
        }

        // Piece preview — displayed at a fixed size per difficulty, preserving aspect ratio.
        // Easy = large piece, Hard = small piece, making difficulty visually clear.
        val stageSize = 220.dp
        val pieceDisplaySize = when (difficulty) {
            OnePieceDifficulty.EASY   -> 120.dp
            OnePieceDifficulty.MEDIUM -> 80.dp
            OnePieceDifficulty.HARD   -> 48.dp
        }

        val pieceAspect = round.pieceBitmap.width.toFloat() / round.pieceBitmap.height.toFloat()
        val (pieceDisplayW, pieceDisplayH) = if (pieceAspect >= 1f) {
            pieceDisplaySize to (pieceDisplaySize / pieceAspect)
        } else {
            (pieceDisplaySize * pieceAspect) to pieceDisplaySize
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(stageSize + 20.dp),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .size(stageSize)
                    .clip(RoundedCornerShape(16.dp))
                    .background(CardBg)
                    .border(2.dp, Color(0xFFFF6F00), RoundedCornerShape(16.dp)),
                contentAlignment = Alignment.Center
            ) {
                Canvas(
                    modifier = Modifier.size(
                        width  = pieceDisplayW.coerceAtLeast(14.dp),
                        height = pieceDisplayH.coerceAtLeast(14.dp)
                    )
                ) {
                    drawImage(
                        image   = round.pieceBitmap,
                        dstSize = androidx.compose.ui.unit.IntSize(
                            this.size.width.roundToInt(),
                            this.size.height.roundToInt()
                        )
                    )
                }
            }
        }

        Spacer(Modifier.height(16.dp))

        Text(
            text      = uiStrings.onePieceQuestion,
            style     = MaterialTheme.typography.titleMedium,
            color     = DeepPurple,
            textAlign = TextAlign.Center,
            modifier  = Modifier.fillMaxWidth().padding(horizontal = 24.dp)
        )

        Spacer(Modifier.height(16.dp))

        // Lineup of 4
        LazyVerticalGrid(
            columns  = GridCells.Fixed(2),
            modifier = Modifier.weight(1f).padding(horizontal = 24.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement   = Arrangement.spacedBy(12.dp)
        ) {
            items(round.lineup) { img ->
                val imageModel = remember(img.imageUrl) {
                    val f = File(img.imageUrl); if (f.exists()) f else img.imageUrl
                }
                val isThisCorrect = img.id == round.correctImage.id
                val isThisSelected = img.id == selectedId

                val borderColor = when {
                    !answered -> Color.Transparent
                    isThisCorrect -> VividGreen
                    isThisSelected -> VividRed
                    else -> Color.Transparent
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(140.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(CardBg)
                        .border(3.dp, borderColor, RoundedCornerShape(16.dp))
                        .clickable(enabled = !answered) {
                            selectedId = img.id
                            answered = true
                        }
                ) {
                    AsyncImage(
                        model              = imageModel,
                        contentDescription = img.titleLocal,
                        contentScale       = ContentScale.Fit,
                        modifier           = Modifier.fillMaxSize().clip(RoundedCornerShape(16.dp))
                    )
                    if (answered && (isThisCorrect || isThisSelected)) {
                        Box(
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(6.dp)
                                .size(28.dp)
                                .clip(RoundedCornerShape(50))
                                .background(if (isThisCorrect) VividGreen else VividRed),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text  = if (isThisCorrect) "✓" else "✗",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(12.dp))

        if (answered) {
            Text(
                text      = if (isCorrectSelection) uiStrings.onePieceCorrect else uiStrings.onePieceWrong,
                style     = MaterialTheme.typography.titleMedium,
                color     = if (isCorrectSelection) VividGreen else VividRed,
                textAlign = TextAlign.Center,
                modifier  = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(12.dp))
            Button(
                onClick = {
                    onRoundDone(round.correctImage, isCorrectSelection)
                    if (roundIndex + 1 >= TOTAL_ROUNDS) {
                        onAllRoundsDone()
                    } else {
                        roundIndex++
                    }
                },
                modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp).height(56.dp),
                shape    = RoundedCornerShape(16.dp),
                colors   = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF6F00))
            ) {
                Text(uiStrings.onePieceNext, style = MaterialTheme.typography.labelLarge)
            }
            Spacer(Modifier.height(24.dp))
        } else {
            Spacer(Modifier.height(24.dp + 56.dp + 12.dp))
        }
    }

    if (showExitDialog) {
        AlertDialog(
            onDismissRequest = { showExitDialog = false },
            title = { Text(uiStrings.onePieceExitTitle, fontWeight = FontWeight.Bold) },
            text  = { Text(uiStrings.onePieceExitBody) },
            confirmButton = {
                TextButton(onClick = {
                    showExitDialog = false
                    onExit()
                }) {
                    Text(uiStrings.onePieceExitConfirm, color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showExitDialog = false }) {
                    Text(uiStrings.onePieceExitCancel, color = DeepPurple)
                }
            }
        )
    }
}

// ── Summary screen ───────────────────────────────────────────────────────────

@Composable
private fun OnePieceSummaryScreen(
    results: List<Pair<GalleryItem, Boolean>>,
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
            text      = uiStrings.onePieceSummaryTitle,
            style     = MaterialTheme.typography.displayLarge,
            textAlign = TextAlign.Center,
            color     = DeepPurple,
            modifier  = Modifier.fillMaxWidth().padding(horizontal = 24.dp)
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text      = uiStrings.onePieceSummaryScore.format(correctCount, results.size),
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
            items(results) { (img, wasCorrect) ->
                val imageModel = remember(img.imageUrl) {
                    val f = File(img.imageUrl); if (f.exists()) f else img.imageUrl
                }
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(140.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(CardBg)
                        .border(3.dp, if (wasCorrect) VividGreen else VividRed, RoundedCornerShape(16.dp))
                ) {
                    AsyncImage(
                        model              = imageModel,
                        contentDescription = img.titleLocal,
                        contentScale       = ContentScale.Fit,
                        modifier           = Modifier.fillMaxSize().clip(RoundedCornerShape(16.dp))
                    )
                    // Check/cross badge
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(6.dp)
                            .size(28.dp)
                            .clip(RoundedCornerShape(50))
                            .background(if (wasCorrect) VividGreen else VividRed),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text  = if (wasCorrect) "✓" else "✗",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }
        }

        Button(
            onClick  = onClose,
            modifier = Modifier.fillMaxWidth().padding(24.dp).height(56.dp),
            shape    = RoundedCornerShape(16.dp),
            colors   = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF6F00))
        ) {
            Text(uiStrings.onePieceSummaryClose, style = MaterialTheme.typography.labelLarge)
        }
    }
}