package com.example.mood


import com.example.mood.model.DailyMoodPoint
import java.time.format.DateTimeFormatter


// ─────────────────────────────────────────────
// Compose runtime
// ─────────────────────────────────────────────
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue

// ─────────────────────────────────────────────
// Compose UI / layout
// ─────────────────────────────────────────────
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.unit.dp

// ─────────────────────────────────────────────
// Icons
// ─────────────────────────────────────────────
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack

// ─────────────────────────────────────────────
// Android / Context
// ─────────────────────────────────────────────
import androidx.compose.ui.platform.LocalContext

// ─────────────────────────────────────────────
// Date / Time
// ─────────────────────────────────────────────
import java.time.LocalDate
import java.time.DayOfWeek

// ─────────────────────────────────────────────
// Your project helpers
// ─────────────────────────────────────────────
import com.example.mood.data.loadWeeklyMood
import androidx.compose.ui.unit.sp
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas


private val dayFormatter =
    DateTimeFormatter.ofPattern("EEE")   // Sun

private val dateFormatter =
    DateTimeFormatter.ofPattern("M/d")   // 12/21



@Composable
fun WeeklyMoodBarChart(
    data: List<DailyMoodPoint>,
    baseline: Float = 5.0f,
    modifier: Modifier = Modifier
) {
    val max = 10f
    val min = 0f

    Canvas(modifier = modifier) {

        val barWidth = size.width / (data.size * 1.6f)
        val spacing = barWidth * 0.6f

        val baselineY =
            size.height * (1f - (baseline - min) / (max - min))

        // Baseline reference
        drawLine(
            color = Color.White.copy(alpha = 0.25f),
            start = Offset(0f, baselineY),
            end = Offset(size.width, baselineY),
            strokeWidth = 2.dp.toPx()
        )

        data.forEachIndexed { index, point ->

            val x =
                spacing + index * (barWidth + spacing)

            val barTop =
                size.height * (1f - (point.score - min) / (max - min))

            val barBottom = size.height

            val barCenterX = x + barWidth / 2f

            val barColor =
                if (point.score >= baseline)
                    Color(0xFF66BB6A)
                else
                    Color(0xFFE57373)

            // ── BAR ─────────────────────────────
            drawRoundRect(
                color = barColor,
                topLeft = Offset(x, barTop),
                size = Size(
                    width = barWidth,
                    height = barBottom - barTop
                ),
                cornerRadius = CornerRadius(
                    6.dp.toPx(),
                    6.dp.toPx()
                )
            )

            // ── SCORE ABOVE BAR ─────────────────
            drawIntoCanvas { canvas ->
                val scorePaint = android.graphics.Paint().apply {
                    isAntiAlias = true
                    textAlign = android.graphics.Paint.Align.CENTER
                    textSize = 12.sp.toPx()
                    color = android.graphics.Color.WHITE
                    alpha = 220
                }

                canvas.nativeCanvas.drawText(
                    String.format("%.2f", point.score),
                    barCenterX,
                    barTop - 8.dp.toPx(),
                    scorePaint
                )
            }


            // ── DAY + DATE BELOW BAR ────────────
            drawIntoCanvas { canvas ->
                val labelPaint = android.graphics.Paint().apply {
                    isAntiAlias = true
                    textAlign = android.graphics.Paint.Align.CENTER
                    textSize = 11.sp.toPx()
                    color = android.graphics.Color.WHITE
                    alpha = 160
                }

                val dayY = barBottom + 14.dp.toPx()
                val dateY = dayY + 12.dp.toPx()

                canvas.nativeCanvas.drawText(
                    point.date.format(dayFormatter),
                    barCenterX,
                    dayY,
                    labelPaint
                )

                canvas.nativeCanvas.drawText(
                    point.date.format(dateFormatter),
                    barCenterX,
                    dateY,
                    labelPaint
                )
            }

        }

    }
}
@Composable
fun MoodTrendsScreen(
    initialWeekStart: LocalDate,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val baseline = 5.0f

    var weekStart by remember(initialWeekStart) {
        mutableStateOf(initialWeekStart)
    }



    var data by remember {
        mutableStateOf<List<DailyMoodPoint>>(emptyList())
    }

    LaunchedEffect(weekStart) {
        data = loadWeeklyMood(context, weekStart, baseline)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {

        // Header
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.Default.ArrowBack, null)
            }

            Spacer(Modifier.width(8.dp))

            Text(
                text = "Mood Trends",
                style = MaterialTheme.typography.headlineMedium
            )
        }

        // Week selector
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextButton(
                onClick = { weekStart = weekStart.minusWeeks(1) }
            ) { Text("← Previous") }

            Text(
                text = "${weekStart} – ${weekStart.plusDays(6)}",
                color = Color.White.copy(alpha = 0.65f)
            )

            TextButton(
                onClick = { weekStart = weekStart.plusWeeks(1) }
            ) { Text("Next →") }
        }

        WeeklyMoodBarChart(
            data = data,
            baseline = baseline,
            modifier = Modifier
                .fillMaxWidth()
                .height(300.dp)
        )

    }
}
