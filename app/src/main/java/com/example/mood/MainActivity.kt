@file:OptIn(kotlinx.serialization.InternalSerializationApi::class)
package com.example.mood



import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.example.mood.ui.theme.MoodTheme
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.Alignment

// For date formatting

import com.example.mood.model.LedgerEntry
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import com.example.mood.data.LedgerStore
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.ui.unit.sp
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.launch
import androidx.compose.foundation.combinedClickable
import androidx.compose.ui.text.font.FontWeight

import java.time.LocalDate
import java.time.format.DateTimeFormatter

import com.example.mood.ui.theme.LedgerTeal
import com.example.mood.ui.theme.LedgerGold
import com.example.mood.util.snapToQuarter
import com.example.mood.ui.DailyReflectionSheet
import com.example.mood.model.DailyReflection
import com.example.mood.util.todayKey
import androidx.compose.foundation.clickable
import androidx.compose.ui.text.style.TextAlign

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material.icons.filled.CalendarMonth
import com.example.mood.model.TagStats
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.background
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.Canvas
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.foundation.Canvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size



class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MoodTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    TodayScreen()
                }
            }
        }
    }
}



@Composable
fun DailyMoodSeal(
    mood: Float,
    tags: List<String>,
    tagStats: Map<String, TagStats>,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawMoodSeal(tags, tagStats)
        }


        // Center number (keep Compose Text – very important)
        Text(
            text = String.format("%.2f", mood),
            style = MaterialTheme.typography.displaySmall.copy(
                fontWeight = FontWeight.Medium
            ),
            color = moodValueColor(mood)
        )
    }
}

private fun DrawScope.drawMoodSeal(
    tags: List<String>,
    tagStats: Map<String, TagStats>
) {
    drawIntoCanvas { canvas ->
        val paint = android.graphics.Paint().apply {
            isAntiAlias = true
            textSize = 14.sp.toPx()
            alpha = 180
        }
    val fontMetrics = paint.fontMetrics
    val textOffset = -fontMetrics.ascent / 2f

    val radius = size.minDimension / 2.2f
    val strokeWidth = 3.dp.toPx()
    val textRadius = radius








    val textHeight = fontMetrics.descent - fontMetrics.ascent
    val bandThickness = textHeight + 10.dp.toPx()

// Move arc inward so it surrounds text
    val arcRadius = textRadius - bandThickness / 2f
    // Outer circle
    drawCircle(
        color = Color.White.copy(alpha = 0.18f),
        radius = radius,
        style = Stroke(width = strokeWidth)
    )

    if (tags.isEmpty()) return



        val path = android.graphics.Path().apply {
            addArc(
                center.x - radius,
                center.y - radius,
                center.x + radius,
                center.y + radius,
                -160f,
                320f
            )
        }

        var horizontalOffset = 0f




        var arcOffset = 0f
        val separator = " · "

        tags.forEachIndexed { index, tag ->
            val avg = tagStats[tag]?.average ?: 0f
            val color = tagImpactColor(avg)

            // Measure text width
            val textWidth = paint.measureText(tag)
            val separatorWidth = paint.measureText(separator)

            // Convert text width → sweep angle
            val sweep = textWidth / (2 * Math.PI.toFloat() * radius) * 360f

            // Draw colored arc segment (background)
            drawArc(
                color = Color(color).copy(alpha = 0.75f),
                startAngle = -160f + arcOffset,
                sweepAngle = sweep,
                useCenter = false,
                topLeft = Offset(
                    center.x - textRadius,
                    center.y - textRadius
                ),
                size = Size(
                    textRadius * 2,
                    textRadius * 2
                ),
                style = Stroke(
                    width = bandThickness,
                    cap = StrokeCap.Round
                )
            )

            // Draw text on top
            paint.color = android.graphics.Color.parseColor("#0F1A24")

            canvas.nativeCanvas.drawTextOnPath(
                tag,
                path,
                arcOffset * radius * Math.PI.toFloat() / 180f,
                textOffset,
                paint
            )

            arcOffset += sweep

            // Separator (no colored band)
            if (index < tags.lastIndex) {
                canvas.nativeCanvas.drawTextOnPath(
                    separator,
                    path,
                    arcOffset * radius * Math.PI.toFloat() / 180f,
                    textOffset,
                    paint
                )
                arcOffset += separatorWidth / (2 * Math.PI.toFloat() * radius) * 360f
            }
        }

    }

}

