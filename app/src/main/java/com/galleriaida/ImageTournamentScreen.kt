package com.galleriaida.ui.screens
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.galleriaida.data.GalleryItem
import com.galleriaida.ui.UiStrings
import com.galleriaida.ui.theme.*
import com.galleriaida.viewmodel.AppViewModel
import java.io.File
// ── Constants ─────────────────────────────────────────────────────────────────
private const val TOURNAMENT_SIZE = 16   // must be a power of 2
// ── Phase ─────────────────────────────────────────────────────────────────────
private enum class TournamentPhase { BRACKET, MATCHUP, WON }
// ── Data ──────────────────────────────────────────────────────────────────────
private data class TournamentSlot(
    val image: GalleryItem?,   // null = TBD (not yet decided)
    val eliminated: Boolean = false
)
// A Round is a list of pairs (matchups). Each element is a pair of slot indices
// into the `slots` flat list.
private data class TournamentState(
    val slots: List<GalleryItem?>,    // current round's participants
    val initialSlots: List<GalleryItem?>, // the original 16, preserved for bracket display
    val round: Int,                   // 0-based (0 = Round of 16)
    val matchIndex: Int,              // which match within the current round
    val winners: List<List<GalleryItem?>> // winners[r] = winners of round r
) {
    val roundSize: Int get() = TOURNAMENT_SIZE / (1 shl round)
    val matchCount: Int get() = roundSize / 2
    val currentLeft: GalleryItem? get() = slots.getOrNull(matchIndex * 2)
    val currentRight: GalleryItem? get() = slots.getOrNull(matchIndex * 2 + 1)
    val isLastRound: Boolean get() = round == 3
    val champion: GalleryItem? get() = if (slots.size == 1) slots[0] else null
}
private fun buildInitialState(images: List<GalleryItem>): TournamentState {
    val picked = images.shuffled().take(TOURNAMENT_SIZE)
    return TournamentState(
        slots        = picked,
        initialSlots = picked,
        round        = 0,
        matchIndex   = 0,
        winners      = emptyList()
    )
}
private fun advanceState(state: TournamentState, winner: GalleryItem): TournamentState {
    val newWinners = state.winners.toMutableList()
    val currentRoundWinners = (newWinners.getOrNull(state.round) ?: emptyList()) + winner
    if (state.round < newWinners.size) newWinners[state.round] = currentRoundWinners
    else newWinners.add(currentRoundWinners)
    val nextMatchIndex = state.matchIndex + 1
    return if (nextMatchIndex >= state.matchCount) {
        state.copy(
            slots      = currentRoundWinners,
            round      = state.round + 1,
            matchIndex = 0,
            winners    = newWinners
        )
    } else {
        state.copy(matchIndex = nextMatchIndex, winners = newWinners)
    }
}
// ── Root ──────────────────────────────────────────────────────────────────────
@Composable
fun ImageTournamentScreen(
    viewModel: AppViewModel,
    onBack: () -> Unit
) {
    val player    by viewModel.currentPlayer.collectAsState()
    val gallery   by viewModel.gallery.collectAsState()
    val uiStrings by viewModel.uiStrings.collectAsState()
    val playerImages = remember(gallery, player) {
        gallery.filter { it.playerId == player?.id }
    }
    // A seed that increments each time the player starts a new tournament,
    // ensuring re-randomisation even if the gallery hasn't changed.
    var tournamentSeed by remember { mutableStateOf(0) }
    var phase   by remember(tournamentSeed) { mutableStateOf(TournamentPhase.BRACKET) }
    var state   by remember(tournamentSeed) {
        mutableStateOf(buildInitialState(playerImages))
    }
    // After a round ends we go back to BRACKET to show updated bracket,
    // then the player taps "Continue" to go back to MATCHUP.
    var roundJustCompleted by remember(tournamentSeed) { mutableStateOf(false) }
    when (phase) {
        TournamentPhase.BRACKET -> {
            TournamentBracketScreen(
                uiStrings          = uiStrings,
                state              = state,
                roundJustCompleted = roundJustCompleted,
                onStart            = {
                    roundJustCompleted = false
                    phase = TournamentPhase.MATCHUP
                },
                onBack = onBack
            )
        }
        TournamentPhase.MATCHUP -> {
            TournamentMatchupScreen(
                uiStrings = uiStrings,
                state     = state,
                onVote    = { winner ->
                    val next = advanceState(state, winner)
                    state = next
                    if (next.slots.size == 1) {
                        // Champion decided
                        phase = TournamentPhase.WON
                    } else if (next.matchIndex == 0) {
                        // New round just started — show bracket
                        roundJustCompleted = true
                        phase = TournamentPhase.BRACKET
                    }
                },
                onBack = onBack
            )
        }
        TournamentPhase.WON -> {
            TournamentWonScreen(
                uiStrings = uiStrings,
                champion  = state.slots.firstOrNull(),
                onClose   = onBack
            )
        }
    }
}
// ── Bracket screen ────────────────────────────────────────────────────────────
@Composable
private fun TournamentBracketScreen(
    uiStrings: UiStrings,
    state: TournamentState,
    roundJustCompleted: Boolean,
    onStart: () -> Unit,
    onBack: () -> Unit
) {
    var showExitDialog by remember { mutableStateOf(false) }
    BackHandler { showExitDialog = true }
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
                text      = uiStrings.tournamentTitle,
                style     = MaterialTheme.typography.titleLarge,
                modifier  = Modifier.weight(1f),
                textAlign = TextAlign.Center,
                color     = DeepPurple,
                fontWeight = FontWeight.Bold
            )
            // Round indicator
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(40.dp))
                    .background(SoftPurple)
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Text(
                    text  = uiStrings.tournamentRound.format(state.round + 1),
                    style = MaterialTheme.typography.bodyMedium,
                    color = DeepPurple
                )
            }
            Spacer(Modifier.width(8.dp))
        }
        if (roundJustCompleted) {
            Text(
                text      = uiStrings.tournamentRoundComplete.format(state.round),
                style     = MaterialTheme.typography.bodyMedium,
                color     = MedText,
                modifier  = Modifier.padding(horizontal = 24.dp),
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(8.dp))
        }
        // Scrollable bracket — both directions
        Box(
            modifier = Modifier
                .weight(1f)
                .horizontalScroll(rememberScrollState())
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 12.dp, vertical = 8.dp)
        ) {
            BracketView(state = state, uiStrings = uiStrings)
        }
        // Start / Continue button
        Button(
            onClick  = onStart,
            modifier = Modifier
                .fillMaxWidth()
                .height(60.dp)
                .padding(horizontal = 24.dp, vertical = 4.dp),
            shape  = RoundedCornerShape(20.dp),
            colors = ButtonDefaults.buttonColors(containerColor = TournamentAccent)
        ) {
            Text(
                text  = if (roundJustCompleted) uiStrings.tournamentContinue
                else uiStrings.tournamentStart,
                style = MaterialTheme.typography.labelLarge,
                color = Color.White
            )
        }
        Spacer(Modifier.height(12.dp))
    }
    if (showExitDialog) {
        AlertDialog(
            onDismissRequest = { showExitDialog = false },
            title   = { Text(uiStrings.tournamentExitTitle) },
            text    = { Text(uiStrings.tournamentExitBody) },
            confirmButton = {
                TextButton(onClick = { showExitDialog = false; onBack() }) {
                    Text(uiStrings.tournamentExitConfirm,
                        color = MaterialTheme.colorScheme.error,
                        fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showExitDialog = false }) {
                    Text(uiStrings.tournamentExitCancel, color = DeepPurple)
                }
            }
        )
    }
}
// ── Bracket visual ────────────────────────────────────────────────────────────
// Fixed slot dimensions — all maths derive from these two values.
private val SLOT_H   = 48.dp
private val SLOT_GAP = 4.dp
@Composable
private fun BracketView(state: TournamentState, uiStrings: UiStrings) {
    val roundLabels = listOf(
        uiStrings.tournamentR16,
        uiStrings.tournamentQF,
        uiStrings.tournamentSF,
        uiStrings.tournamentFinal
    )
    // ── Reconstruct slot lists per round ─────────────────────────────────────
    // R16 always shows the original 16 (stored in initialSlots).
    // Later rounds show winners once decided, or TBD nulls if not yet reached.
    val r0: List<GalleryItem?> = state.initialSlots +
            List(maxOf(0, 16 - state.initialSlots.size)) { null }
    val r1: List<GalleryItem?> = when {
        state.round < 1  -> List(8) { null }
        state.round == 1 -> state.slots + List(maxOf(0, 8 - state.slots.size)) { null }
        else             -> (state.winners.getOrNull(0) ?: emptyList()) +
                List(maxOf(0, 8 - (state.winners.getOrNull(0)?.size ?: 0))) { null }
    }
    val r2: List<GalleryItem?> = when {
        state.round < 2  -> List(4) { null }
        state.round == 2 -> state.slots + List(maxOf(0, 4 - state.slots.size)) { null }
        else             -> (state.winners.getOrNull(1) ?: emptyList()) +
                List(maxOf(0, 4 - (state.winners.getOrNull(1)?.size ?: 0))) { null }
    }
    val r3: List<GalleryItem?> = when {
        state.round < 3  -> List(2) { null }
        state.round == 3 -> state.slots + List(maxOf(0, 2 - state.slots.size)) { null }
        else             -> (state.winners.getOrNull(2) ?: emptyList()) +
                List(maxOf(0, 2 - (state.winners.getOrNull(2)?.size ?: 0))) { null }
    }
    val champion: GalleryItem? = if (state.slots.size == 1) state.slots[0] else null
    val allRounds   = listOf(r0, r1, r2, r3)
    val roundCounts = listOf(16, 8, 4, 2)   // slots per round
    // ── Height maths ─────────────────────────────────────────────────────────
    // Total height occupied by R16 (16 slots, 15 gaps):
    //   totalH = 16 * SLOT_H + 15 * SLOT_GAP
    //
    // In each subsequent round, a slot is centred over its 2 "parents".
    // The stride (centre-to-centre distance between adjacent slots) doubles each round:
    //   stride[0] = SLOT_H + SLOT_GAP
    //   stride[N] = stride[N-1] * 2
    //
    // The top offset of slot i in round N:
    //   topOffset = firstSlotTop[N] + i * stride[N]
    //   where firstSlotTop[N] = (stride[N] - SLOT_H) / 2   (centres first slot over first pair)
    val density = androidx.compose.ui.platform.LocalDensity.current
    val slotHPx  = with(density) { SLOT_H.toPx() }
    val slotGPx  = with(density) { SLOT_GAP.toPx() }
    val stride0  = slotHPx + slotGPx
    // Total canvas height = same as R16 column
    val totalHeightPx = 16 * slotHPx + 15 * slotGPx
    val totalHeightDp = with(density) { totalHeightPx.toDp() }
    // Per-round: (stride, firstSlotTopPx)
    val roundLayouts = (0..3).map { r ->
        val stride    = stride0 * (1 shl r)   // doubles each round
        val firstTop  = (stride - slotHPx) / 2f
        Pair(stride, firstTop)
    }
    // Champion: centred over the 2 finalists
    val champStride   = stride0 * 16f
    val champFirstTop = (champStride - slotHPx) / 2f
    // ── Draw ─────────────────────────────────────────────────────────────────
    val colWidth = 90.dp

    Row(
        modifier              = Modifier.wrapContentWidth(),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        allRounds.forEachIndexed { roundIndex, slots ->
            val (_, firstTop) = roundLayouts[roundIndex]
            val stride        = roundLayouts[roundIndex].first
            val isCurrent     = roundIndex == state.round
            val isDone        = roundIndex < state.round

            val roundWinnerIds: Set<String> = state.winners.getOrNull(roundIndex)
                ?.mapNotNull { it?.id }?.toSet() ?: emptySet()

            Column(modifier = Modifier.width(colWidth)) {
                Text(
                    text       = roundLabels[roundIndex],
                    style      = MaterialTheme.typography.labelSmall,
                    color      = if (isCurrent) TournamentAccent else MedText,
                    fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Normal,
                    textAlign  = TextAlign.Center,
                    modifier   = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(4.dp))

                Box(modifier = Modifier.width(colWidth).height(totalHeightDp)) {
                    slots.forEachIndexed { slotIndex, image ->
                        val topPx    = firstTop + slotIndex * stride
                        val topDp    = with(density) { topPx.toDp() }
                        val eliminated = isDone && image != null && image.id !in roundWinnerIds
                        Box(modifier = Modifier.width(colWidth).height(SLOT_H).absoluteOffset(y = topDp)) {
                            BracketSlot(image = image, isCurrent = isCurrent, isDone = isDone, isEliminated = eliminated)
                        }
                    }
                }
            }
        }

        // Champion column
        Column(modifier = Modifier.width(colWidth)) {
            Text(
                text       = uiStrings.tournamentChampion,
                style      = MaterialTheme.typography.labelSmall,
                color      = if (champion != null) TournamentAccent else MedText,
                fontWeight = FontWeight.Bold,
                textAlign  = TextAlign.Center,
                modifier   = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(4.dp))
            Box(modifier = Modifier.width(colWidth).height(totalHeightDp)) {
                val topDp = with(density) { champFirstTop.toDp() }
                Box(modifier = Modifier.width(colWidth).height(SLOT_H).absoluteOffset(y = topDp)) {
                    BracketSlot(image = champion, isCurrent = false, isDone = champion != null)
                }
            }
        }
    }
}

