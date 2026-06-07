package com.galleriaida.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.galleriaida.ui.theme.*
import com.galleriaida.viewmodel.AppViewModel
import com.galleriaida.R

@Composable
fun PlayerLoadingScreen(
    viewModel: AppViewModel,
    onReady: () -> Unit
) {
    val translating by viewModel.translating.collectAsState()

    LaunchedEffect(translating) {
        if (!translating) {
            onReady()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            AsyncImage(
                model = R.mipmap.galleria_ida_logo,
                contentDescription = "Logo",
                modifier = Modifier.size(120.dp)
            )
            Spacer(Modifier.height(40.dp))
            CircularProgressIndicator(color = ButtonPrimary, strokeWidth = 4.dp)
        }
    }
}