fun orderTagsByImpact(
    todaysTags: List<String>,
    tagStats: Map<String, TagStats>
): List<String> {
    return todaysTags
        .distinct()
        .map { tag ->
            tag to (tagStats[tag]?.average ?: 0f)
        }
        .sortedWith(
            compareByDescending<Pair<String, Float>> { it.second }
        )
        .map { it.first }
}

fun feelingEmoji(feeling: String): String =
    when (feeling.lowercase()) {
        "excited"     -> "🤩"
        "happy"       -> "🙂"
        "content"     -> "😌"
        "hopeful"     -> "🌱"

        "anxious"     -> "😰"
        "overwhelmed" -> "😵‍💫"
        "sad"         -> "😞"
        "angry"       -> "😠"
        "embarrassed" -> "😳"

        else -> "•"   // neutral fallback
    }

fun deltaColor(delta: Float): Color {
    val clamped = delta.coerceIn(-2f, 2f)
    val t = kotlin.math.abs(clamped) / 2f

    if (clamped == 0f) {
        return Color.White
    }

    val target = if (clamped > 0f) {
        Color(0xFF4CAF50) // green
    } else {
        Color(0xFFE57373) // red
    }

    return Color(
        red   = lerp(1f, target.red,   t),
        green = lerp(1f, target.green, t),
        blue  = lerp(1f, target.blue,  t),
        alpha = 1f
    )
}
fun tagImpactColor(avg: Float): Int {
    val clamped = avg.coerceIn(-2f, 2f)
    val t = kotlin.math.abs(clamped) / 2f

    if (clamped == 0f) {
        return android.graphics.Color.WHITE
    }

    val targetColor =
        if (clamped > 0f) {
            android.graphics.Color.rgb(76, 175, 80)   // green
        } else {
            android.graphics.Color.rgb(229, 115, 115) // red
        }

    val r = android.graphics.Color.red(targetColor)
    val g = android.graphics.Color.green(targetColor)
    val b = android.graphics.Color.blue(targetColor)

    return android.graphics.Color.argb(
        200,
        lerp(255f, r.toFloat(), t).toInt(),
        lerp(255f, g.toFloat(), t).toInt(),
        lerp(255f, b.toFloat(), t).toInt()
    )
}


private fun lerp(start: Float, end: Float, t: Float): Float =
    start + (end - start) * t

private val todayFormatter =
    DateTimeFormatter.ofPattern("EEEE, MMM dd")



