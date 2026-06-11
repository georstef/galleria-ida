package com.galleriaida.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.galleriaida.data.Quiz
import com.galleriaida.ui.UiStrings
import com.galleriaida.ui.theme.*
import com.galleriaida.viewmodel.AppViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt

private data class HistoryCard(
    val dateLabel: String,   // yyyy-MM-dd
    val timeLabel: String,   // HH:mm
    val quiz: Quiz,
    val duration: Int        // in minutes
)

@Composable
fun QuizHistoryScreen(
    viewModel: AppViewModel,
    onBack: () -> Unit,
    onQuizSelected: (Quiz) -> Unit
) {
    val quizHistory by viewModel.quizHistory.collectAsState()
    val uiStrings   by viewModel.uiStrings.collectAsState()

    // Load history when screen opens, clear when it closes
    LaunchedEffect(Unit) { viewModel.loadQuizHistory() }
    DisposableEffect(Unit) { onDispose { viewModel.clearQuizHistory() } }

    BackHandler { onBack() }

    val dateFormatter = remember { SimpleDateFormat("yyyy-MM-dd", Locale.US) }
    val timeFormatter = remember { SimpleDateFormat("HH:mm", Locale.US) }

    // Build display cards with duration, sorted by most recent first
    val cards: List<HistoryCard> = remember(quizHistory) {
        quizHistory
            .sortedByDescending { it.submittedAt }  // Most recent first
            .map { quiz ->
                val date = dateFormatter.format(Date(quiz.submittedAt))
                val time = timeFormatter.format(Date(quiz.submittedAt))
                val durationMs = quiz.submittedAt - quiz.startedAt
                val durationMinutes = (durationMs / (1000 * 60)).toInt()

                HistoryCard(
                    dateLabel = date,
                    timeLabel = time,
                    quiz = quiz,
                    duration = durationMinutes
                )
            }
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
            verticalAlignment     = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = DeepPurple)
            }
            Text(
                text  = uiStrings.historyTitle,
                style = MaterialTheme.typography.titleLarge,
                color = DeepPurple
            )
            // Balance spacer matching the icon button width
            Spacer(Modifier.size(48.dp))
        }

        if (cards.isEmpty()) {

            // ── Empty state ──────────────────────────────────────────────────
            Box(
                modifier         = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text      = uiStrings.historyEmpty,
                    style     = MaterialTheme.typography.bodyLarge,
                    color     = MedText,
                    textAlign = TextAlign.Center
                )
            }

        } else {

            // ── Column headers ───────────────────────────────────────────────
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 0.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(DeepPurple.copy(alpha = 0.12f))
                    .border(
                        width = 1.dp,
                        color = DeepPurple.copy(alpha = 0.2f),
                        shape = RoundedCornerShape(12.dp)
                    )
                    .padding(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = uiStrings.historyColumnDate,
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold,
                        color = DeepPurple,
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        text = uiStrings.historyColumnTime,
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold,
                        color = DeepPurple,
                        modifier = Modifier.weight(0.7f),
                        textAlign = TextAlign.Center
                    )
                    Text(
                        text = uiStrings.historyColumnDuration,
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold,
                        color = DeepPurple,
                        modifier = Modifier.weight(0.8f),
                        textAlign = TextAlign.Center
                    )
                    Text(
                        text = uiStrings.historyColumnScore,
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold,
                        color = DeepPurple,
                        modifier = Modifier.weight(0.9f),
                        textAlign = TextAlign.Center
                    )
                    Text(
                        text = uiStrings.historyColumnStars,
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold,
                        color = DeepPurple,
                        modifier = Modifier.weight(1f),
                        textAlign = TextAlign.End
                    )
                }
            }

            // ── Quiz cards ───────────────────────────────────────────────────
            LazyColumn(
                modifier       = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                items(cards) { card ->
                    QuizCard(
                        card = card,
                        onQuizSelected = onQuizSelected
                    )
                }
            }
        }
    }
}

// ── Quiz card ─────────────────────────────────────────────────────────────────

@Composable
private fun QuizCard(
    card: HistoryCard,
    onQuizSelected: (Quiz) -> Unit
) {
    val quiz = card.quiz

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(SoftPurple.copy(alpha = 0.2f))
            .border(
                width = 1.dp,
                color = SoftPurple.copy(alpha = 0.4f),
                shape = RoundedCornerShape(12.dp)
            )
            .clickable { onQuizSelected(quiz) }
            .padding(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Date
            Text(
                text = card.dateLabel,
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Medium,
                color = DeepPurple,
                modifier = Modifier.weight(1f)
            )

            // Time
            Text(
                text = card.timeLabel,
                style = MaterialTheme.typography.bodySmall,
                color = DeepPurple,
                modifier = Modifier.weight(0.7f),
                textAlign = TextAlign.Center
            )

            // Duration
            Text(
                text = "${card.duration}m",
                style = MaterialTheme.typography.bodySmall,
                color = DeepPurple,
                modifier = Modifier.weight(0.8f),
                textAlign = TextAlign.Center
            )

            // Score
            Text(
                text = "${quiz.correctAnswers}/${quiz.totalQuestions}",
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Medium,
                color = DeepPurple,
                modifier = Modifier.weight(0.9f),
                textAlign = TextAlign.Center
            )

            // Stars
            Text(
                text = "+${quiz.starsEarned}⭐",
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Bold,
                color = ButtonPrimary,
                modifier = Modifier.weight(1f),
                textAlign = TextAlign.End
            )
        }
    }
}