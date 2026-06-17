package com.galleriaida.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.graphics.Color
import com.galleriaida.ui.theme.*
import com.galleriaida.viewmodel.AppViewModel

private const val PUZZLE_UNLOCK_THRESHOLD     = 1
private const val FILL_BLANK_UNLOCK_THRESHOLD = 6
private const val ONE_PIECE_UNLOCK_THRESHOLD  = 8

@Composable
fun MiniGamesScreen(
    viewModel: AppViewModel,
    onBack: () -> Unit,
    onPuzzle: () -> Unit,
    onFillTheBlank: () -> Unit,
    onOnePiece: () -> Unit,
    onEditProfile: () -> Unit
) {
    val player    by viewModel.currentPlayer.collectAsState()
    val gallery   by viewModel.gallery.collectAsState()
    val uiStrings by viewModel.uiStrings.collectAsState()

    val stars           = player?.stars ?: 0
    val initial         = player?.name?.firstOrNull()?.uppercase() ?: "?"
    val playerImages    = gallery.filter { it.playerId == player?.id }
    val imageCount      = playerImages.size
    val puzzleLocked    = imageCount < PUZZLE_UNLOCK_THRESHOLD
    val fillBlankLocked = imageCount < FILL_BLANK_UNLOCK_THRESHOLD
    val onePieceLocked  = imageCount < ONE_PIECE_UNLOCK_THRESHOLD

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        Column(modifier = Modifier.fillMaxSize()) {

            // ── Top bar ──────────────────────────────────────────────────
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
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
                        text  = "$stars",
                        style = MaterialTheme.typography.titleMedium,
                        color = DeepPurple
                    )
                }

                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(SoftPurple)
                        .clickable { onEditProfile() },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text       = initial,
                        fontSize   = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color      = DeepPurple
                    )
                }
            }

            // ── Title ────────────────────────────────────────────────────
            Spacer(Modifier.height(8.dp))
            Text(
                text      = uiStrings.miniGamesTitle,
                style     = MaterialTheme.typography.displayLarge,
                textAlign = TextAlign.Center,
                modifier  = Modifier.fillMaxWidth().padding(horizontal = 24.dp)
            )

            Spacer(Modifier.height(40.dp))

            // ── Game cards ───────────────────────────────────────────────
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                MiniGameCard(
                    emoji       = "🧩",
                    name        = uiStrings.miniGamesPuzzleName,
                    locked      = puzzleLocked,
                    lockedHint  = uiStrings.miniGamesLockedHint.format(PUZZLE_UNLOCK_THRESHOLD),
                    costHint    = uiStrings.miniGamesCostHint,
                    color       = ButtonPrimary,
                    onClick     = { if (!puzzleLocked) onPuzzle() }
                )

                MiniGameCard(
                    emoji       = "🔡",
                    name        = uiStrings.miniGamesFillBlankName,
                    locked      = fillBlankLocked,
                    lockedHint  = uiStrings.miniGamesLockedHint.format(FILL_BLANK_UNLOCK_THRESHOLD),
                    costHint    = uiStrings.miniGamesCostHint,
                    color       = ButtonSecondary,
                    onClick     = { if (!fillBlankLocked) onFillTheBlank() }
                )

                MiniGameCard(
                    emoji       = "🔲",
                    name        = uiStrings.miniGamesOnePieceName,
                    locked      = onePieceLocked,
                    lockedHint  = uiStrings.miniGamesLockedHint.format(ONE_PIECE_UNLOCK_THRESHOLD),
                    costHint    = uiStrings.miniGamesCostHint,
                    color       = Color(0xFFFF6F00),
                    onClick     = { if (!onePieceLocked) onOnePiece() }
                )
            }
        }
    }
}

@Composable
private fun MiniGameCard(
    emoji: String,
    name: String,
    locked: Boolean,
    lockedHint: String,
    costHint: String,
    color: androidx.compose.ui.graphics.Color,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(if (locked) DisabledGray.copy(alpha = 0.3f) else CardBg)
            .clickable(enabled = !locked) { onClick() }
            .padding(20.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.alpha(if (locked) 0.5f else 1f)
        ) {
            // Icon circle
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(CircleShape)
                    .background(if (locked) DisabledGray else color.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Text(emoji, fontSize = 30.sp)
            }

            Spacer(Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text       = name,
                    style      = MaterialTheme.typography.titleMedium,
                    color      = if (locked) MedText else DeepPurple,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.height(8.dp))
                if (locked) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.Lock,
                            contentDescription = null,
                            tint     = MedText,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(
                            text  = lockedHint,
                            style = MaterialTheme.typography.bodySmall,
                            color = MedText
                        )
                    }
                } else {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(40.dp))
                            .background(color.copy(alpha = 0.15f))
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text  = costHint,
                            style = MaterialTheme.typography.bodySmall,
                            color = color,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}