@Composable
private fun BracketSlot(
    image: GalleryItem?,
    isCurrent: Boolean,
    isDone: Boolean,
    isEliminated: Boolean = false
) {
    val borderColor = when {
        isDone    -> TournamentAccent
        isCurrent -> DeepPurple.copy(alpha = 0.4f)
        else      -> Color.LightGray
    }
    val grayscaleFilter = remember {
        ColorFilter.colorMatrix(ColorMatrix().apply { setToSaturation(0f) })
    }
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(44.dp)
            .clip(RoundedCornerShape(8.dp))
            .border(1.dp, borderColor, RoundedCornerShape(8.dp))
            .background(if (image != null) Color.Transparent else CardBg),
        contentAlignment = Alignment.Center
    ) {
        if (image != null) {
            val imageFile = File(image.imageUrl)
            AsyncImage(
                model              = if (imageFile.exists()) imageFile else image.imageUrl,
                contentDescription = null,
                contentScale       = ContentScale.Crop,
                colorFilter        = if (isEliminated) grayscaleFilter else null,
                modifier           = Modifier.fillMaxSize().clip(RoundedCornerShape(8.dp))
            )
        } else {
            Text("?", color = Color.LightGray, fontSize = 14.sp)
        }
    }
}

// ── Matchup screen ────────────────────────────────────────────────────────────

