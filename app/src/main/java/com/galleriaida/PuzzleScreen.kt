package com.galleriaida.ui.screens

import androidx.activity.compose.BackHandler
import android.graphics.BitmapFactory
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import coil.compose.AsyncImage
import com.galleriaida.data.GalleryItem
import com.galleriaida.ui.theme.*
import com.galleriaida.viewmodel.AppViewModel
import java.io.File
import kotlin.math.roundToInt

enum class PuzzleSize(val cols: Int, val rows: Int) {
    EASY(4, 4),
    MEDIUM(4, 5),
    HARD(4, 8)
}

@Composable
fun PuzzleScreen(
    viewModel: AppViewModel,
    onBack: () -> Unit
) {
    val player    by viewModel.currentPlayer.collectAsState()
    val gallery   by viewModel.gallery.collectAsState()
    val uiStrings by viewModel.uiStrings.collectAsState()

    val playerImages = remember(gallery, player) {
        gallery.filter { it.playerId == player?.id }
    }

    // ── State machine ─────────────────────────────────────────────────────────
    // Phase 1: setup  → player picks size and sees a random image preview
    // Phase 2: playing → the actual puzzle; on completion a popup appears and
    //                     closing it exits straight back to the Mini Games hub

    var phase by remember { mutableStateOf(PuzzlePhase.SETUP) }
    var selectedSize by remember { mutableStateOf(PuzzleSize.EASY) }
    var currentImageIndex by remember { mutableStateOf((playerImages.indices).randomOrNull() ?: 0) }
    val currentImage = playerImages.getOrNull(currentImageIndex)

    val isDev = player?.name?.trim()?.equals(com.galleriaida.AppConstants.DEV_PLAYER_NAME, ignoreCase = true) ?: false

    when (phase) {
        PuzzlePhase.SETUP -> {
            PuzzleSetupScreen(
                image        = currentImage,
                uiStrings    = uiStrings,
                selectedSize = selectedSize,
                stars        = player?.stars ?: 0,
                isDev        = isDev,
                onSizeSelected = { selectedSize = it },
                onReroll     = {
                    if (playerImages.size > 1) {
                        currentImageIndex = ((currentImageIndex + 1) % playerImages.size)
                    }
                },
                onPlay       = {
                    val spent = viewModel.spendStars(10)
                    if (spent) phase = PuzzlePhase.PLAYING
                },
                onBack       = onBack
            )
        }
        PuzzlePhase.PLAYING -> {
            if (currentImage != null) {
                PuzzleGameScreen(
                    image      = currentImage,
                    size       = selectedSize,
                    uiStrings  = uiStrings,
                    onSolved   = onBack,
                    onBack     = onBack
                )
            }
        }
    }
}

enum class PuzzlePhase { SETUP, PLAYING }

// ── Setup screen ──────────────────────────────────────────────────────────────

@Composable
private fun PuzzleSetupScreen(
    image: GalleryItem?,
    uiStrings: com.galleriaida.ui.UiStrings,
    selectedSize: PuzzleSize,
    stars: Int,
    isDev: Boolean,
    onSizeSelected: (PuzzleSize) -> Unit,
    onReroll: () -> Unit,
    onPlay: () -> Unit,
    onBack: () -> Unit
) {
    val canAfford = isDev || stars >= 10
    val imageModel = remember(image?.imageUrl) {
        image?.imageUrl?.let { url -> val f = File(url); if (f.exists()) f else url }
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
            IconButton(onClick = onBack) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = DeepPurple)
            }
            Text(
                text     = uiStrings.puzzleTitle,
                style    = MaterialTheme.typography.titleLarge,
                modifier = Modifier.weight(1f),
                textAlign = TextAlign.Center
            )
            // Stars badge
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(40.dp))
                    .background(LemonYellow)
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("⭐ $stars", style = MaterialTheme.typography.bodyMedium, color = DeepPurple)
            }
            Spacer(Modifier.width(8.dp))
        }

        // Image preview
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(horizontal = 24.dp, vertical = 8.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(CardBg),
            contentAlignment = Alignment.Center
        ) {
            if (imageModel != null) {
                AsyncImage(
                    model              = imageModel,
                    contentDescription = image?.titleLocal,
                    contentScale       = ContentScale.Fit,
                    modifier           = Modifier.fillMaxSize().clip(RoundedCornerShape(20.dp))
                )
                // Title overlay bottom
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .background(Color.Black.copy(alpha = 0.45f))
                        .padding(8.dp)
                ) {
                    Text(
                        text      = image?.titleLocal?.ifBlank { image.titleEn } ?: "",
                        style     = MaterialTheme.typography.bodyMedium,
                        color     = Color.White,
                        textAlign = TextAlign.Center,
                        modifier  = Modifier.fillMaxWidth()
                    )
                }
            } else {
                Text("No images yet", color = MedText, style = MaterialTheme.typography.bodyLarge)
            }
        }

        // Reroll button
        TextButton(
            onClick  = onReroll,
            modifier = Modifier.align(Alignment.CenterHorizontally)
        ) {
            Text(uiStrings.puzzleReroll, color = DeepPurple, style = MaterialTheme.typography.bodyMedium)
        }

        Spacer(Modifier.height(8.dp))

        // Difficulty picker
        Text(
            text      = uiStrings.puzzleSelectSize,
            style     = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Bold,
            color     = DeepPurple,
            modifier  = Modifier.padding(horizontal = 24.dp)
        )
        Spacer(Modifier.height(10.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            PuzzleSize.values().forEach { size ->
                val selected = size == selectedSize
                val label = when (size) {
                    PuzzleSize.EASY   -> uiStrings.puzzleEasy
                    PuzzleSize.MEDIUM -> uiStrings.puzzleMedium
                    PuzzleSize.HARD   -> uiStrings.puzzleHard
                }
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(16.dp))
                        .background(if (selected) ButtonPrimary else CardBg)
                        .clickable { onSizeSelected(size) }
                        .padding(vertical = 14.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text  = label,
                        style = MaterialTheme.typography.bodySmall,
                        color = if (selected) Color.White else DeepPurple,
                        fontWeight = FontWeight.Bold,
                        textAlign  = TextAlign.Center
                    )
                }
            }
        }

        Spacer(Modifier.height(20.dp))

        // Play button
        Button(
            onClick  = onPlay,
            enabled  = canAfford && image != null,
            modifier = Modifier
                .fillMaxWidth()
                .height(60.dp)
                .padding(horizontal = 24.dp),
            shape  = RoundedCornerShape(20.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor         = ButtonPrimary,
                disabledContainerColor = DisabledGray
            )
        ) {
            Text(
                text  = if (canAfford) uiStrings.puzzlePlay else uiStrings.puzzleNotEnoughStars,
                style = MaterialTheme.typography.labelLarge
            )
        }

        Spacer(Modifier.height(24.dp))
    }
}

