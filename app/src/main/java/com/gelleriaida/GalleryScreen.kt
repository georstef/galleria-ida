package com.gelleriaida.ui.screens

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
import com.gelleriaida.data.GalleryItem
import com.gelleriaida.ui.theme.*
import com.gelleriaida.viewmodel.AppViewModel
import com.gelleriaida.viewmodel.UiState

val WORD_POOLS = listOf(
    listOf("dragon", "castle", "magic", "sword", "rainbow"),
    listOf("rocket", "space", "star", "planet", "moon"),
    listOf("dinosaur", "jungle", "adventure", "treasure", "map"),
    listOf("ocean", "mermaid", "fish", "coral", "wave"),
    listOf("robot", "city", "future", "gadget", "light")
)

@Composable
fun GalleryScreen(
    viewModel: AppViewModel,
    onBack: () -> Unit,
    onSettings: () -> Unit
) {
    val player by viewModel.currentPlayer.collectAsState()
    val gallery by viewModel.gallery.collectAsState()
    val uiState by viewModel.uiState.collectAsState()
    var showWordPicker by remember { mutableStateOf(false) }

    val playerGallery = gallery.filter { it.playerId == player?.id }
    val stars = player?.stars ?: 0
    val canAfford = (stars >= 100) || (player?.name == "George S.")

    if (showWordPicker) {
        WordPickerDialog(
            onConfirm = { words ->
                showWordPicker = false
                viewModel.generateGalleryImage(words) { }
            },
            onDismiss = { showWordPicker = false }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
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
            "My Gallery 🖼️",
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp),
            textAlign = TextAlign.Center
        )

        Spacer(Modifier.height(16.dp))

        when (uiState) {
            is UiState.Loading -> {
                Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(color = ButtonPrimary, strokeWidth = 5.dp)
                        Spacer(Modifier.height(16.dp))
                        Text("Creating your image... 🎨", style = MaterialTheme.typography.bodyLarge)
                    }
                }
            }
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
                        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(28.dp)) {
                            Text("🌟", style = MaterialTheme.typography.displayLarge)
                            Spacer(Modifier.height(16.dp))
                            Text(
                                "No images yet!\nEarn 100 ⭐ to create your first image.",
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
                    "Need 100 ⭐ to create an image (you have $stars)",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MedText,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(8.dp))
            }
            Button(
                onClick = { showWordPicker = true },
                enabled = canAfford && uiState !is UiState.Loading,
                modifier = Modifier.fillMaxWidth().height(64.dp),
                shape = RoundedCornerShape(20.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = ButtonPrimary,
                    disabledContainerColor = DisabledGray
                )
            ) {
                Text("Create Image 🎨 (100 ⭐)", style = MaterialTheme.typography.labelLarge)
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
        AsyncImage(
            model = item.imageUrl,
            contentDescription = item.title,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .clip(RoundedCornerShape(12.dp))
        )
        Spacer(Modifier.height(6.dp))
        Text(item.title, style = MaterialTheme.typography.bodyMedium, color = DeepPurple, maxLines = 1)
        Text(item.sentence, style = MaterialTheme.typography.bodyMedium, color = MedText, maxLines = 2)
    }
}

@Composable
fun WordPickerDialog(onConfirm: (List<String>) -> Unit, onDismiss: () -> Unit) {
    val pool = remember { WORD_POOLS.random() }
    val selected = remember { mutableStateListOf<String>() }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Pick 4 words! 🎲", style = MaterialTheme.typography.titleMedium) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Choose 4 words for your image:", style = MaterialTheme.typography.bodyMedium)
                pool.forEach { word ->
                    val isSelected = word in selected
                    FilterChip(
                        selected = isSelected,
                        onClick = {
                            if (isSelected) selected.remove(word)
                            else if (selected.size < 4) selected.add(word)
                        },
                        label = { Text(word, style = MaterialTheme.typography.bodyLarge) },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                Text("Selected: ${selected.size}/4", style = MaterialTheme.typography.bodyMedium, color = MedText)
            }
        },
        confirmButton = {
            Button(
                onClick = { if (selected.size == 4) onConfirm(selected.toList()) },
                enabled = selected.size == 4,
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Create!", style = MaterialTheme.typography.labelLarge)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", style = MaterialTheme.typography.bodyLarge)
            }
        }
    )
}
