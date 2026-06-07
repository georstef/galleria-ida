package com.galleriaida.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
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
    onCreateImage: () -> Unit
) {
    val player by viewModel.currentPlayer.collectAsState()
    val gallery by viewModel.gallery.collectAsState()
    val uiState by viewModel.uiState.collectAsState()
    val uiStrings by viewModel.uiStrings.collectAsState()

    val playerGallery = gallery.filter { it.playerId == player?.id }
    val stars = player?.stars ?: 0
    val canAfford = (stars >= 100) || (player?.name == "George S.")

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
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
                Text("⭐ $stars", style = MaterialTheme.typography.titleMedium, color = DeepPurple)
            }
            IconButton(onClick = onSettings) {
                Icon(Icons.Default.Settings, contentDescription = "Settings", tint = DeepPurple)
            }
        }

        Text(
            uiStrings.galleryTitle,
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp),
            textAlign = TextAlign.Center
        )

        Spacer(Modifier.height(16.dp))

        when (uiState) {
            is UiState.Error -> {
                Box(Modifier.weight(1f).fillMaxWidth().padding(28.dp), contentAlignment = Alignment.Center) {
                    Text(
                        (uiState as UiState.Error).message,
                        color = ErrorRed,
                        style = MaterialTheme.typography.bodyLarge,
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
                            modifier = Modifier.padding(28.dp)
                        ) {
                            Text("🌟", style = MaterialTheme.typography.displayLarge)
                            Spacer(Modifier.height(16.dp))
                            Text(
                                uiStrings.galleryEmpty,
                                style = MaterialTheme.typography.bodyLarge,
                                textAlign = TextAlign.Center,
                                color = MedText
                            )
                        }
                    }
                } else {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(2),
                        modifier = Modifier.weight(1f).padding(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(playerGallery) { item ->
                            GalleryCard(item)
                        }
                    }
                }
            }
        }

        Column(modifier = Modifier.padding(horizontal = 24.dp, vertical = 16.dp)) {
            if (!canAfford) {
                Text(
                    uiStrings.galleryNeedStars.format(stars),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MedText,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(8.dp))
            }
            Button(
                onClick = onCreateImage,
                enabled = canAfford,
                modifier = Modifier.fillMaxWidth().height(64.dp),
                shape = RoundedCornerShape(20.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = ButtonPrimary,
                    disabledContainerColor = DisabledGray
                )
            ) {
                Text(uiStrings.galleryCreateButton, style = MaterialTheme.typography.labelLarge)
            }
        }
    }
}

@Composable
fun GalleryCard(item: GalleryItem) {
    Column(
        modifier = Modifier
            .clip(RoundedCornerShape(16.dp))
            .background(CardBg)
            .padding(8.dp)
    ) {
        // Load from local file path if it exists, otherwise treat as URL
        val imageModel = remember(item.imageUrl) {
            val file = File(item.imageUrl)
            if (file.exists()) file else item.imageUrl
        }
        AsyncImage(
            model = imageModel,
            contentDescription = item.phraseLocal,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .clip(RoundedCornerShape(12.dp))
        )
        Spacer(Modifier.height(6.dp))
        Text(item.phraseLocal, style = MaterialTheme.typography.bodyMedium, color = DeepPurple, maxLines = 2)
        Text(item.sentence, style = MaterialTheme.typography.bodyMedium, color = MedText, maxLines = 1)
    }
}