// ── Game screen ───────────────────────────────────────────────────────────────

@Composable
private fun PuzzleGameScreen(
    image: GalleryItem,
    size: PuzzleSize,
    uiStrings: com.galleriaida.ui.UiStrings,
    onSolved: () -> Unit,
    onBack: () -> Unit
) {
    val cols  = size.cols
    val rows  = size.rows
    val total = cols * rows

    val tiles = remember {
        mutableStateListOf<Int>().also { list ->
            val shuffled = (0 until total).toMutableList().also { it.shuffle() }
            list.addAll(shuffled)
        }
    }

    var draggingIndex by remember { mutableStateOf(-1) }
    var dragOffset    by remember { mutableStateOf(Offset.Zero) }
    var boardWidthPx  by remember { mutableStateOf(0) }
    var boardHeightPx by remember { mutableStateOf(0) }
    var showingImage    by remember { mutableStateOf(false) }
    var showExitDialog  by remember { mutableStateOf(false) }
    var showSolvedPopup by remember { mutableStateOf(false) }
    val density = LocalDensity.current

    // Load the bitmap once
    val bitmap: ImageBitmap? = remember(image.imageUrl) {
        try {
            val file = File(image.imageUrl)
            val bmp = if (file.exists()) BitmapFactory.decodeFile(file.absolutePath)
            else null
            bmp?.asImageBitmap()
        } catch (e: Exception) { null }
    }

    val isSolved = remember(tiles.toList()) { tiles.indices.all { tiles[it] == it } }
    LaunchedEffect(isSolved) { if (isSolved) showSolvedPopup = true }

    // Intercept Android back gesture/button — ask for confirmation instead of leaving silently
    BackHandler {
        if (!showSolvedPopup) showExitDialog = true
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
                text      = uiStrings.puzzleTitle,
                style     = MaterialTheme.typography.titleLarge,
                modifier  = Modifier.weight(1f),
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.width(48.dp))
        }

        // Board
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(16.dp)
                .clip(RoundedCornerShape(16.dp))
                .onGloballyPositioned { coords ->
                    boardWidthPx  = coords.size.width
                    boardHeightPx = coords.size.height
                }
        ) {
            if (boardWidthPx > 0 && boardHeightPx > 0 && bitmap != null) {
                val tileW = boardWidthPx.toFloat() / cols
                val tileH = boardHeightPx.toFloat() / rows

                tiles.forEachIndexed { position, piece ->
                    val col = position % cols
                    val row = position / cols
                    val pieceCol = piece % cols
                    val pieceRow = piece / cols

                    val baseX = col * tileW
                    val baseY = row * tileH
                    val isDragging = draggingIndex == position

                    Box(
                        modifier = Modifier
                            .size(
                                width  = with(density) { tileW.toDp() },
                                height = with(density) { tileH.toDp() }
                            )
                            .offset {
                                if (isDragging) {
                                    IntOffset(
                                        (baseX + dragOffset.x).roundToInt(),
                                        (baseY + dragOffset.y).roundToInt()
                                    )
                                } else {
                                    IntOffset(baseX.roundToInt(), baseY.roundToInt())
                                }
                            }
                            .zIndex(if (isDragging) 10f else 1f)
                            .border(1.dp, Color.White.copy(alpha = 0.5f))
                            .clipToBounds()
                            .pointerInput(position) {
                                detectDragGestures(
                                    onDragStart = {
                                        draggingIndex = position
                                        dragOffset = Offset.Zero
                                    },
                                    onDrag = { change, amount ->
                                        change.consume()
                                        dragOffset += amount
                                    },
                                    onDragEnd = {
                                        val dropX = baseX + dragOffset.x + tileW / 2
                                        val dropY = baseY + dragOffset.y + tileH / 2
                                        val targetCol = (dropX / tileW).toInt().coerceIn(0, cols - 1)
                                        val targetRow = (dropY / tileH).toInt().coerceIn(0, rows - 1)
                                        val targetPos = targetRow * cols + targetCol
                                        if (targetPos != position) {
                                            val tmp = tiles[position]
                                            tiles[position] = tiles[targetPos]
                                            tiles[targetPos] = tmp
                                        }
                                        draggingIndex = -1
                                        dragOffset = Offset.Zero
                                    },
                                    onDragCancel = {
                                        draggingIndex = -1
                                        dragOffset = Offset.Zero
                                    }
                                )
                            }
                    ) {
                        // Draw just the correct crop of the bitmap for this piece
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            val srcLeft   = (pieceCol.toFloat() / cols) * bitmap.width
                            val srcTop    = (pieceRow.toFloat() / rows) * bitmap.height
                            val srcRight  = ((pieceCol + 1).toFloat() / cols) * bitmap.width
                            val srcBottom = ((pieceRow + 1).toFloat() / rows) * bitmap.height

                            drawImage(
                                image     = bitmap,
                                srcOffset = androidx.compose.ui.unit.IntOffset(
                                    srcLeft.roundToInt(), srcTop.roundToInt()
                                ),
                                srcSize   = androidx.compose.ui.unit.IntSize(
                                    (srcRight - srcLeft).roundToInt(),
                                    (srcBottom - srcTop).roundToInt()
                                ),
                                dstOffset = androidx.compose.ui.unit.IntOffset.Zero,
                                dstSize   = androidx.compose.ui.unit.IntSize(
                                    this.size.width.roundToInt(),
                                    this.size.height.roundToInt()
                                )
                            )
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(16.dp))

        // Show Image button
        OutlinedButton(
            onClick  = { showingImage = true },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .height(48.dp),
            shape  = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.outlinedButtonColors(containerColor = CardBg)
        ) {
            Text(uiStrings.puzzleShowImage, color = DeepPurple, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
        }

        Spacer(Modifier.height(16.dp))
    }

    // Full-image overlay — shown on top when showingImage == true
    if (showingImage) {
        val imageModel = remember(image.imageUrl) {
            val f = File(image.imageUrl); if (f.exists()) f else image.imageUrl
        }
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.92f))
                .statusBarsPadding()
                .navigationBarsPadding(),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(24.dp)
            ) {
                AsyncImage(
                    model              = imageModel,
                    contentDescription = image.titleLocal,
                    contentScale       = ContentScale.Fit,
                    modifier           = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 480.dp)
                        .clip(RoundedCornerShape(16.dp))
                )
                Spacer(Modifier.height(16.dp))
                Text(
                    text      = image.titleLocal.ifBlank { image.titleEn },
                    style     = MaterialTheme.typography.bodyLarge,
                    color     = Color.White,
                    textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(24.dp))
                Button(
                    onClick  = { showingImage = false },
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    shape    = RoundedCornerShape(16.dp),
                    colors   = ButtonDefaults.buttonColors(containerColor = ButtonPrimary)
                ) {
                    Text(uiStrings.puzzleCloseImage, style = MaterialTheme.typography.labelLarge)
                }
            }
        }
    }

    // Exit confirmation dialog — shown when player tries to leave mid-puzzle
    if (showExitDialog) {
        AlertDialog(
            onDismissRequest = { showExitDialog = false },
            title = { Text(uiStrings.puzzleExitTitle, fontWeight = FontWeight.Bold) },
            text  = { Text(uiStrings.puzzleExitBody) },
            confirmButton = {
                TextButton(onClick = {
                    showExitDialog = false
                    onBack()
                }) {
                    Text(uiStrings.puzzleExitConfirm, color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showExitDialog = false }) {
                    Text(uiStrings.puzzleExitCancel, color = DeepPurple)
                }
            }
        )
    }

    // Solved popup — shown once the puzzle is completed, player must close it explicitly
    if (showSolvedPopup) {
        AlertDialog(
            onDismissRequest = { /* not dismissible by tapping outside */ },
            title = { Text(uiStrings.puzzleSolved, fontWeight = FontWeight.Bold, color = DeepPurple) },
            text  = { Text(uiStrings.puzzleSolvedPopupBody) },
            confirmButton = {
                TextButton(onClick = {
                    showSolvedPopup = false
                    onSolved()
                }) {
                    Text(uiStrings.puzzleSolvedPopupClose, color = DeepPurple, fontWeight = FontWeight.Bold)
                }
            }
        )
    }
}