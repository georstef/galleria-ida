package com.galleriaida.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.galleriaida.data.GalleryItem
import com.galleriaida.ui.theme.*
import com.galleriaida.viewmodel.AppViewModel
import com.galleriaida.viewmodel.UiState
import java.io.File

@Composable
fun GalleryScreen(
    viewModel: AppViewModel,
    onBack: () -> Unit,
    onSettings: () -> Unit,
    onCreateImage: () -> Unit,
    onEditProfile: () -> Unit
) {
    val player     by viewModel.currentPlayer.collectAsState()
    val gallery    by viewModel.gallery.collectAsState()
    val uiState    by viewModel.uiState.collectAsState()
    val uiStrings  by viewModel.uiStrings.collectAsState()

    val playerGallery = gallery.filter { it.playerId == player?.id }
    val stars     = player?.stars ?: 0
    val canAfford = (stars >= 100) || (player?.name == "George S.")
    val initial   = player?.name?.firstOrNull()?.uppercase() ?: "?"

    // Full-screen viewer state — null = closed
    var fullscreenItem by remember { mutableStateOf<GalleryItem?>(null) }

    // When a new image is created the gallery re-composes; auto-open the latest item
    var lastSeenCount by remember { mutableStateOf(playerGallery.size) }
    LaunchedEffect(playerGallery.size) {
        if (playerGallery.size > lastSeenCount && playerGallery.isNotEmpty()) {
            fullscreenItem = playerGallery.last()
        }
        lastSeenCount = playerGallery.size
    }

    // Full-screen viewer takes over the whole screen
    fullscreenItem?.let { item ->
        ImageFullscreenScreen(
            item    = item,
            onClose = { fullscreenItem = null }
        )
        return
    }

    // ── Gallery list ─────────────────────────────────────────────────────────
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
    ) {
        // Top bar
        Row(
            modifier              = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment     = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = DeepPurple)
            }

            // Stars badge (centre)
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(40.dp))
                    .background(LemonYellow)
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("⭐ $stars", style = MaterialTheme.typography.titleMedium, color = DeepPurple)
            }

            // Player avatar (top-right, like other screens) → opens profile
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

        Text(
            uiStrings.galleryTitle,
            style    = MaterialTheme.typography.titleLarge,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp),
            textAlign = TextAlign.Center
        )

        Spacer(Modifier.height(16.dp))

        when (uiState) {
            is UiState.Error -> {
                Box(
                    Modifier.weight(1f).fillMaxWidth().padding(28.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        (uiState as UiState.Error).message,
                        color     = ErrorRed,
                        style     = MaterialTheme.typography.bodyLarge,
                        textAlign = TextAlign.Center
                    )
                }
                LaunchedEffect(Unit) { viewModel.clearUiState() }
            }
            else -> {
                if (playerGallery.isEmpty()) {
                    Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier            = Modifier.padding(28.dp)
                        ) {
                            Text("🌟", style = MaterialTheme.typography.displayLarge)
                            Spacer(Modifier.height(16.dp))
                            Text(
                                uiStrings.galleryEmpty,
                                style     = MaterialTheme.typography.bodyLarge,
                                textAlign = TextAlign.Center,
                                color     = MedText
                            )
                        }
                    }
                } else {
                    LazyVerticalGrid(
                        columns             = GridCells.Fixed(3),
                        modifier            = Modifier.weight(1f).padding(horizontal = 12.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement   = Arrangement.spacedBy(8.dp)
                    ) {
                        items(playerGallery) { item ->
                            GalleryCard(item = item, onClick = { fullscreenItem = item })
                        }
                    }
                }
            }
        }

        // Bottom bar: create button
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            if (!canAfford) {
                Text(
                    uiStrings.galleryNeedStars.format(stars),
                    style     = MaterialTheme.typography.bodySmall,
                    color     = MedText,
                    textAlign = TextAlign.Center,
                    modifier  = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(4.dp))
            }
            Button(
                onClick  = onCreateImage,
                enabled  = canAfford,
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape    = RoundedCornerShape(16.dp),
                colors   = ButtonDefaults.buttonColors(
                    containerColor         = ButtonPrimary,
                    disabledContainerColor = DisabledGray
                )
            ) {
                Text(uiStrings.galleryCreateButton, style = MaterialTheme.typography.labelLarge)
            }
        }
    }
}