@Composable
private fun TournamentMatchupScreen(
    uiStrings: UiStrings,
    state: TournamentState,
    onVote: (GalleryItem) -> Unit,
    onBack: () -> Unit
) {
    var showExitDialog by remember { mutableStateOf(false) }
    BackHandler { showExitDialog = true }

    val left  = state.currentLeft
    val right = state.currentRight

    val roundName = listOf(
        uiStrings.tournamentR16,
        uiStrings.tournamentQF,
        uiStrings.tournamentSF,
        uiStrings.tournamentFinal
    ).getOrElse(state.round) { "" }

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
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text      = roundName,
                    style     = MaterialTheme.typography.titleMedium,
                    textAlign = TextAlign.Center,
                    modifier  = Modifier.fillMaxWidth(),
                    color     = DeepPurple,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text      = uiStrings.tournamentMatchProgress
                        .format(state.matchIndex + 1, state.matchCount),
                    style     = MaterialTheme.typography.bodySmall,
                    textAlign = TextAlign.Center,
                    modifier  = Modifier.fillMaxWidth(),
                    color     = MedText
                )
            }
            Spacer(Modifier.width(48.dp))
        }

        // VS label
        Box(
            modifier = Modifier.fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text  = uiStrings.tournamentVs,
                style = MaterialTheme.typography.headlineMedium,
                color = TournamentAccent,
                fontWeight = FontWeight.Black
            )
        }

        Spacer(Modifier.height(12.dp))

        // Side-by-side images
        Row(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            listOf(left, right).forEachIndexed { idx, image ->
                Column(
                    modifier            = Modifier.weight(1f),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Image — fixed height, full image visible (no cropping)
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(260.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(CardBg),
                        contentAlignment = Alignment.Center
                    ) {
                        if (image != null) {
                            val imageFile = File(image.imageUrl)
                            AsyncImage(
                                model              = if (imageFile.exists()) imageFile else image.imageUrl,
                                contentDescription = null,
                                contentScale       = ContentScale.Fit,
                                modifier           = Modifier.fillMaxSize()
                            )
                        } else {
                            Text("?", fontSize = 32.sp, color = MedText)
                        }
                    }

                    Spacer(Modifier.height(10.dp))

                    // Vote button
                    Button(
                        onClick  = { if (image != null) onVote(image) },
                        enabled  = image != null,
                        modifier = Modifier.fillMaxWidth().height(48.dp),
                        shape    = RoundedCornerShape(14.dp),
                        colors   = ButtonDefaults.buttonColors(containerColor = TournamentAccent)
                    ) {
                        Text(
                            text  = uiStrings.tournamentVote,
                            style = MaterialTheme.typography.labelLarge,
                            color = Color.White
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(16.dp))
    }

    if (showExitDialog) {
        AlertDialog(
            onDismissRequest = { showExitDialog = false },
            title   = { Text(uiStrings.tournamentExitTitle) },
            text    = { Text(uiStrings.tournamentExitBody) },
            confirmButton = {
                TextButton(onClick = { showExitDialog = false; onBack() }) {
                    Text(uiStrings.tournamentExitConfirm,
                        color = MaterialTheme.colorScheme.error,
                        fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showExitDialog = false }) {
                    Text(uiStrings.tournamentExitCancel, color = DeepPurple)
                }
            }
        )
    }
}

// ── Won screen ────────────────────────────────────────────────────────────────

@Composable
private fun TournamentWonScreen(
    uiStrings: UiStrings,
    champion: GalleryItem?,
    onClose: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.height(24.dp))
        Text("🏆", fontSize = 64.sp)
        Spacer(Modifier.height(16.dp))
        Text(
            text      = uiStrings.tournamentWonTitle,
            style     = MaterialTheme.typography.displaySmall,
            textAlign = TextAlign.Center,
            fontWeight = FontWeight.Bold,
            color     = DeepPurple
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text      = uiStrings.tournamentWonBody,
            style     = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            color     = MedText
        )
        Spacer(Modifier.height(32.dp))

        // Champion image
        if (champion != null) {
            val imageFile = File(champion.imageUrl)
            Box(
                modifier = Modifier
                    .size(260.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .border(3.dp, TournamentAccent, RoundedCornerShape(24.dp))
            ) {
                AsyncImage(
                    model              = if (imageFile.exists()) imageFile else champion.imageUrl,
                    contentDescription = null,
                    contentScale       = ContentScale.Crop,
                    modifier           = Modifier.fillMaxSize()
                )
            }
            Spacer(Modifier.height(16.dp))
            Text(
                text       = champion.titleLocal,
                style      = MaterialTheme.typography.titleMedium,
                textAlign  = TextAlign.Center,
                fontWeight = FontWeight.Bold,
                color      = DeepPurple,
                modifier   = Modifier.padding(horizontal = 16.dp)
            )
        }

        Spacer(Modifier.weight(1f))

        Button(
            onClick  = onClose,
            modifier = Modifier.fillMaxWidth().height(60.dp),
            shape    = RoundedCornerShape(20.dp),
            colors   = ButtonDefaults.buttonColors(containerColor = TournamentAccent)
        ) {
            Text(uiStrings.tournamentWonClose,
                style = MaterialTheme.typography.labelLarge,
                color = Color.White)
        }
    }
}