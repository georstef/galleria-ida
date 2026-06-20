package com.galleriaida.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.galleriaida.data.Player
import com.galleriaida.ui.theme.*
import com.galleriaida.viewmodel.AppViewModel

@Composable
fun PlayerSelectionScreen(
    viewModel: AppViewModel,
    onPlayerSelected: () -> Unit,
    onNewPlayer: () -> Unit,
    onSettings: () -> Unit
) {
    val players by viewModel.players.collectAsState()
    val playersLoaded by viewModel.playersLoaded.collectAsState()

    LaunchedEffect(playersLoaded) {
        if (playersLoaded && players.isEmpty()) onNewPlayer()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(48.dp))

            Text(
                text = "Who is playing? 👋",
                style = MaterialTheme.typography.titleLarge,
                textAlign = TextAlign.Center
            )

            Spacer(Modifier.height(8.dp))

            Text(
                text = "Tap your name to start",
                style = MaterialTheme.typography.bodyMedium,
                color = MedText,
                textAlign = TextAlign.Center
            )

            Spacer(Modifier.height(32.dp))

            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                items(players) { player ->
                    PlayerCard(player = player, onClick = {
                        viewModel.selectPlayer(player)
                        onPlayerSelected()
                    })
                }

                // "Add New Player" as the last item in the list
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(20.dp))
                            .background(SoftPurple.copy(alpha = 0.5f))
                            .clickable { onNewPlayer() }
                            .padding(20.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = "+ Add New Player",
                            style = MaterialTheme.typography.titleMedium,
                            color = DeepPurple
                        )
                    }
                }
            }

            Spacer(Modifier.height(80.dp)) // room for the FAB
        }

        // Settings cog — bottom right, floating (same as all other screens)
        IconButton(
            onClick = onSettings,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(20.dp)
                .size(52.dp)
                .clip(CircleShape)
                .background(SoftPurple)
        ) {
            Icon(
                Icons.Default.Settings,
                contentDescription = "Settings",
                tint = DeepPurple,
                modifier = Modifier.size(28.dp)
            )
        }
    }
}

@Composable
fun PlayerCard(player: Player, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(SoftPurple)
            .clickable { onClick() }
            .padding(20.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column {
            Text(
                text = player.name,
                style = MaterialTheme.typography.titleMedium
            )
            if (player.schoolClass.isNotBlank()) {
                Text(
                    text = "Class ${player.schoolClass}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MedText
                )
            }
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "${player.stars}",
                style = MaterialTheme.typography.titleMedium,
                color = DeepPurple
            )
            Spacer(Modifier.width(4.dp))
            Text(text = "⭐", style = MaterialTheme.typography.titleMedium)
        }
    }
}