package com.galleriaida.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.galleriaida.data.QuizQuestion
import com.galleriaida.ui.theme.*
import com.galleriaida.viewmodel.AppViewModel
import com.galleriaida.viewmodel.UiState

@Composable
fun GameScreen(
    viewModel: AppViewModel,
    onAbandoned: () -> Unit,
    onSubmitted: () -> Unit,
    onEditProfile: () -> Unit
) {
    val uiState   by viewModel.uiState.collectAsState()
    val questions by viewModel.questions.collectAsState()
    val uiStrings by viewModel.uiStrings.collectAsState()
    val player    by viewModel.currentPlayer.collectAsState()
    val initial   = player?.name?.firstOrNull()?.uppercase() ?: "?"

    // Map of questionId → player's current answer
    val playerAnswers = remember { mutableStateMapOf<String, String>() }

    // Current page index
    var currentIndex by remember { mutableIntStateOf(0) }

    // Direction of slide animation: 1 = forward, -1 = backward
    var slideDirection by remember { mutableIntStateOf(1) }

    // Abandon confirmation dialog
    var showAbandonDialog by remember { mutableStateOf(false) }

    // Unanswered questions dialog
    var showUnansweredDialog by remember { mutableStateOf(false) }
    var unansweredIndices by remember { mutableStateOf<List<Int>>(emptyList()) }

    // Load questions when the screen first appears
    LaunchedEffect(Unit) {
        viewModel.loadQuestions()
    }

    // Intercept Android back gesture — treat same as back button
    BackHandler {
        showAbandonDialog = true
    }

    // ── Abandon dialog ───────────────────────────────────────────────────────
    if (showAbandonDialog) {
        AlertDialog(
            onDismissRequest = { showAbandonDialog = false },
            title = {
                Text(
                    uiStrings.gameAbandonTitle,
                    style = MaterialTheme.typography.titleMedium
                )
            },
            text = {
                Text(
                    uiStrings.gameAbandonMessage,
                    style = MaterialTheme.typography.bodyLarge
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showAbandonDialog = false
                        viewModel.discardQuiz()
                        onAbandoned()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = ErrorRed),
                    shape  = RoundedCornerShape(16.dp)
                ) {
                    Text(uiStrings.gameAbandonConfirm, style = MaterialTheme.typography.labelLarge)
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = { showAbandonDialog = false },
                    shape   = RoundedCornerShape(16.dp)
                ) {
                    Text(uiStrings.gameAbandonCancel, style = MaterialTheme.typography.labelLarge)
                }
            }
        )
    }

    // ── Unanswered questions dialog ──────────────────────────────────────────
    if (showUnansweredDialog) {
        AlertDialog(
            onDismissRequest = { showUnansweredDialog = false },
            title = {
                Text(
                    uiStrings.gameUnansweredTitle,
                    style = MaterialTheme.typography.titleMedium
                )
            },
            text = {
                Column {
                    Text(
                        uiStrings.gameUnansweredMessage,
                        style = MaterialTheme.typography.bodyLarge
                    )
                    Spacer(Modifier.height(12.dp))
                    // Tappable question numbers
                    unansweredIndices.forEach { idx ->
                        TextButton(
                            onClick = {
                                showUnansweredDialog = false
                                slideDirection = if (idx > currentIndex) 1 else -1
                                currentIndex   = idx
                            }
                        ) {
                            Text(
                                uiStrings.gameUnansweredQuestion.format(idx + 1),
                                style = MaterialTheme.typography.bodyLarge,
                                color = ButtonPrimary
                            )
                        }
                    }
                }
            },
            confirmButton = {
                OutlinedButton(
                    onClick = { showUnansweredDialog = false },
                    shape   = RoundedCornerShape(16.dp)
                ) {
                    Text(uiStrings.gameUnansweredClose, style = MaterialTheme.typography.labelLarge)
                }
            }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {

        // ── Top bar ──────────────────────────────────────────────────────────
        Row(
            modifier              = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment     = Alignment.CenterVertically
        ) {
            // Back / abandon button
            IconButton(onClick = { showAbandonDialog = true }) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Exit quiz", tint = DeepPurple)
            }

            // Question counter
            if (questions.isNotEmpty()) {
                Text(
                    text  = uiStrings.gameQuestionCounter.format(currentIndex + 1, questions.size),
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            // Player avatar → edit profile
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

        // ── Body ─────────────────────────────────────────────────────────────
        when (uiState) {

            is UiState.Loading -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(color = ButtonPrimary, strokeWidth = 5.dp)
                        Spacer(Modifier.height(20.dp))
                        Text(uiStrings.gameLoadingQuestions, style = MaterialTheme.typography.bodyLarge)
                    }
                }
            }

            is UiState.Error -> {
                Box(
                    Modifier
                        .fillMaxSize()
                        .padding(28.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("😕", style = MaterialTheme.typography.displayLarge)
                        Spacer(Modifier.height(16.dp))
                        Text(
                            (uiState as UiState.Error).message,
                            style     = MaterialTheme.typography.bodyLarge,
                            textAlign = TextAlign.Center,
                            color     = ErrorRed
                        )
                        Spacer(Modifier.height(24.dp))
                        // Primary — try loading questions again
                        Button(
                            onClick  = { viewModel.loadQuestions() },
                            shape    = RoundedCornerShape(16.dp),
                            colors   = ButtonDefaults.buttonColors(containerColor = ButtonPrimary),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(uiStrings.gameTryAgain, style = MaterialTheme.typography.labelLarge)
                        }
                    }
                }
            }

            else -> {
                if (questions.isNotEmpty() && currentIndex < questions.size) {
                    val isLast = currentIndex == questions.size - 1

                    // ── Animated question page ────────────────────────────────
                    AnimatedContent(
                        targetState = currentIndex,
                        transitionSpec = {
                            slideInHorizontally { it * slideDirection } togetherWith
                                    slideOutHorizontally { -it * slideDirection }
                        },
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        label    = "QuestionPage"
                    ) { pageIndex ->
                        QuestionPage(
                            question      = questions[pageIndex],
                            questionNumber = pageIndex + 1,
                            totalQuestions = questions.size,
                            playerAnswer  = playerAnswers[questions[pageIndex].id] ?: "",
                            onAnswerChange = { answer ->
                                playerAnswers[questions[pageIndex].id] = answer
                            },
                            uiStrings     = uiStrings
                        )
                    }

                    // ── Navigation row ────────────────────────────────────────
                    Row(
                        modifier              = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp, vertical = 16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment     = Alignment.CenterVertically
                    ) {
                        // Previous — hidden on first question
                        if (currentIndex > 0) {
                            OutlinedButton(
                                onClick = {
                                    slideDirection = -1
                                    currentIndex--
                                },
                                shape  = RoundedCornerShape(16.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Default.ArrowBack, contentDescription = null)
                                Spacer(Modifier.width(6.dp))
                                Text(uiStrings.gamePrevious, style = MaterialTheme.typography.labelLarge)
                            }
                            Spacer(Modifier.width(12.dp))
                        }

                        // Next / Submit
                        Button(
                            onClick = {
                                if (isLast) {
                                    // Check for unanswered questions
                                    val unanswered = questions.indices.filter { idx ->
                                        playerAnswers[questions[idx].id].isNullOrBlank()
                                    }
                                    if (unanswered.isNotEmpty()) {
                                        unansweredIndices    = unanswered
                                        showUnansweredDialog = true
                                    } else {
                                        viewModel.submitQuiz(playerAnswers.toMap())
                                        onSubmitted()
                                    }
                                } else {
                                    slideDirection = 1
                                    currentIndex++
                                }
                            },
                            shape    = RoundedCornerShape(16.dp),
                            colors   = ButtonDefaults.buttonColors(containerColor = ButtonPrimary),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(
                                if (isLast) uiStrings.gameSubmit else uiStrings.gameNext,
                                style = MaterialTheme.typography.labelLarge
                            )
                            if (!isLast) {
                                Spacer(Modifier.width(6.dp))
                                Icon(Icons.Default.ArrowForward, contentDescription = null)
                            }
                        }
                    }
                }
            }
        }
    }
}

