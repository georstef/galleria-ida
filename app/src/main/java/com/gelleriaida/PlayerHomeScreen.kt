package com.gelleriaida.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gelleriaida.ui.theme.*
import com.gelleriaida.viewmodel.AppViewModel

@Composable
fun PlayerHomeScreen(
    viewModel: AppViewModel,
    onStartLesson: () -> Unit,
    onGallery: () -> Unit,
    onSettings: () -> Unit,
    onEditProfile: () -> Unit,
    onBack: () -> Unit
) {
    val player by viewModel.currentPlayer.collectAsState()
    val initial = player?.name?.firstOrNull()?.uppercase() ?: "?"

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            // Top bar
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

                // Stars badge
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
                        text = "${player?.stars ?: 0}",
                        style = MaterialTheme.typography.titleMedium,
                        color = DeepPurple
                    )
                }

                // Avatar circle → taps to edit profile
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(SoftPurple)
                        .clickable { onEditProfile() },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = initial,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = DeepPurple
                    )
                }
            }

            // Main content
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 28.dp)
                    .offset(y = (-40).dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "Hi, ${player?.name ?: ""}! 👋",
                    style = MaterialTheme.typography.displayLarge,
                    textAlign = TextAlign.Center
                )

                Spacer(Modifier.height(12.dp))

                Text(
                    text = "What do you want to do?",
                    style = MaterialTheme.typography.bodyLarge,
                    textAlign = TextAlign.Center,
                    color = MedText
                )

                Spacer(Modifier.height(56.dp))

                Button(
                    onClick = onStartLesson,
                    modifier = Modifier.fillMaxWidth().height(72.dp),
                    shape = RoundedCornerShape(24.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = ButtonPrimary)
                ) {
                    Text("📚  Start Lesson", style = MaterialTheme.typography.labelLarge)
                }

                Spacer(Modifier.height(20.dp))

                Button(
                    onClick = onGallery,
                    modifier = Modifier.fillMaxWidth().height(72.dp),
                    shape = RoundedCornerShape(24.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = ButtonSecondary)
                ) {
                    Text("🖼️  My Gallery", style = MaterialTheme.typography.labelLarge)
                }
            }
        }

        // Settings cog — bottom right, floating
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