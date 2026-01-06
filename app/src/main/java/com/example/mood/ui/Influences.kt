@file:Suppress("NewApi")
@file:OptIn(kotlinx.serialization.InternalSerializationApi::class)

package com.example.mood.ui

import androidx.compose.runtime.Composable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.foundation.background
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.dp
import androidx.compose.ui.graphics.Color
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import com.example.mood.ui.theme.LedgerGold
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import com.example.mood.data.LedgerStore
import com.example.mood.model.TagStats
import androidx.compose.ui.text.font.FontWeight
import com.example.mood.deltaColor
import androidx.compose.foundation.clickable


private val entryDateFormatter =
    java.time.format.DateTimeFormatter.ofPattern("MM/dd/yy")

enum class InfluenceSort {
    AZ,
    COUNT,
    INTENSITY
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InfluencesScreen(
    onBack: () -> Unit
) {
    val context = LocalContext.current
    var allTags by remember { mutableStateOf<List<TagStats>>(emptyList()) }
    var sortMode by remember { mutableStateOf(InfluenceSort.AZ) }
    var sortAscending by remember { mutableStateOf(true) }
    var selectedInfluence by remember { mutableStateOf<String?>(null) }
    var influenceEntries by remember { mutableStateOf(emptyList<com.example.mood.model.LedgerEntry>()) }
    var expandedEntryId by remember { mutableStateOf<Long?>(null) }




    LaunchedEffect(Unit) {
        allTags =
            LedgerStore
                .loadTagStats(context)
                .values
                .sortedBy { it.tag.lowercase() }
    }

    LaunchedEffect(selectedInfluence) {
        val tag = selectedInfluence ?: return@LaunchedEffect

        val daysWithData =
            LedgerStore.loadDaysWithEntries(context)

        val results = mutableListOf<com.example.mood.model.LedgerEntry>()

        daysWithData.forEach { dayKey ->
            val entries =
                LedgerStore.loadEntriesForDay(context, dayKey)

            results += entries.filter { it.tags.contains(tag) }
        }

        influenceEntries = results.sortedByDescending { it.timestamp }
    }


    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
    ) {


        // Header
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = Color.White
                )
            }

            Spacer(Modifier.width(8.dp))

            Text(
                text = "Influences",
                style = MaterialTheme.typography.headlineLarge,
                color = LedgerGold
            )
        }

        Spacer(Modifier.height(16.dp))

        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            AssistChip(
                onClick = {
                    if (sortMode == InfluenceSort.AZ) {
                        sortAscending = !sortAscending
                    } else {
                        sortMode = InfluenceSort.AZ
                        sortAscending = true   // A → Z default
                    }
                },
                label = { Text("A–Z") },

                colors =
                    if (sortMode == InfluenceSort.AZ)
                        AssistChipDefaults.assistChipColors(
                            containerColor = LedgerGold,
                            labelColor = Color.Black
                        )
                    else
                        AssistChipDefaults.assistChipColors()
            )

            AssistChip(
                onClick = {
                    if (sortMode == InfluenceSort.COUNT) {
                        sortAscending = !sortAscending
                    } else {
                        sortMode = InfluenceSort.COUNT
                        sortAscending = false  // high → low default
                    }
                },
                label = { Text("Count") },

                colors =
                    if (sortMode == InfluenceSort.COUNT)
                        AssistChipDefaults.assistChipColors(
                            containerColor = LedgerGold,
                            labelColor = Color.Black
                        )
                    else
                        AssistChipDefaults.assistChipColors()
            )


            AssistChip(
                onClick = {
                    if (sortMode == InfluenceSort.INTENSITY) {
                        sortAscending = !sortAscending
                    } else {
                        sortMode = InfluenceSort.INTENSITY
                        sortAscending = false  // strongest first default
                    }
                },
                label = { Text("Intensity") },

                colors =
                if (sortMode == InfluenceSort.INTENSITY)
                    AssistChipDefaults.assistChipColors(
                        containerColor = LedgerGold,
                        labelColor = Color.Black
                    )
                else
                    AssistChipDefaults.assistChipColors()
        )
    }
        Spacer(Modifier.height(12.dp))


        val visibleTags =
            when (sortMode) {
                InfluenceSort.AZ ->
                    if (sortAscending)
                        allTags.sortedBy { it.tag.lowercase() }
                    else
                        allTags.sortedByDescending { it.tag.lowercase() }

                InfluenceSort.COUNT ->
                    if (sortAscending)
                        allTags.sortedBy { it.count }
                    else
                        allTags.sortedByDescending { it.count }

                InfluenceSort.INTENSITY ->
                    if (sortAscending)
                        allTags.sortedBy { kotlin.math.abs(it.average) }
                    else
                        allTags.sortedByDescending { kotlin.math.abs(it.average) }
            }



        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .background(
                    color = Color.White.copy(alpha = 0.04f),
                    shape = RoundedCornerShape(16.dp)
                )
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {

                visibleTags.forEach { stat ->

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {

                        Text(
                            text = stat.tag,
                            modifier = Modifier
                                .weight(1f)
                                .clickable {
                                    selectedInfluence = stat.tag
                                },
                            color = deltaColor(stat.average),
                            style = MaterialTheme.typography.bodyMedium
                        )



                        Text(
                            text = stat.count.toString(),
                            modifier = Modifier.width(24.dp),
                            color = Color.White.copy(alpha = 0.55f),
                            textAlign = androidx.compose.ui.text.style.TextAlign.End
                        )

                        Spacer(Modifier.width(12.dp))

                        Text(
                            text = String.format("%+.2f", stat.average),
                            modifier = Modifier.width(56.dp),
                            color = deltaColor(stat.average),
                            textAlign = androidx.compose.ui.text.style.TextAlign.End,
                            fontWeight = FontWeight.Medium
                        )

                    }
                }
            }

        }
    }
    if (selectedInfluence != null) {
        ModalBottomSheet(
            onDismissRequest = {
                selectedInfluence = null
                influenceEntries = emptyList()
                expandedEntryId = null
            }
            ,
            containerColor = MaterialTheme.colorScheme.background
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {

                Text(
                    text = selectedInfluence!!,
                    style = MaterialTheme.typography.headlineSmall,
                    color = deltaColor(
                        influenceEntries.firstOrNull()?.delta ?: 0f
                    )
                )

                Spacer(Modifier.height(8.dp))

                if (influenceEntries.isEmpty()) {
                    Text(
                        text = "No entries found",
                        color = Color.White.copy(alpha = 0.6f)
                    )
                } else {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        influenceEntries.forEach { entry ->

                            val isExpanded = expandedEntryId == entry.id

                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        expandedEntryId =
                                            if (isExpanded) null else entry.id
                                    },
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {

                                    Text(
                                        text =
                                            java.time.Instant
                                                .ofEpochMilli(entry.timestamp)
                                                .atZone(java.time.ZoneId.systemDefault())
                                                .toLocalDate()
                                                .format(entryDateFormatter),

                                        modifier = Modifier.width(72.dp),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Color.White.copy(alpha = 0.5f)
                                    )

                                    Text(
                                        text = entry.title.ifBlank { "Untitled" },
                                        color = deltaColor(entry.delta),
                                        fontWeight = FontWeight.Medium
                                    )
                                }

                                if (isExpanded && entry.note.isNotBlank()) {
                                    Text(
                                        text = entry.note,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Color.White.copy(alpha = 0.75f),
                                        modifier = Modifier.padding(start = 72.dp)
                                    )
                                }
                            }
                        }

                    }
                }

                Spacer(Modifier.height(24.dp))
            }
        }
    }

}