// ── Question page ─────────────────────────────────────────────────────────────

@Composable
fun QuestionPage(
    question: QuizQuestion,
    questionNumber: Int,
    totalQuestions: Int,
    playerAnswer: String,
    onAnswerChange: (String) -> Unit,
    uiStrings: com.galleriaida.ui.UiStrings
) {
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(horizontal = 24.dp, vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Subject + level badge row
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment     = Alignment.CenterVertically
        ) {
            // Subject chip
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(40.dp))
                    .background(SoftPurple)
                    .padding(horizontal = 14.dp, vertical = 6.dp)
            ) {
                Text(question.subject, style = MaterialTheme.typography.bodySmall, color = DeepPurple)
            }
            // Level → stars
            val starsLabel = when (question.level) { 1 -> "⭐"; 2 -> "⭐⭐"; else -> "⭐⭐⭐" }
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(40.dp))
                    .background(LemonYellow)
                    .padding(horizontal = 14.dp, vertical = 6.dp)
            ) {
                Text(
                    uiStrings.gameWorth.format(starsLabel),
                    style = MaterialTheme.typography.bodySmall,
                    color = DeepPurple
                )
            }
        }

        Spacer(Modifier.height(20.dp))

        // Instruction
        if (question.instruction.isNotBlank()) {
            Text(
                text      = question.instruction,
                style     = MaterialTheme.typography.bodyMedium,
                color     = MedText,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(12.dp))
        }

        // Question card
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(24.dp))
                .background(SoftPurple)
                .padding(28.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text      = question.question,
                style     = MaterialTheme.typography.titleMedium,
                textAlign = TextAlign.Center
            )
        }

        Spacer(Modifier.height(24.dp))

        // Answer input — varies by question type
        when (question.type) {

            "multiple_choice" -> {
                val options = question.options ?: emptyList()
                options.forEach { option ->
                    val selected = playerAnswer == option
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(if (selected) ButtonPrimary else MaterialTheme.colorScheme.surface)
                            .selectable(
                                selected = selected,
                                onClick  = { onAnswerChange(option) }
                            )
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = selected,
                            onClick  = null,
                            colors   = RadioButtonDefaults.colors(
                                selectedColor   = MaterialTheme.colorScheme.onPrimary,
                                unselectedColor = DeepPurple
                            )
                        )
                        Spacer(Modifier.width(12.dp))
                        Text(
                            text  = option,
                            style = MaterialTheme.typography.bodyLarge,
                            color = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }

            "true_false" -> {
                val options = listOf(uiStrings.gameTrue, uiStrings.gameFalse)
                Row(
                    modifier              = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    options.forEach { option ->
                        val selected = playerAnswer == option
                        Button(
                            onClick  = { onAnswerChange(option) },
                            modifier = Modifier.weight(1f).height(56.dp),
                            shape    = RoundedCornerShape(16.dp),
                            colors   = ButtonDefaults.buttonColors(
                                containerColor = if (selected) ButtonPrimary else SoftPurple,
                                contentColor   = if (selected) MaterialTheme.colorScheme.onPrimary else DeepPurple
                            )
                        ) {
                            Text(option, style = MaterialTheme.typography.labelLarge)
                        }
                    }
                }
            }

            else -> {
                // "text" — free text input
                OutlinedTextField(
                    value         = playerAnswer,
                    onValueChange = onAnswerChange,
                    label         = { Text(uiStrings.gameYourAnswer, style = MaterialTheme.typography.bodyLarge) },
                    singleLine    = true,
                    modifier      = Modifier.fillMaxWidth(),
                    shape         = RoundedCornerShape(16.dp),
                    textStyle     = MaterialTheme.typography.titleMedium
                )
            }
        }

        Spacer(Modifier.height(24.dp))
    }
}