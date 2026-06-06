package com.gelleriaida.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.gelleriaida.data.Player
import com.gelleriaida.ui.theme.*
import com.gelleriaida.viewmodel.AppViewModel

@Composable
fun PlayerSelectionScreen(
    viewModel: AppViewModel,
    onPlayerSelected: () -> Unit,
    onNewPlayer: () -> Unit
) {
    val players by viewModel.players.collectAsState()

    // First launch — no players yet, go straight to setup
    val playersLoaded by viewModel.playersLoaded.collectAsState()

    LaunchedEffect(playersLoaded) {
        if (playersLoaded && players.isEmpty()) onNewPlayer()
    }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
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
        }

        Spacer(Modifier.height(24.dp))

        Button(
            onClick = onNewPlayer,
            modifier = Modifier
                .fillMaxWidth()
                .height(64.dp),
            shape = RoundedCornerShape(20.dp),
            colors = ButtonDefaults.buttonColors(containerColor = ButtonSecondary)
        ) {
            Text("+ Add New Player", style = MaterialTheme.typography.labelLarge)
        }

        Spacer(Modifier.height(16.dp))
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
