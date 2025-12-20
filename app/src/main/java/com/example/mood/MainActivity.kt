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

private fun lerp(start: Float, end: Float, t: Float): Float =
    start + (end - start) * t





fun deltaLabel(delta: Float): String =
    when {
        delta > 0f -> "▲ %.2f".format(delta)
        delta < 0f -> "▼ %.2f".format(kotlin.math.abs(delta))
        else -> "— 0.00"
    }

@Composable
fun TodayScreen() {
    var entries by remember { mutableStateOf(listOf<LedgerEntry>()) }
    var showSheet by remember { mutableStateOf(false) }
    var entryBeingEdited by remember { mutableStateOf<LedgerEntry?>(null) }

    val baseline = 5.00f
    val drift = 0.00f
    val eventTotal = entries.sumOf { it.delta.toDouble() }.toFloat()
    val finalScore = baseline + eventTotal + drift

    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        entries = LedgerStore.loadEntries(context)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {

        // HEADER
        Text(
            text = "Today – Emotional Ledger",
            style = MaterialTheme.typography.headlineMedium.copy(
                fontWeight = FontWeight.Medium,   // was SemiBold
                letterSpacing = 0.sp,             // remove display spacing
                fontSize = 24.sp                  // down from 26
            ),
            color = Color.White
        )


        // METRICS
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Metric("Baseline", baseline)
            Metric("Events", eventTotal)
            Metric("Drift", drift)
        }

        HorizontalDivider(color = Color.White.copy(alpha = 0.2f))

        // ENTRIES
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

                    coroutineScope.launch {
                        LedgerStore.saveEntries(context, updated)
                    }
                }
            )

            if (index < entries.lastIndex) {
                HorizontalDivider(color = Color.White.copy(alpha = 0.08f))
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        // FINAL SCORE
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "Final mood",
                style = MaterialTheme.typography.headlineSmall,
                color = Color.White
            )
            Spacer(Modifier.width(12.dp))
            Text(
                text = "%.2f / 10.00".format(finalScore),
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White
            )
        }

        // ADD BUTTON
        Button(
            onClick = {
                entryBeingEdited = null
                showSheet = true
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            shape = RoundedCornerShape(24.dp)
        ) {
            Text("+ Add Entry")
        }
    }

    // BOTTOM SHEET
    if (showSheet) {
        AddEntrySheet(
            existingEntry = entryBeingEdited,
            onSave = { savedEntry ->
                val updated = entries
                    .filterNot { it.id == savedEntry.id } + savedEntry

                entries = updated.sortedBy { it.timestamp }

                coroutineScope.launch {
                    LedgerStore.saveEntries(context, entries)
                }

                entryBeingEdited = null
                showSheet = false
            },
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
                onClick = {},
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