fun deltaLabel(delta: Float): String =
    when {
        delta > 0f -> "▲ %.2f".format(delta)
        delta < 0f -> "▼ %.2f".format(kotlin.math.abs(delta))
        else -> "— 0.00"
    }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TodayScreen() {
    var entries by remember { mutableStateOf(listOf<LedgerEntry>()) }
    var showSheet by remember { mutableStateOf(false) }
    var entryBeingEdited by remember { mutableStateOf<LedgerEntry?>(null) }

    var reflectionText by remember { mutableStateOf("") }

    var activeDayKey by remember { mutableStateOf(todayKey()) }

    val baseline = 5.00f
    var reflectionScore by remember { mutableStateOf<Float?>(null) }
    val eventTotal = entries.fold(0f) { acc, e -> acc + e.delta }
    val computedScore = baseline + eventTotal
    val finalScore = reflectionScore ?: computedScore
    val drift = finalScore - computedScore
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var showReflectionSheet by remember { mutableStateOf(false) }
    var showCalendar by remember { mutableStateOf(false) }
    var daysWithData by remember { mutableStateOf(setOf<String>()) }

    val todaysTags = remember(entries) {
        entries
            .flatMap { it.tags }
            .distinct()
            .sorted()
    }
    var tagStats by remember { mutableStateOf<Map<String, TagStats>>(emptyMap()) }

    LaunchedEffect(activeDayKey) {
        tagStats = LedgerStore.loadTagStats(context)
    }





    LaunchedEffect(Unit) {
        daysWithData = LedgerStore.loadDaysWithEntries(context)
    }


    // Load entries for the day
    LaunchedEffect(activeDayKey) {
        entries = LedgerStore.loadEntriesForDay(context, activeDayKey)
    }

    val calendarSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    if (showCalendar) {
        ModalBottomSheet(
            onDismissRequest = { showCalendar = false },
            sheetState = calendarSheetState,
            containerColor = MaterialTheme.colorScheme.background
        ) {
            MonthCalendarView(
                activeDayKey = activeDayKey,
                daysWithData = daysWithData,
                onSelectDay = { selected ->
                    activeDayKey = selected
                    showCalendar = false
                },
                onDismiss = { showCalendar = false }
            )

            Spacer(Modifier.height(24.dp))
        }
    }


// Apply reflection AFTER entries are loaded & computed
    LaunchedEffect(entries, activeDayKey) {
        LedgerStore.loadDailyReflection(context, activeDayKey)?.let { reflection ->
            reflectionText = reflection.note
            reflectionScore =
                baseline +
                        entries.fold(0f) { acc, e -> acc + e.delta } +
                        reflection.drift
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {

// HEADER (title + menu)
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Emotional Ledger",
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontWeight = FontWeight.Medium,
                    letterSpacing = 0.sp,
                    fontSize = 24.sp
                ),
                color = Color.White,
                modifier = Modifier.weight(1f)
            )

            IconButton(onClick = { showCalendar = true }) {
                Icon(
                    imageVector = Icons.Default.CalendarMonth,
                    contentDescription = "Open calendar",
                    tint = Color.White
                )
            }


        }

        Text(
            text = LocalDate.parse(activeDayKey).format(todayFormatter),
            style = MaterialTheme.typography.bodyLarge.copy(fontSize = 14.sp),
            color = Color.White.copy(alpha = 0.65f)
        )


        // ENTRIES (boxed)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    color = Color.White.copy(alpha = 0.04f),
                    shape = RoundedCornerShape(16.dp)
                )
                .padding(horizontal = 12.dp, vertical = 8.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 420.dp) // ~6–7 entries before scroll
                .verticalScroll(rememberScrollState())
            ) {
                entries.forEachIndexed { index, entry ->
                    LedgerEntryRow(
                        entry = entry,
                        onEdit = {
                            entryBeingEdited = it
                            showSheet = true
                        },
                        onDelete = { toDelete ->
                            val updated = entries.filterNot { it.id == toDelete.id }
                            entries = updated

                            val dayKey = activeDayKey
                            coroutineScope.launch {
                                LedgerStore.saveEntriesForDay(context, dayKey, updated)
                                LedgerStore.removeEntryFromTagStats(context, toDelete)
                                tagStats = LedgerStore.loadTagStats(context)
                            }
                        }
                    )

                    if (index < entries.lastIndex) {
                        HorizontalDivider(
                            color = Color.White.copy(alpha = 0.06f),
                            modifier = Modifier.padding(vertical = 4.dp)
                        )
                    }
                }
            }
        }


        Spacer(modifier = Modifier.weight(1f))

        if (todaysTags.isNotEmpty()) {
            val orderedTags = orderTagsByImpact(todaysTags, tagStats)

            DailyMoodSeal(
                mood = finalScore,
                tags = orderedTags,
                tagStats = tagStats,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(240.dp)
            )

            Spacer(Modifier.height(12.dp))

            Text(
                text =
                    if (reflectionScore == null && reflectionText.isBlank())
                        "Current Mood"
                    else
                        "Final Mood",
                style = MaterialTheme.typography.bodySmall,
                color = Color.White.copy(alpha = 0.75f),
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )
        }


        // ADD BUTTON
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Button(
                onClick = { showReflectionSheet = true },
                modifier = Modifier
                    .weight(1f)
                    .height(56.dp),
                shape = RoundedCornerShape(18.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = LedgerGold,
                    contentColor = Color.Black
                )
            ) {
                Text(if (reflectionScore == null && reflectionText.isBlank()) "Daily Reflection" else "Edit Reflection")

            }

            Button(
                onClick = {
                    entryBeingEdited = null
                    showSheet = true

                },
                modifier = Modifier
                    .weight(1f)
                    .height(56.dp),
                shape = RoundedCornerShape(18.dp)
            ) {
                Text("+ Add Entry")
            }
        }

    }
    if (showReflectionSheet) {
        DailyReflectionSheet(
            currentComputedScore = computedScore,
            initialText = reflectionText,
            initialScore = reflectionScore,
            onSave = { text, score ->
                reflectionText = text
                reflectionScore = score

                val drift = score - computedScore
                val dayKey = activeDayKey


                coroutineScope.launch {
                    LedgerStore.saveDailyReflection(
                        context,
                        DailyReflection(
                            date = dayKey,
                            note = text,
                            drift = drift
                        )
                    )
                }

                showReflectionSheet = false
            },
            onDismiss = {
                showReflectionSheet = false
            }
        )
    }

    // BOTTOM SHEET
    if (showSheet) {
        AddEntrySheet(
            existingEntry = entryBeingEdited,
            onSave = { savedEntry ->

                val previous = entryBeingEdited   // ← important

                val updated =
                    entries.filterNot { it.id == savedEntry.id } + savedEntry

                entries = updated.sortedBy { it.timestamp }

                val dayKey = activeDayKey
                coroutineScope.launch {
                    LedgerStore.saveEntriesForDay(context, dayKey, entries)

                    LedgerStore.applyEntryToTagStats(
                        context = context,
                        entry = savedEntry,
                        previousEntry = previous
                    )

                    // ✅ FORCE refresh after stats are written
                    tagStats = LedgerStore.loadTagStats(context)
                }


                entryBeingEdited = null
                showSheet = false
            }
            ,
            onDismiss = {
                entryBeingEdited = null
                showSheet = false
            }
        )
    }
}

