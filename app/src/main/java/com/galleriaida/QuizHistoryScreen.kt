package com.galleriaida.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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

// Two alternating soft background colors for day groups
private val DayColorA = Color(0xFFF3EFFE)   // soft lavender
private val DayColorB = Color(0xFFEAF6F6)   // soft mint

// Fixed column widths — guarantees alignment across all rows and the header
private val DateColumnWidth  = 90.dp
private val TimeColumnWidth  = 52.dp
private val StarsColumnWidth = 64.dp

private data class HistoryRow(
    val dateLabel: String,   // yyyy-MM-dd on first row of the day, "" on subsequent rows
    val timeLabel: String,   // HH:mm
    val quiz: Quiz,
    val rowColor: Color
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
    val timeFormatter = remember { SimpleDateFormat("HH:mm",      Locale.US) }

    // Build display rows: group by date, assign alternating color per day group,
    // show date label only on the first row of each group.
    val rows: List<HistoryRow> = remember(quizHistory) {
        var lastDate     = ""
        var colorIndex   = 0
        var currentColor = DayColorA

        quizHistory.map { quiz ->
            val date = dateFormatter.format(Date(quiz.submittedAt))
            val time = timeFormatter.format(Date(quiz.submittedAt))

            val isFirstOfDay = date != lastDate
            if (isFirstOfDay) {
                if (lastDate.isNotEmpty()) colorIndex++
                currentColor = if (colorIndex % 2 == 0) DayColorA else DayColorB
                lastDate = date
            }

            HistoryRow(
                dateLabel = if (isFirstOfDay) date else "",
                timeLabel = time,
                quiz      = quiz,
                rowColor  = currentColor
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

        if (rows.isEmpty()) {

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
            HistoryHeaderRow(uiStrings = uiStrings)

            HorizontalDivider(
                color     = DeepPurple.copy(alpha = 0.15f),
                thickness = 1.dp,
                modifier  = Modifier.padding(horizontal = 16.dp)
            )

            // ── Quiz rows ────────────────────────────────────────────────────
            LazyColumn(
                modifier       = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 16.dp)
            ) {
                items(rows) { row ->
                    HistoryDataRow(
                        row            = row,
                        onQuizSelected = onQuizSelected
                    )
                    HorizontalDivider(
                        color     = row.rowColor.copy(alpha = 0.5f),
                        thickness = 0.5.dp,
                        modifier  = Modifier.padding(horizontal = 16.dp)
                    )
                }
            }
        }
    }
}

// ── Header row ────────────────────────────────────────────────────────────────

@Composable
private fun HistoryHeaderRow(uiStrings: UiStrings) {
    Row(
        modifier          = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text       = uiStrings.historyColumnDate,
            modifier   = Modifier.width(DateColumnWidth),
            style      = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Bold,
            color      = DeepPurple
        )
        Text(
            text       = uiStrings.historyColumnTime,
            modifier   = Modifier.width(TimeColumnWidth),
            style      = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Bold,
            color      = DeepPurple
        )
        Text(
            text       = uiStrings.historyColumnScore,
            modifier   = Modifier.weight(1f),
            style      = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Bold,
            color      = DeepPurple
        )
        Text(
            text       = uiStrings.historyColumnStars,
            modifier   = Modifier.width(StarsColumnWidth),
            style      = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Bold,
            color      = DeepPurple,
            textAlign  = TextAlign.End
        )
    }
}

// ── Single data row ───────────────────────────────────────────────────────────

@Composable
private fun HistoryDataRow(
    row: HistoryRow,
    onQuizSelected: (Quiz) -> Unit
) {
    Row(
        modifier          = Modifier
            .fillMaxWidth()
            .background(row.rowColor)
            .clickable { onQuizSelected(row.quiz) }
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Date — only shown on the first row of each day group
        Text(
            text       = row.dateLabel,
            modifier   = Modifier.width(DateColumnWidth),
            style      = MaterialTheme.typography.bodySmall,
            color      = if (row.dateLabel.isNotEmpty()) DeepPurple else Color.Transparent,
            fontWeight = FontWeight.SemiBold,
            fontSize   = 11.sp
        )
        // Time
        Text(
            text     = row.timeLabel,
            modifier = Modifier.width(TimeColumnWidth),
            style    = MaterialTheme.typography.bodySmall,
            color    = MaterialTheme.colorScheme.onSurface
        )
        // Score
        Text(
            text     = "${row.quiz.correctAnswers} / ${row.quiz.totalQuestions}",
            modifier = Modifier.weight(1f),
            style    = MaterialTheme.typography.bodySmall,
            color    = MaterialTheme.colorScheme.onSurface
        )
        // Stars — right aligned
        Text(
            text       = "+${row.quiz.starsEarned} ⭐",
            modifier   = Modifier.width(StarsColumnWidth),
            style      = MaterialTheme.typography.bodySmall,
            color      = DeepPurple,
            fontWeight = FontWeight.Bold,
            textAlign  = TextAlign.End
        )
    }
}
