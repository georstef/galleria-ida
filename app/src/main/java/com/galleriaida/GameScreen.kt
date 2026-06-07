package com.galleriaida.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
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
import com.galleriaida.network.MathQuestion
import com.galleriaida.ui.theme.*
import com.galleriaida.viewmodel.AppViewModel
import com.galleriaida.viewmodel.UiState

@Composable
fun GameScreen(
    viewModel: AppViewModel,
    onFinished: () -> Unit,
    onSettings: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val questions by viewModel.questions.collectAsState()
    var currentIndex by remember { mutableIntStateOf(0) }
    var userAnswer by remember { mutableStateOf("") }
    var totalEarned by remember { mutableIntStateOf(0) }
    var showResult by remember { mutableStateOf(false) }
    var lastCorrect by remember { mutableStateOf<Boolean?>(null) }
    var showEndDialog by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        viewModel.loadQuestions()
    }

    if (showEndDialog) {
        AlertDialog(
            onDismissRequest = {},
            title = { Text("Lesson done! 🎉", style = MaterialTheme.typography.titleMedium) },
            text = {
                Text(
                    "You earned $totalEarned ⭐ stars!",
                    style = MaterialTheme.typography.bodyLarge,
                    textAlign = TextAlign.Center
                )
            },
            confirmButton = {
                Button(
                    onClick = { viewModel.awardStars(totalEarned); onFinished() },
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = ButtonPrimary)
                ) {
                    Text("OK!", style = MaterialTheme.typography.labelLarge)
                }
            }
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
            Text(
                text = if (questions.isNotEmpty()) "Question ${currentIndex + 1} / ${questions.size}" else "",
                style = MaterialTheme.typography.bodyMedium
            )
            IconButton(onClick = onSettings) {
                Icon(Icons.Default.Settings, contentDescription = "Settings", tint = DeepPurple)
            }
        }

        when (uiState) {
            is UiState.Loading -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(color = ButtonPrimary, strokeWidth = 5.dp)
                        Spacer(Modifier.height(20.dp))
                        Text("Loading questions... 🤔", style = MaterialTheme.typography.bodyLarge)
                    }
                }
            }
            is UiState.Error -> {
                Box(Modifier.fillMaxSize().padding(28.dp), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("😕", style = MaterialTheme.typography.displayLarge)
                        Spacer(Modifier.height(16.dp))
                        Text(
                            (uiState as UiState.Error).message,
                            style = MaterialTheme.typography.bodyLarge,
                            textAlign = TextAlign.Center,
                            color = ErrorRed
                        )
                        Spacer(Modifier.height(24.dp))
                        Button(onClick = onSettings, shape = RoundedCornerShape(16.dp)) {
                            Text("Go to Settings", style = MaterialTheme.typography.labelLarge)
                        }
                    }
                }
            }
            else -> {
                if (questions.isNotEmpty() && currentIndex < questions.size) {
                    val q = questions[currentIndex]
                    QuestionCard(
                        question = q,
                        userAnswer = userAnswer,
                        onAnswerChange = { userAnswer = it },
                        showResult = showResult,
                        lastCorrect = lastCorrect,
                        totalEarned = totalEarned,
                        onSubmit = {
                            val correct = userAnswer.trim().toIntOrNull() == q.answer
                            lastCorrect = correct
                            if (correct) totalEarned += q.difficulty
                            showResult = true
                        },
                        onNext = {
                            showResult = false
                            lastCorrect = null
                            userAnswer = ""
                            if (currentIndex + 1 >= questions.size) {
                                showEndDialog = true
                            } else {
                                currentIndex++
                            }
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun QuestionCard(
    question: MathQuestion,
    userAnswer: String,
    onAnswerChange: (String) -> Unit,
    showResult: Boolean,
    lastCorrect: Boolean?,
    totalEarned: Int,
    onSubmit: () -> Unit,
    onNext: () -> Unit
) {
    val starsLabel = when (question.difficulty) { 1 -> "⭐"; 2 -> "⭐⭐"; else -> "⭐⭐⭐" }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(28.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(40.dp))
                .background(LemonYellow)
                .padding(horizontal = 20.dp, vertical = 8.dp)
        ) {
            Text("Worth $starsLabel", style = MaterialTheme.typography.bodyMedium, color = DeepPurple)
        }

        Spacer(Modifier.height(32.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(24.dp))
                .background(SoftPurple)
                .padding(36.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = question.question,
                style = MaterialTheme.typography.displayLarge,
                textAlign = TextAlign.Center
            )
        }

        Spacer(Modifier.height(32.dp))

        if (showResult) {
            val bg = if (lastCorrect == true) MintGreen else PeachOrange
            val emoji = if (lastCorrect == true) "✅ Correct!" else "❌ The answer was ${question.answer}"
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .background(bg)
                    .padding(20.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(emoji, style = MaterialTheme.typography.titleMedium, textAlign = TextAlign.Center)
            }
            Spacer(Modifier.height(24.dp))
            Button(
                onClick = onNext,
                modifier = Modifier.fillMaxWidth().height(64.dp),
                shape = RoundedCornerShape(20.dp),
                colors = ButtonDefaults.buttonColors(containerColor = ButtonPrimary)
            ) {
                Text("Next ➡️", style = MaterialTheme.typography.labelLarge)
            }
        } else {
            OutlinedTextField(
                value = userAnswer,
                onValueChange = onAnswerChange,
                label = { Text("Your answer", style = MaterialTheme.typography.bodyLarge) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                textStyle = MaterialTheme.typography.titleMedium
            )
            Spacer(Modifier.height(24.dp))
            Button(
                onClick = onSubmit,
                enabled = userAnswer.isNotBlank(),
                modifier = Modifier.fillMaxWidth().height(64.dp),
                shape = RoundedCornerShape(20.dp),
                colors = ButtonDefaults.buttonColors(containerColor = ButtonPrimary)
            ) {
                Text("Check Answer ✔️", style = MaterialTheme.typography.labelLarge)
            }
        }

        Spacer(Modifier.height(16.dp))
        Text("Earned so far: $totalEarned ⭐", style = MaterialTheme.typography.bodyMedium, color = MedText)
    }
}