@Composable
private fun Metric(label: String, value: Float) {
    Column {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            color = Color.White.copy(alpha = 0.65f)
        )
        Text(
            text = "%.2f".format(value),
            style = MaterialTheme.typography.bodyMedium,
            color = Color.White
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun LedgerEntryRow(
    entry: LedgerEntry,
    onEdit: (LedgerEntry) -> Unit,
    onDelete: (LedgerEntry) -> Unit
) {
    var menuExpanded by remember { mutableStateOf(false) }
    val color = deltaColor(entry.delta)
    val impactColor = color

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = { onEdit(entry) },
                onLongClick = { menuExpanded = true }
            )
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {


    // Title
        Text(
            text = entry.title.ifBlank { "Untitled" },
            color = impactColor,
            style = MaterialTheme.typography.bodyLarge.copy(
                fontSize = 18.sp,          // up from 16
                fontWeight = FontWeight.Medium,
                letterSpacing = 0.3.sp
            ),
            modifier = Modifier.weight(1f)
        )


        // Category / Feeling
        Box(
            modifier = Modifier
                .width(36.dp),        // fixed column width
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = feelingEmoji(entry.feeling),
                fontSize = 20.sp
            )
        }


        // Delta
        Text(
            text = deltaLabel(entry.delta),
            color = color,
            style = MaterialTheme.typography.bodyMedium
        )
        DropdownMenu(
            expanded = menuExpanded,
            onDismissRequest = { menuExpanded = false }
        ) {
            DropdownMenuItem(
                text = { Text("Edit") },
                onClick = {
                    menuExpanded = false
                    onEdit(entry)
                }
            )
            DropdownMenuItem(
                text = { Text("Delete") },
                onClick = {
                    menuExpanded = false
                    onDelete(entry)
                }
            )
        }
    }
}
fun moodValueColor(value: Float): Color {
    return when {
        value > 0f -> Color(0xFF4CAF50)   // green
        value < 0f -> Color(0xFFE57373)   // red
        else -> Color.White
    }
}
@Composable
fun MonthCalendarView(
    activeDayKey: String,
    daysWithData: Set<String>,
    onSelectDay: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val today = LocalDate.now()

    var visibleMonth by remember {
        mutableStateOf(
            LocalDate.parse(activeDayKey).withDayOfMonth(1)
        )
    }

    val daysInMonth = visibleMonth.lengthOfMonth()
    val firstDayOffset = visibleMonth.dayOfWeek.value % 7

    val monthLabel =
        visibleMonth.format(DateTimeFormatter.ofPattern("MMMM yyyy"))

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(24.dp)
    ) {

        // Header
        // Header with month navigation
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {

            // ← Previous month
            IconButton(
                onClick = {
                    visibleMonth = visibleMonth.minusMonths(1)
                }
            ) {
                Text(
                    text = "←",
                    fontSize = 20.sp,
                    color = Color.White
                )
            }

            Text(
                text = monthLabel,
                style = MaterialTheme.typography.headlineSmall,
                color = Color.White
            )

            // → Next month (don’t allow future months)
            IconButton(
                onClick = {
                    val next = visibleMonth.plusMonths(1)
                    if (!next.isAfter(today.withDayOfMonth(1))) {
                        visibleMonth = next
                    }
                }
            ) {
                Text(
                    text = "→",
                    fontSize = 20.sp,
                    color = Color.White
                )
            }
        }


        Spacer(Modifier.height(12.dp))

        // Day labels
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            listOf("S","M","T","W","T","F","S").forEach {
                Text(it, color = Color.White.copy(alpha = 0.5f))
            }
        }

        Spacer(Modifier.height(8.dp))

        val totalCells = ((firstDayOffset + daysInMonth + 6) / 7) * 7

        Column {
            (0 until totalCells step 7).forEach { week ->
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    (0..6).forEach { dayIndex ->
                        val cellIndex = week + dayIndex
                        val dayNumber = cellIndex - firstDayOffset + 1

                        if (dayNumber in 1..daysInMonth) {
                            val date =
                                visibleMonth.withDayOfMonth(dayNumber).toString()

                            val hasData = daysWithData.contains(date)
                            val isFuture =
                                LocalDate.parse(date).isAfter(today)
                            val isToday = date == today.toString()

                            Text(
                                text = dayNumber.toString(),
                                modifier = Modifier
                                    .size(36.dp)
                                    .clickable(
                                        enabled = (hasData || isToday) && !isFuture
                                    ) {
                                        onSelectDay(date)
                                    }
                                ,
                                color = when {
                                    date == activeDayKey ->
                                        LedgerGold
                                    isToday ->
                                        LedgerGold.copy(alpha = 0.6f)
                                    hasData ->
                                        LedgerGold.copy(alpha = 0.9f)
                                    isFuture ->
                                        Color.White.copy(alpha = 0.25f)
                                    else ->
                                        Color.White.copy(alpha = 0.4f)
                                }
                                ,
                                textAlign = TextAlign.Center
                            )
                        } else {
                            Spacer(Modifier.size(36.dp))
                        }
                    }
                }
                Spacer(Modifier.height(6.dp))
            }
        }

        Spacer(Modifier.height(16.dp))

        TextButton(onClick = onDismiss) {
            Text("Close")
        }
    }
}




@Composable
fun Greeting(name: String, modifier: Modifier = Modifier) {
    Text(
        text = "Hello $name!",
        modifier = modifier
    )
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    MoodTheme {
        Greeting("Android")
    }
}