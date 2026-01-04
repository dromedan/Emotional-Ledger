@file:Suppress("NewApi")
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


// ─────────────────────────────────────────────
// Your project helpers
// ─────────────────────────────────────────────

import androidx.compose.ui.unit.sp
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.Brush
import com.example.mood.ui.theme.LedgerGold

import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.graphics.toArgb

import com.example.mood.model.WeeklyReflection
import com.example.mood.ui.WeeklyReflectionSheet
import androidx.compose.runtime.rememberCoroutineScope
import com.example.mood.data.LedgerStore
import kotlinx.coroutines.launch
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.RoundedCornerShape
import com.example.mood.model.MonthlyReflection

import androidx.compose.material.icons.filled.Download
import com.example.mood.export.exportWeeklyReflectionToObsidian
import com.example.mood.export.exportMonthlyReflectionToObsidian
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.filled.Edit

import android.net.Uri



private val dayFormatter =
    DateTimeFormatter.ofPattern("EEE")   // Sun

private val dateFormatter =
    DateTimeFormatter.ofPattern("M/d")   // 12/21

private val monthFormatter =
    DateTimeFormatter.ofPattern("MMM")   // Jan



enum class TrendsRange {
    WEEK, MONTH, YEAR
}





@Composable
fun WeeklyMoodBarChart(
    data: List<DailyMoodPoint>,
    baseline: Float = 5.0f,
    modifier: Modifier = Modifier,
    range: TrendsRange,
    onDaySelected: (LocalDate) -> Unit
)
 {

    val max = 10f
    val min = 0f
    val today = LocalDate.now()

     val visibleData =
         data.filter { !it.date.isAfter(today) }

     val slotCount =
         when (range) {
             TrendsRange.YEAR -> 12
             TrendsRange.MONTH -> 5  // max weeks shown
             TrendsRange.WEEK -> 7
         }



     Canvas(
         modifier = modifier.pointerInput(data) {
             detectTapGestures { offset ->

                 val barWidth = size.width / (slotCount * 1.6f)


                 val spacing = barWidth * 0.6f

                 val index =
                     ((offset.x - spacing) / (barWidth + spacing))
                         .toInt()

                 if (index in visibleData.indices) {
                     val point = visibleData[index]

                     if (!point.date.isAfter(LocalDate.now())) {
                         onDaySelected(point.date)
                     }
                 }
             }
         }
     ) {


         val barWidth = size.width / (slotCount * 1.6f)
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

        val today = LocalDate.now()

        visibleData.forEachIndexed { index, point ->


            val x =
                spacing + index * (barWidth + spacing)

            val barTop =
                size.height * (1f - (point.score - min) / (max - min))

            val barBottom = size.height

            val barCenterX = x + barWidth / 2f

            val isToday = point.date == today

            val isCurrentBar =
                when (range) {
                    TrendsRange.WEEK ->
                        isToday

                    TrendsRange.MONTH ->
                        today in point.date..point.date.plusDays(6)

                    TrendsRange.YEAR ->
                        point.date.month == today.month &&
                                point.date.year == today.year
                }


            val baseColor =
                if (point.score >= baseline)
                    Color(0xFF66BB6A)
                else
                    Color(0xFFE57373)



            val barBrush =
                Brush.verticalGradient(
                    colors = listOf(
                        baseColor.copy(alpha = 0.95f), // darker bottom
                        baseColor.copy(alpha = 0.55f)  // lighter top
                    )
                )

            // ── BAR ─────────────────────────────
            drawRoundRect(
                brush = barBrush,
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
                    color =
                        if (isCurrentBar)
                            LedgerGold.toArgb()
                        else
                            android.graphics.Color.WHITE

                    alpha = if (isCurrentBar) 220 else 160


                }

                val dayY = barBottom + 14.dp.toPx()
                val dateY = dayY + 12.dp.toPx()

                when (range) {
                    TrendsRange.YEAR -> {
                        canvas.nativeCanvas.drawText(
                            point.date.format(monthFormatter),
                            barCenterX,
                            dayY,
                            labelPaint
                        )
                    }

                    else -> {
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

    }
}
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MoodTrendsScreen(
    initialWeekStart: LocalDate,
    onBack: () -> Unit,
    onSelectDay: (LocalDate) -> Unit
) {

    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var obsidianFolder by remember { mutableStateOf<Uri?>(null) }

    LaunchedEffect(Unit) {
        obsidianFolder =
            com.example.mood.data.ObsidianStore.loadFolder(context)
    }

    val obsidianConnected = obsidianFolder != null

    val baseline = 5.0f



    @OptIn(kotlinx.serialization.InternalSerializationApi::class)
    suspend fun computeDailyAverage(
        start: LocalDate,
        end: LocalDate
    ): Float? {
        val scores = mutableListOf<Float>()

        var day = start
        while (!day.isAfter(end)) {
            val dayKey = day.toString()

            val entries =
                com.example.mood.data.LedgerStore
                    .loadEntriesForDay(context, dayKey)

            val reflection =
                com.example.mood.data.LedgerStore
                    .loadDailyReflection(context, dayKey)

            val eventTotal =
                entries.sumOf { it.delta.toDouble() }.toFloat()

            val computed = baseline + eventTotal

            val final =
                if (reflection != null)
                    computed + reflection.drift
                else
                    computed

            scores += final
            day = day.plusDays(1)
        }

        return if (scores.isNotEmpty())
            scores.average().toFloat()
        else
            null


    }


    var weekStart by remember(initialWeekStart) {
        mutableStateOf(initialWeekStart)
    }

    var visibleMonth by remember {
        mutableStateOf(initialWeekStart.withDayOfMonth(1))
    }

    var visibleYear by remember {
        mutableStateOf(initialWeekStart.year)
    }

    var range by remember { mutableStateOf(TrendsRange.WEEK) }

    var data by remember {
        mutableStateOf<List<DailyMoodPoint>>(emptyList())
    }



    LaunchedEffect(range) {
        // Prevent old data being drawn with new slotCount (flash)
        data = emptyList()

        val today = LocalDate.now()

        when (range) {
            TrendsRange.MONTH -> {
                visibleMonth = today.withDayOfMonth(1)
            }

            TrendsRange.YEAR -> {
                visibleYear = today.year
            }

            TrendsRange.WEEK -> {
                // weekStart already correct by design
            }
        }
    }








    val today = LocalDate.now()

    val completedDays =
        data.filter { !it.date.isAfter(today) }

    val weeklyAverage =
        if (completedDays.isNotEmpty())
            completedDays.map { it.score }.average().toFloat()
        else
            0f

    var visibleValue by remember { mutableStateOf<Float?>(null) }

    var weeklyReflection by remember { mutableStateOf<String>("") }
    var showWeeklyReflectionSheet by remember { mutableStateOf(false) }
    var monthlyReflection by remember { mutableStateOf("") }
    var showMonthlyReflectionSheet by remember { mutableStateOf(false) }


    LaunchedEffect(range, visibleMonth, visibleYear) {
        visibleValue =
            when (range) {
                TrendsRange.WEEK ->
                    weeklyAverage

                TrendsRange.MONTH -> {
                    val isCurrentMonth =
                        visibleMonth.year == today.year &&
                                visibleMonth.month == today.month

                    val start = visibleMonth
                    val end =
                        if (isCurrentMonth)
                            today
                        else
                            visibleMonth.plusMonths(1).minusDays(1)

                    computeDailyAverage(start, end)
                }

                TrendsRange.YEAR -> {
                    val isCurrentYear =
                        visibleYear == today.year

                    val start = LocalDate.of(visibleYear, 1, 1)
                    val end =
                        if (isCurrentYear)
                            today
                        else
                            LocalDate.of(visibleYear, 12, 31)

                    computeDailyAverage(start, end)
                }
            }
    }





    LaunchedEffect(range, weekStart, visibleMonth, visibleYear) {
        data =
            when (range) {
                TrendsRange.WEEK ->
                    LedgerStore.loadWeeklyMood(context, weekStart, baseline)
                TrendsRange.MONTH ->
                    LedgerStore.loadMonthlyMood(context, visibleMonth, baseline)
                TrendsRange.YEAR ->
                    LedgerStore.loadYearlyMood(context, visibleYear, baseline)
            }

        if (range == TrendsRange.WEEK) {
                weeklyReflection =
                    com.example.mood.data.LedgerStore
                        .loadWeeklyReflection(
                            context,
                            weekStart.toString()
                        )?.note ?: ""
            }
        if (range == TrendsRange.MONTH) {
            val monthKey =
                visibleMonth.format(
                    java.time.format.DateTimeFormatter.ofPattern("yyyy-MM")
                )

            monthlyReflection =
                LedgerStore
                    .loadMonthlyReflection(context, monthKey)
                    ?.note ?: ""
        }

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
                Icon(Icons.AutoMirrored.Filled.ArrowBack, null)

            }

            Spacer(Modifier.width(8.dp))

            Text(
                text = "Mood Trends",
                style = MaterialTheme.typography.headlineMedium
            )
        }

        Spacer(Modifier.height(8.dp))

// Range toggle (Weekly default)
        SingleChoiceSegmentedButtonRow(
            modifier = Modifier.fillMaxWidth()
        ) {
            SegmentedButton(
                selected = range == TrendsRange.WEEK,
                onClick = { range = TrendsRange.WEEK },
                shape = SegmentedButtonDefaults.itemShape(index = 0, count = 3)
            ) { Text("Weekly") }

            SegmentedButton(
                selected = range == TrendsRange.MONTH,
                onClick = { range = TrendsRange.MONTH },
                shape = SegmentedButtonDefaults.itemShape(index = 1, count = 3)
            ) { Text("Monthly") }

            SegmentedButton(
                selected = range == TrendsRange.YEAR,
                onClick = { range = TrendsRange.YEAR },
                shape = SegmentedButtonDefaults.itemShape(index = 2, count = 3)
            ) { Text("Yearly") }
        }


        if (range == TrendsRange.WEEK) {
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
        }
        if (range == TrendsRange.MONTH) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(
                    onClick = {
                        visibleMonth = visibleMonth.minusMonths(1)
                    }
                ) { Text("← Previous") }

                Text(
                    text = visibleMonth.format(
                        java.time.format.DateTimeFormatter.ofPattern("MMMM yyyy")
                    ),
                    color = Color.White.copy(alpha = 0.65f)
                )

                TextButton(
                    onClick = {
                        val next = visibleMonth.plusMonths(1)
                        if (!next.isAfter(LocalDate.now().withDayOfMonth(1))) {
                            visibleMonth = next
                        }
                    }
                ) { Text("Next →") }
            }
        }

        if (range == TrendsRange.YEAR) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(
                    onClick = {
                        visibleYear -= 1
                    }
                ) { Text("← Previous") }

                Text(
                    text = visibleYear.toString(),
                    color = Color.White.copy(alpha = 0.65f)
                )

                TextButton(
                    onClick = {
                        if (visibleYear < LocalDate.now().year) {
                            visibleYear += 1
                        }
                    }
                ) { Text("Next →") }
            }
        }


        WeeklyMoodBarChart(
            data = data,
            baseline = baseline,
            range = range,
            modifier = Modifier
                .fillMaxWidth()
                .height(300.dp),
            onDaySelected = { selectedDate ->
                onSelectDay(selectedDate)
            }
        )






        Spacer(Modifier.height(12.dp))

        when (range) {
            TrendsRange.WEEK -> {
                Column(verticalArrangement = Arrangement.spacedBy(24.dp)) {

                    Text(
                        text = "Weekly Average: %.2f".format(weeklyAverage),
                        style = MaterialTheme.typography.bodyLarge,
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.height(12.dp))



                    if (weeklyReflection.isBlank()) {
                        TextButton(
                            onClick = { showWeeklyReflectionSheet = true }
                        ) {
                            Text("Add Weekly Reflection")
                        }
                    } else {
                        Text(
                            text = "Weekly Reflection",
                            style = MaterialTheme.typography.labelMedium,
                            color = Color.White.copy(alpha = 0.55f)
                        )

                        Box(
                            modifier = Modifier.fillMaxWidth()
                        ) {

                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(max = 190.dp)   // 👈 caps growth
                                    .background(
                                        color = Color.White.copy(alpha = 0.04f),
                                        shape = RoundedCornerShape(12.dp)
                                    )
                                    .border(
                                        width = 1.dp,
                                        color = Color.White.copy(alpha = 0.12f),
                                        shape = RoundedCornerShape(12.dp)
                                    )
                                    .padding(
                                        start = 16.dp,
                                        top = 16.dp,
                                        end = 16.dp,
                                        bottom = 36.dp
                                    )
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .verticalScroll(rememberScrollState())
                                ) {
                                    Text(
                                        text = weeklyReflection,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = Color.White.copy(alpha = 0.85f)
                                    )
                                }
                            }


                            Row(
                                modifier = Modifier
                                    .align(Alignment.BottomEnd)
                                    .offset(x = 6.dp, y = 6.dp),
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {

                                // ✎ Edit
                                IconButton(
                                    onClick = { showWeeklyReflectionSheet = true }
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Edit,
                                        contentDescription = "Edit Reflection",
                                        tint = Color.White.copy(alpha = 0.7f),
                                        modifier = Modifier.size(18.dp)
                                    )
                                }

                                // ⬇ Export (only if Obsidian connected)
                                obsidianFolder?.let { folderUri ->
                                    IconButton(
                                        onClick = {
                                            coroutineScope.launch {
                                                exportWeeklyReflectionToObsidian(
                                                    context = context,
                                                    folderUri = folderUri,
                                                    weekStart = weekStart,
                                                    content = weeklyReflection
                                                )
                                            }
                                        }
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Download,
                                            contentDescription = "Export to Obsidian",
                                            tint = LedgerGold.copy(alpha = 0.8f),
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                            }

                        }
                    }
                }
            }

            TrendsRange.MONTH -> {
                Column(verticalArrangement = Arrangement.spacedBy(24.dp)) {

                    Text(
                        text = "Monthly Average: %.2f".format(visibleValue ?: 0f),
                        style = MaterialTheme.typography.bodyLarge,
                        color = Color.White
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    if (monthlyReflection.isBlank()) {
                        TextButton(
                            onClick = { showMonthlyReflectionSheet = true }
                        ) {
                            Text("Add Monthly Reflection")
                        }
                    } else {
                        Text(
                            text = "Monthly Reflection",
                            style = MaterialTheme.typography.labelMedium,
                            color = Color.White.copy(alpha = 0.55f)
                        )

                        Box(
                            modifier = Modifier.fillMaxWidth()
                        ) {

                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(max = 190.dp)   // 👈 caps growth
                                    .background(
                                        color = Color.White.copy(alpha = 0.04f),
                                        shape = RoundedCornerShape(12.dp)
                                    )
                                    .border(
                                        width = 1.dp,
                                        color = Color.White.copy(alpha = 0.12f),
                                        shape = RoundedCornerShape(12.dp)
                                    )
                                    .padding(
                                        start = 16.dp,
                                        top = 16.dp,
                                        end = 16.dp,
                                        bottom = 36.dp
                                    )
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .verticalScroll(rememberScrollState())
                                ) {
                                    Text(
                                        text = monthlyReflection,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = Color.White.copy(alpha = 0.85f)
                                    )
                                }
                            }


                            Row(
                                modifier = Modifier
                                    .align(Alignment.BottomEnd)
                                    .offset(x = 6.dp, y = 6.dp),
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {

                                // ✎ Edit
                                IconButton(
                                    onClick = { showMonthlyReflectionSheet = true }
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Edit,
                                        contentDescription = "Edit Reflection",
                                        tint = Color.White.copy(alpha = 0.7f),
                                        modifier = Modifier.size(18.dp)
                                    )
                                }

                                // ⬇ Export (only if Obsidian connected)
                                obsidianFolder?.let { folderUri ->
                                    IconButton(
                                        onClick = {
                                            coroutineScope.launch {
                                                exportMonthlyReflectionToObsidian(
                                                    context = context,
                                                    folderUri = folderUri,
                                                    visibleMonth = visibleMonth,
                                                    content = monthlyReflection
                                                )
                                            }
                                        }
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Download,
                                            contentDescription = "Export to Obsidian",
                                            tint = LedgerGold.copy(alpha = 0.8f),
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                            }

                        }
                    }
                }
            }


            TrendsRange.YEAR -> {
                Text("Yearly Average: %.2f".format(visibleValue ?: 0f))
            }
        }

    }
    if (showWeeklyReflectionSheet) {
        WeeklyReflectionSheet(
            title = "Weekly Reflection",
            initialText = weeklyReflection,
            onSave = { text ->

            weeklyReflection = text

                coroutineScope.launch {
                    LedgerStore.saveWeeklyReflection(
                        context = context,
                        reflection = WeeklyReflection(
                            weekStart = weekStart.toString(),
                            note = text
                        )
                    )
                }

                showWeeklyReflectionSheet = false
            },
            onDismiss = {
                showWeeklyReflectionSheet = false
            }
        )
    }
    if (showMonthlyReflectionSheet) {
        val monthKey =
            visibleMonth.format(
                java.time.format.DateTimeFormatter.ofPattern("yyyy-MM")
            )

        WeeklyReflectionSheet(
            title = "Monthly Reflection",
            initialText = monthlyReflection,
            onSave = { text ->
                monthlyReflection = text

                coroutineScope.launch {
                    LedgerStore.saveMonthlyReflection(
                        context,
                        MonthlyReflection(
                            monthKey = monthKey,
                            note = text
                        )
                    )
                }

                showMonthlyReflectionSheet = false
            },
            onDismiss = {
                showMonthlyReflectionSheet = false
            }
        )
    }


}
