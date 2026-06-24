package com.galleriaida.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.galleriaida.ui.theme.*
import com.galleriaida.viewmodel.AppViewModel
import com.galleriaida.R
import kotlinx.coroutines.delay

@Composable
fun PlayerLoadingScreen(
    viewModel: AppViewModel,
    onReady: () -> Unit
) {
    val translating by viewModel.translating.collectAsState()
    var translatingStarted by remember { mutableStateOf(false) }

    LaunchedEffect(translating) {
        if (translating) {
            translatingStarted = true
        } else if (translatingStarted) {
            // Translation was in progress and just finished
            onReady()
        } else {
            // translating is false on arrival — give it a short window to start
            delay(500)
            if (!translating) onReady()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            AsyncImage(
                model              = R.drawable.logo,
                contentDescription = "Logo",
                modifier           = Modifier
                    .size(120.dp)
                    .clip(RoundedCornerShape(28.dp))
            )
            Spacer(Modifier.height(48.dp))
            CircularProgressIndicator(
                color       = Color.White,
                strokeWidth = 3.dp,
                modifier    = Modifier.size(32.dp)
            )
        }
    }
}