// ── Gallery card (thumbnail + title) ─────────────────────────────────────────

@Composable
fun GalleryCard(item: GalleryItem, onClick: () -> Unit) {
    val imageModel = remember(item.imageUrl) {
        val file = File(item.imageUrl)
        if (file.exists()) file else item.imageUrl
    }
    Column(
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(CardBg)
            .clickable { onClick() }
            .padding(5.dp)
    ) {
        AsyncImage(
            model              = imageModel,
            contentDescription = item.titleLocal,
            contentScale       = ContentScale.Crop,
            modifier           = Modifier
                .fillMaxWidth()
                .aspectRatio(768f / 1200f)
                .clip(RoundedCornerShape(8.dp))
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text       = item.titleLocal.ifBlank { item.titleEn },
            fontSize   = 9.sp,
            color      = DeepPurple,
            maxLines   = 2,
            overflow   = TextOverflow.Ellipsis,
            fontWeight = FontWeight.SemiBold
        )
    }
}

// ── Full-screen image viewer ──────────────────────────────────────────────────

@Composable
fun ImageFullscreenScreen(item: GalleryItem, onClose: () -> Unit) {
    val imageModel = remember(item.imageUrl) {
        val file = File(item.imageUrl)
        if (file.exists()) file else item.imageUrl
    }

    var showMetadata by remember { mutableStateOf(false) }

    // Long-press detection via pointer input
    val interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }

    // Metadata JSON
    val metadataJson = remember(item) {
        org.json.JSONObject().apply {
            put("id",            item.id)
            put("playerId",      item.playerId)
            put("titleEn",       item.titleEn)
            put("phraseEn",      item.phraseEn)
            put("titleLocal",    item.titleLocal)
            put("phraseLocal",   item.phraseLocal)
            put("wordCharacter", item.wordCharacter)
            put("wordAction",    item.wordAction)
            put("wordPlace",     item.wordPlace)
            put("imageUrl",      item.imageUrl)
            put("cost",          item.cost)
        }.toString(2)
    }

    if (showMetadata) {
        AlertDialog(
            onDismissRequest = { showMetadata = false },
            title = { Text("Image Metadata", style = MaterialTheme.typography.titleMedium) },
            text  = {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 420.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    Text(
                        text  = metadataJson,
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontFamily = FontFamily.Monospace,
                            fontSize   = 11.sp
                        ),
                        color = DarkText
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { showMetadata = false }) {
                    Text("Close", style = MaterialTheme.typography.bodyLarge)
                }
            }
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .statusBarsPadding()
    ) {
        Column(
            modifier            = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Image — long-press reveals metadata
            AsyncImage(
                model              = imageModel,
                contentDescription = item.titleLocal,
                contentScale       = ContentScale.Fit,
                modifier           = Modifier
                    .fillMaxWidth()
                    .weight(1f, fill = false)
                    .clip(RoundedCornerShape(0.dp))
                    .pointerInput(Unit) {
                        detectTapGestures(
                            onLongPress = { showMetadata = true }
                        )
                    }
            )

            Spacer(Modifier.height(16.dp))

            // Player-language title only
            Text(
                text      = item.titleLocal.ifBlank { item.titleEn },
                style     = MaterialTheme.typography.titleMedium,
                color     = Color.White,
                textAlign = TextAlign.Center,
                modifier  = Modifier.padding(horizontal = 24.dp)
            )

            Spacer(Modifier.height(20.dp))

            // Close button
            Button(
                onClick  = onClose,
                modifier = Modifier
                    .padding(horizontal = 48.dp)
                    .fillMaxWidth()
                    .height(52.dp),
                shape  = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color.White.copy(alpha = 0.15f))
            ) {
                Icon(Icons.Default.Close, contentDescription = null, tint = Color.White)
                Spacer(Modifier.width(8.dp))
                Text("Close", style = MaterialTheme.typography.labelLarge, color = Color.White)
            }

            Spacer(Modifier.height(24.dp))
        }
    }
}