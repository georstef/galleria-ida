package com.galleriaida.ui.screens

import android.app.Activity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
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

    // Paint the system bars black while this screen is shown, and restore on exit,
    // so the whole screen (including status/navigation bar areas) matches the splash.
    val view = LocalView.current
    if (!view.isInEditMode) {
        DisposableEffect(Unit) {
            val window = (view.context as Activity).window
            val previousStatus = window.statusBarColor
            val previousNav    = window.navigationBarColor
            val insetsController = WindowCompat.getInsetsController(window, view)
            val previousLightStatus = insetsController.isAppearanceLightStatusBars
            val previousLightNav    = insetsController.isAppearanceLightNavigationBars

            window.statusBarColor     = Color.Black.toArgb()
            window.navigationBarColor = Color.Black.toArgb()
            insetsController.isAppearanceLightStatusBars     = false
            insetsController.isAppearanceLightNavigationBars = false

            onDispose {
                window.statusBarColor     = previousStatus
                window.navigationBarColor = previousNav
                insetsController.isAppearanceLightStatusBars     = previousLightStatus
                insetsController.isAppearanceLightNavigationBars = previousLightNav
            }
        }
    }

    val shownAt = remember { System.currentTimeMillis() }
    val minDisplayMs = 600L

    suspend fun finish() {
        val elapsed = System.currentTimeMillis() - shownAt
        if (elapsed < minDisplayMs) delay(minDisplayMs - elapsed)
        onReady()
    }

    LaunchedEffect(translating) {
        if (translating) {
            translatingStarted = true
        } else if (translatingStarted) {
            // Translation was in progress and just finished
            finish()
        } else {
            // translating is false on arrival. Translation starts almost immediately if
            // needed, so poll briefly. If it hasn't started within ~150ms, the cache was
            // complete and we can proceed (after the minimum display time).
            var waited = 0
            while (waited < 150 && !translating) {
                delay(30)
                waited += 30
            }
            if (!translating) finish()
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