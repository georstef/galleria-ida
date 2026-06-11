package com.galleriaida.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import com.galleriaida.data.Quiz
import com.galleriaida.data.QuizAnswer
import com.galleriaida.ui.theme.*
import com.galleriaida.viewmodel.AppViewModel

@Composable
fun QuizSummaryScreen(
    viewModel: AppViewModel,
    onClose: () -> Unit
) {
    val quiz      by viewModel.lastCompletedQuiz.collectAsState()
    val uiStrings by viewModel.uiStrings.collectAsState()

    // Both the close button and the Android back gesture go to PlayerHome
    BackHandler { onClose() }

    // Should never be null when this screen is shown, but guard defensively
    val completedQuiz = quiz ?: return

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {

        // ── Score header ─────────────────────────────────────────────────────
        Column(
            modifier            = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(bottomStart = 32.dp, bottomEnd = 32.dp))
                .background(SoftPurple)
                .padding(horizontal = 28.dp, vertical = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text  = uiStrings.summaryTitle,
                style = MaterialTheme.typography.displayLarge,
                color = DeepPurple
            )

            Spacer(Modifier.height(16.dp))

            // Stars earned
            Row(
                modifier          = Modifier
                    .clip(RoundedCornerShape(40.dp))
                    .background(LemonYellow)
                    .padding(horizontal = 20.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("⭐", style = MaterialTheme.typography.titleLarge)
                Spacer(Modifier.width(8.dp))
                Text(
                    text  = uiStrings.summaryStarsEarned.format(completedQuiz.starsEarned),
                    style = MaterialTheme.typography.titleLarge,
                    color = DeepPurple
                )
            }

            Spacer(Modifier.height(12.dp))

            // Correct / total
            Text(
                text  = uiStrings.summaryScore.format(
                    completedQuiz.correctAnswers,
                    completedQuiz.totalQuestions
                ),
                style = MaterialTheme.typography.bodyLarge,
                color = DeepPurple
            )
        }

        // ── Answer list ──────────────────────────────────────────────────────
        LazyColumn(
            modifier            = Modifier
                .weight(1f)
                .fillMaxWidth(),
            contentPadding      = PaddingValues(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            itemsIndexed(completedQuiz.answers) { index, answer ->
                QuizAnswerCard(
                    index     = index,
                    answer    = answer,
                    uiStrings = uiStrings
                )
            }
        }

        // ── Close button ─────────────────────────────────────────────────────
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 16.dp)
        ) {
            Button(
                onClick  = {
                    viewModel.clearLastCompletedQuiz()
                    onClose()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(64.dp),
                shape    = RoundedCornerShape(20.dp),
                colors   = ButtonDefaults.buttonColors(containerColor = ButtonPrimary)
            ) {
                Text(uiStrings.summaryClose, style = MaterialTheme.typography.labelLarge)
            }
        }
    }
}

// ── Single answer card ────────────────────────────────────────────────────────

@Composable
fun QuizAnswerCard(
    index: Int,
    answer: QuizAnswer,
    uiStrings: com.galleriaida.ui.UiStrings
) {
    val cardBackground = if (answer.wasCorrect) MintGreen.copy(alpha = 0.3f) else PeachOrange.copy(alpha = 0.3f)
    val accentColor    = if (answer.wasCorrect) MintGreen else PeachOrange

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(cardBackground)
            .padding(16.dp)
    ) {
        // Question number + subject + level
        Row(
            modifier              = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment     = Alignment.CenterVertically
        ) {
            // Question number and subject
            Text(
                text  = uiStrings.summaryQuestionNumber.format(index + 1, answer.subject),
                style = MaterialTheme.typography.bodySmall,
                color = MedText
            )

            // Level stars + result indicator
            Row(verticalAlignment = Alignment.CenterVertically) {
                val starsLabel = when (answer.level) { 1 -> "⭐"; 2 -> "⭐⭐"; else -> "⭐⭐⭐" }
                Text(starsLabel, style = MaterialTheme.typography.bodySmall)
                Spacer(Modifier.width(6.dp))
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(40.dp))
                        .background(accentColor)
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text(
                        text  = if (answer.wasCorrect) uiStrings.summaryCorrect else uiStrings.summaryWrong,
                        style = MaterialTheme.typography.bodySmall,
                        color = DeepPurple,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        Spacer(Modifier.height(8.dp))

        // Instruction (if present)
        if (answer.instruction.isNotBlank()) {
            Text(
                text  = answer.instruction,
                style = MaterialTheme.typography.bodySmall,
                color = MedText
            )
            Spacer(Modifier.height(4.dp))
        }

        // Question text
        Text(
            text  = answer.question,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface
        )

        Spacer(Modifier.height(10.dp))

        HorizontalDivider(color = accentColor.copy(alpha = 0.5f), thickness = 1.dp)

        Spacer(Modifier.height(10.dp))

        // Player's answer
        if (answer.wasCorrect) {
            // Correct — show normally in one line
            Text(
                text = buildAnnotatedString {
                    withStyle(SpanStyle(color = MedText)) {
                        append(uiStrings.summaryYourAnswer + " ")
                    }
                    withStyle(SpanStyle(
                        color      = DeepPurple,
                        fontWeight = FontWeight.Bold
                    )) {
                        append(answer.playerAnswer)
                    }
                },
                style = MaterialTheme.typography.bodyMedium
            )
        } else {
            // Wrong — player's answer in red with strikethrough, then correct answer below
            Text(
                text = buildAnnotatedString {
                    withStyle(SpanStyle(color = MedText)) {
                        append(uiStrings.summaryYourAnswer + " ")
                    }
                    withStyle(SpanStyle(
                        color           = ErrorRed,
                        fontWeight      = FontWeight.Bold,
                        textDecoration  = TextDecoration.LineThrough
                    )) {
                        append(answer.playerAnswer.ifBlank { uiStrings.summaryNoAnswer })
                    }
                },
                style = MaterialTheme.typography.bodyMedium
            )

            Spacer(Modifier.height(4.dp))

            Text(
                text = buildAnnotatedString {
                    withStyle(SpanStyle(color = MedText)) {
                        append(uiStrings.summaryCorrectAnswer + " ")
                    }
                    withStyle(SpanStyle(
                        color      = DeepPurple,
                        fontWeight = FontWeight.Bold
                    )) {
                        append(answer.correctAnswer)
                    }
                },
                style = MaterialTheme.typography.bodyMedium
            )
        }

        // For multiple_choice — show all options for reference
        if (answer.type == "multiple_choice" && !answer.options.isNullOrEmpty()) {
            Spacer(Modifier.height(10.dp))
            HorizontalDivider(color = accentColor.copy(alpha = 0.3f), thickness = 1.dp)
            Spacer(Modifier.height(8.dp))
            Text(
                text  = uiStrings.summaryOptions,
                style = MaterialTheme.typography.bodySmall,
                color = MedText
            )
            Spacer(Modifier.height(4.dp))
            answer.options.forEach { option ->
                val isCorrect = option == answer.correctAnswer
                val isChosen  = option == answer.playerAnswer
                Text(
                    text = buildAnnotatedString {
                        val bullet = if (isCorrect) "✓ " else "• "
                        withStyle(SpanStyle(
                            color      = when {
                                isCorrect -> DeepPurple
                                isChosen  -> ErrorRed
                                else      -> MedText
                            },
                            fontWeight      = if (isCorrect || isChosen) FontWeight.Bold else FontWeight.Normal,
                            textDecoration  = if (isChosen && !isCorrect) TextDecoration.LineThrough else TextDecoration.None
                        )) {
                            append(bullet + option)
                        }
                    },
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}
