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
import com.example.mood.model.InfluenceCardModel
import com.example.mood.model.CardPolarity
import com.example.mood.model.CardRank
import com.example.mood.ui.buildInfluenceCardModel
import com.example.mood.ui.InfluenceGeneratedCard
import com.example.mood.model.InfluenceOverride
import kotlinx.coroutines.launch
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import java.io.File
import java.io.FileOutputStream
import android.graphics.Bitmap
import android.graphics.BitmapFactory


private val entryDateFormatter =
    java.time.format.DateTimeFormatter.ofPattern("MM/dd/yy")

enum class InfluenceSort {
    AZ,
    COUNT,
    INTENSITY,
    TOTAL;

    fun displayName(): String =
        when (this) {
            AZ -> "Alphabetical"
            COUNT -> "Most Frequent"
            INTENSITY -> "Average Impact"
            TOTAL -> "Total Impact"
        }
}




enum class TimeScale {
    ALL_TIME,
    WEEK,
    MONTH,
    YEAR
}



@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InfluencesScreen(
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var influenceOverrides by remember {
        mutableStateOf<Map<String, InfluenceOverride>>(emptyMap())
    }
    val sortInactiveColor = Color.White.copy(alpha = 0.65f)
    val sortPrimaryColor = LedgerGold          // Most → Least / A → Z
    val sortReverseColor = Color(0xFF2EC4B6)   // Teal


    var allTags by remember { mutableStateOf<List<TagStats>>(emptyList()) }
    var sortMode by remember { mutableStateOf(InfluenceSort.TOTAL) }
    var sortExpanded by remember { mutableStateOf(false) }
    var typeExpanded by remember { mutableStateOf(false) }

    var sortAscending by remember { mutableStateOf(false) }

// Insight Lens state
    var selectedTimeScale by remember { mutableStateOf(TimeScale.ALL_TIME) }

    var selectedType by remember { mutableStateOf<String?>(null) }
    var selectedSubType by remember { mutableStateOf<String?>(null) }


    var selectedInfluence by remember { mutableStateOf<String?>(null) }
    var editingCard by remember { mutableStateOf<InfluenceCardModel?>(null) }
    var editedCardOverride by remember { mutableStateOf<InfluenceCardModel?>(null) }
    var influenceEntries by remember { mutableStateOf(emptyList<com.example.mood.model.LedgerEntry>()) }
    var expandedEntryId by remember { mutableStateOf<Long?>(null) }
    val sheetState = rememberModalBottomSheetState(
        skipPartiallyExpanded = true
    )

    LaunchedEffect(Unit) {
        influenceOverrides =
            LedgerStore.loadInfluenceOverrides(context)
    }



    fun effectiveTypeFor(tag: String): String {
        val override = influenceOverrides[tag]?.type
        if (!override.isNullOrBlank()) return override

        // fallback to generated card model type
        val stats = allTags.firstOrNull { it.tag == tag } ?: return ""
        return buildInfluenceCardModel(
            tag = stats.tag,
            stats = stats,
            entries = emptyList()
        ).type.orEmpty()
    }


    fun isEntryInTimeScale(
        timestamp: Long
    ): Boolean {
        if (selectedTimeScale == TimeScale.ALL_TIME) return true

        val entryDate =
            java.time.Instant
                .ofEpochMilli(timestamp)
                .atZone(java.time.ZoneId.systemDefault())
                .toLocalDate()

        val today = java.time.LocalDate.now()

        return when (selectedTimeScale) {
            TimeScale.WEEK ->
                entryDate.isAfter(today.minusDays(7))

            TimeScale.MONTH ->
                entryDate.month == today.month &&
                        entryDate.year == today.year

            TimeScale.YEAR ->
                entryDate.year == today.year

            TimeScale.ALL_TIME -> true
        }
    }


    LaunchedEffect(selectedTimeScale) {

        if (selectedTimeScale == TimeScale.ALL_TIME) {
            allTags =
                LedgerStore
                    .loadTagStats(context)
                    .values
                    .sortedBy { it.tag.lowercase() }
            return@LaunchedEffect
        }

        val days =
            LedgerStore.loadDaysWithEntries(context)

        val stats = mutableMapOf<String, Pair<Int, Float>>() // count, totalDelta

        days.forEach { dayKey ->
            val entries =
                LedgerStore.loadEntriesForDay(context, dayKey)

            entries
                .filter { isEntryInTimeScale(it.timestamp) }
                .forEach { entry ->
                    entry.tags.forEach { tag ->
                        val (count, total) =
                            stats[tag] ?: (0 to 0f)

                        stats[tag] =
                            (count + 1) to (total + entry.delta)
                    }
                }
        }

        allTags =
            stats.map { (tag, pair) ->
                TagStats(
                    tag = tag,
                    count = pair.first,
                    totalDelta = pair.second
                )
            }.sortedBy { it.tag.lowercase() }
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

        // ───────────────── Insight Lens Header ─────────────────

        Text(
            text = "Showing ${selectedType ?: "All"} · ${selectedTimeScale.name.lowercase().replaceFirstChar { it.uppercase() }} · ${sortMode.displayName()}",
            style = MaterialTheme.typography.labelMedium,
            color = Color.White.copy(alpha = 0.65f)
        )

        Spacer(Modifier.height(12.dp))

// Time Scale
        SingleChoiceSegmentedButtonRow {
            TimeScale.values().forEachIndexed { index, scale ->
                SegmentedButton(
                    selected = selectedTimeScale == scale,
                    onClick = { selectedTimeScale = scale },
                    shape = SegmentedButtonDefaults.itemShape(
                        index = index,
                        count = TimeScale.values().size
                    ),
                    label = {
                        Text(
                            when (scale) {
                                TimeScale.ALL_TIME -> "All-Time"
                                TimeScale.WEEK -> "Week"
                                TimeScale.MONTH -> "Month"
                                TimeScale.YEAR -> "Year"
                            }
                        )
                    }

                )
            }
        }


        Spacer(Modifier.height(12.dp))

// Collect used types with counts (based on visible tags)
        val availableTypesWithCounts: Map<String, Int> =
            remember(allTags, influenceOverrides) {
                allTags
                    .mapNotNull { stat ->
                        effectiveTypeFor(stat.tag).takeIf { it.isNotBlank() }
                    }
                    .groupingBy { it }
                    .eachCount()
                    .toSortedMap(compareBy { it.lowercase() })
            }

// Collect sub-types for the selected type (with counts)
        val availableSubTypesWithCounts: Map<String, Int> =
            remember(allTags, influenceOverrides, selectedType) {
                if (selectedType == null) emptyMap()
                else
                    allTags
                        .mapNotNull { stat ->
                            val override = influenceOverrides[stat.tag]
                            if (override?.type.equals(selectedType, ignoreCase = true))
                                override?.subType?.trim()
                            else null
                        }
                        .filter { it.isNotBlank() }
                        .groupingBy { it }
                        .eachCount()
                        .toSortedMap(compareBy { it.lowercase() })
            }


        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {

            // ── Type ───────────────────────────────
            Box(modifier = Modifier.weight(1f)) {
                AssistChip(
                    onClick = { typeExpanded = true },
                    label = {
                        val count =
                            selectedType?.let { availableTypesWithCounts[it] }

                        Text(
                            if (selectedType == null)
                                "Type: All"
                            else
                                "Type: $selectedType ($count)"
                        )
                    }
                )

                DropdownMenu(
                    expanded = typeExpanded,
                    onDismissRequest = { typeExpanded = false }
                ) {
                    DropdownMenuItem(
                        text = { Text("All") },
                        onClick = {
                            selectedType = null
                            selectedSubType = null
                            typeExpanded = false
                        }
                    )

                    if (availableTypesWithCounts.isNotEmpty()) {
                        Divider()

                        availableTypesWithCounts.forEach { (type, count) ->
                            DropdownMenuItem(
                                text = { Text("$type ($count)") },
                                onClick = {
                                    selectedType = type
                                    selectedSubType = null
                                    typeExpanded = false
                                }
                            )
                        }
                    }
                }
            }

            // ── Sub-type ───────────────────────────
            Box(modifier = Modifier.weight(1f)) {

                var subTypeExpanded by remember { mutableStateOf(false) }

                AssistChip(
                    onClick = {
                        if (selectedType != null) {
                            subTypeExpanded = true
                        }
                    },
                    enabled = selectedType != null,
                    label = {
                        val count =
                            selectedSubType?.let { availableSubTypesWithCounts[it] }

                        Text(
                            when {
                                selectedType == null ->
                                    "Sub-type: —"
                                selectedSubType == null ->
                                    "Sub-type: All"
                                else ->
                                    "Sub-type: $selectedSubType ($count)"
                            }
                        )
                    }
                )

                DropdownMenu(
                    expanded = subTypeExpanded,
                    onDismissRequest = { subTypeExpanded = false }
                ) {
                    DropdownMenuItem(
                        text = { Text("All") },
                        onClick = {
                            selectedSubType = null
                            subTypeExpanded = false
                        }
                    )

                    if (availableSubTypesWithCounts.isNotEmpty()) {
                        Divider()

                        availableSubTypesWithCounts.forEach { (subType, count) ->
                            DropdownMenuItem(
                                text = { Text("$subType ($count)") },
                                onClick = {
                                    selectedSubType = subType
                                    subTypeExpanded = false
                                }
                            )
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(12.dp))


// Sub-type filter (appears only when Type is selected)








        Spacer(Modifier.height(12.dp))







        val filteredTags =
            allTags.filter { stat ->
                val matchesType =
                    selectedType == null ||
                            effectiveTypeFor(stat.tag)
                                .equals(selectedType, ignoreCase = true)

                val matchesSubType =
                    selectedSubType == null ||
                            influenceOverrides[stat.tag]?.subType
                                ?.equals(selectedSubType, ignoreCase = true) == true

                matchesType && matchesSubType
            }


        val visibleTags =
            when (sortMode) {
                InfluenceSort.AZ ->
                    if (sortAscending)
                        filteredTags.sortedBy { it.tag.lowercase() }
                    else
                        filteredTags.sortedByDescending { it.tag.lowercase() }

                InfluenceSort.COUNT ->
                    if (sortAscending)
                        filteredTags.sortedBy { it.count }
                    else
                        filteredTags.sortedByDescending { it.count }

                InfluenceSort.INTENSITY ->
                    if (sortAscending)
                        filteredTags.sortedBy { kotlin.math.abs(it.average) }
                    else
                        filteredTags.sortedByDescending { kotlin.math.abs(it.average) }

                InfluenceSort.TOTAL ->
                    if (sortAscending)
                        filteredTags.sortedBy { it.count * kotlin.math.abs(it.average) }
                    else
                        filteredTags.sortedByDescending { it.count * kotlin.math.abs(it.average) }
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
            // ── Sortable Column Headers ─────────────────────
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {

                // Alphabetical: A→Z = primary (Gold)
                fun alphaHeaderColor(isActive: Boolean): Color =
                    if (!isActive) sortInactiveColor
                    else if (sortAscending) sortPrimaryColor
                    else sortReverseColor

                // Numeric: Most→Least = primary (Gold)
                fun numericHeaderColor(isActive: Boolean): Color =
                    if (!isActive) sortInactiveColor
                    else if (!sortAscending) sortPrimaryColor
                    else sortReverseColor


                // Influence name (A–Z / Z–A)
                Text(
                    text = "Influence",
                    modifier = Modifier
                        .weight(1f)
                        .clickable {
                            if (sortMode == InfluenceSort.AZ) {
                                sortAscending = !sortAscending
                            } else {
                                sortMode = InfluenceSort.AZ
                                sortAscending = true // A → Z
                            }
                        },
                    color = alphaHeaderColor(sortMode == InfluenceSort.AZ),

                    fontWeight = FontWeight.Medium
                )

                // Count
                Text(
                    text = "#",
                    modifier = Modifier
                        .width(24.dp)
                        .clickable {
                            if (sortMode == InfluenceSort.COUNT) {
                                sortAscending = !sortAscending
                            } else {
                                sortMode = InfluenceSort.COUNT
                                sortAscending = false // Most → Least
                            }
                        },
                    color = numericHeaderColor(sortMode == InfluenceSort.COUNT),
                    textAlign = androidx.compose.ui.text.style.TextAlign.End,
                    fontWeight = FontWeight.Medium
                )

                Spacer(Modifier.width(12.dp))

                // Average impact
                Text(
                    text = "Avg",
                    modifier = Modifier
                        .width(56.dp)
                        .clickable {
                            if (sortMode == InfluenceSort.INTENSITY) {
                                sortAscending = !sortAscending
                            } else {
                                sortMode = InfluenceSort.INTENSITY
                                sortAscending = false
                            }
                        },
                    color = numericHeaderColor(sortMode == InfluenceSort.INTENSITY),

                    textAlign = androidx.compose.ui.text.style.TextAlign.End,
                    fontWeight = FontWeight.Medium
                )

                Spacer(Modifier.width(8.dp))

                // Total impact
                Text(
                    text = "Total",
                    modifier = Modifier
                        .width(64.dp)
                        .clickable {
                            if (sortMode == InfluenceSort.TOTAL) {
                                sortAscending = !sortAscending
                            } else {
                                sortMode = InfluenceSort.TOTAL
                                sortAscending = false
                            }
                        },
                    color = numericHeaderColor(sortMode == InfluenceSort.TOTAL),

                    textAlign = androidx.compose.ui.text.style.TextAlign.End,
                    fontWeight = FontWeight.ExtraBold
                )
            }




            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 32.dp) // ⬅ reserves space for column headers
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {




            visibleTags.forEach { stat ->


                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {

                        val effectiveType =
                            influenceOverrides[stat.tag]?.type.orEmpty()

                        val displayName =
                            formatInfluenceName(stat.tag) +
                                    if (effectiveType.isBlank()) " •" else ""


                        Text(
                            text = displayName,
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

                        Spacer(Modifier.width(8.dp))


                        Text(
                            text = String.format("%+.2f", stat.count * stat.average),
                            modifier = Modifier.width(64.dp),
                            color = deltaColor(stat.average),
                            textAlign = androidx.compose.ui.text.style.TextAlign.End,
                            fontWeight = FontWeight.ExtraBold
                        )




                    }
                }
            }
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .width(1.dp)
                    .align(Alignment.CenterEnd)
                    .offset(x = -54.dp) // Total width (64) + spacer (8)
                    .background(Color.White.copy(alpha = 0.12f))
            )

        }
    }

    if (selectedInfluence != null) {
        ModalBottomSheet(
            onDismissRequest = {
                selectedInfluence = null
                influenceEntries = emptyList()
                expandedEntryId = null
            },
            sheetState = sheetState,
            containerColor = MaterialTheme.colorScheme.background
        ) {

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {






            val tagStats =
                allTags.firstOrNull { it.tag == selectedInfluence }

            if (tagStats != null && influenceEntries.isNotEmpty()) {

                val generatedCard =
                    buildInfluenceCardModel(
                        tag = tagStats.tag,
                        stats = tagStats,
                        entries = influenceEntries
                    )

                val override = influenceOverrides[generatedCard.name]

                val cardModel =
                    generatedCard.copy(
                        type = override?.type ?: generatedCard.type,
                        subType = override?.subType ?: generatedCard.subType,
                        description = override?.description ?: generatedCard.description,
                        imageResId = override?.imageResId ?: generatedCard.imageResId,
                        imagePath = override?.imagePath ?: generatedCard.imagePath
                    )




                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    InfluenceGeneratedCard(
                        card = cardModel,
                        onEditClick = { card ->
                            editingCard = card
                        }
                    )


                }


                Spacer(Modifier.height(12.dp))
            }




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
    if (editingCard != null) {
        ModalBottomSheet(
            onDismissRequest = {
                editingCard = null
            },
            containerColor = MaterialTheme.colorScheme.background
        ) {
            InfluenceEditSheet(
                card = editingCard!!,
                onDone = { updated ->
                    val oldName = editingCard!!.name
                    val newName = updated.name

                    if (oldName != newName) {
                        scope.launch {
                            LedgerStore.renameTag(
                                context = context,
                                oldTag = oldName,
                                newTag = newName
                            )

                            // 🔁 IMMEDIATELY refresh visible tag list
                            allTags =
                                LedgerStore
                                    .loadTagStats(context)
                                    .values
                                    .sortedBy { it.tag.lowercase() }

                            // Clear selection to avoid dangling old tag
                            selectedInfluence = null
                        }
                    }


                    val override = InfluenceOverride(
                        tag = newName,
                        type = normalizeCategory(updated.type),
                        subType = updated.subType?.trim(),
                        description = updated.description,
                        imageResId = updated.imageResId,
                        imagePath = updated.imagePath
                    )




                    scope.launch {
                        LedgerStore.saveInfluenceOverride(context, override)
                        influenceOverrides =
                            influenceOverrides + (override.tag to override)

                    }



                    editingCard = null
                    selectedInfluence = null



                }

            )



        }
    }
}

private fun normalizeCategory(input: String?): String {
    return input
        ?.trim()
        ?.lowercase()
        ?.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
        ?: ""
}




@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InfluenceEditSheet(
    card: InfluenceCardModel,
    onDone: (InfluenceCardModel) -> Unit
) {
    var name by remember { mutableStateOf(card.name) }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

// Load existing influence overrides for suggestions
    var allOverrides by remember { mutableStateOf<Map<String, InfluenceOverride>>(emptyMap()) }

    var type by remember { mutableStateOf(card.type) }
    var hasEditedType by remember { mutableStateOf(false) }
    var typeExpanded by remember { mutableStateOf(false) }


    var subType by remember { mutableStateOf(card.subType ?: "") }

    LaunchedEffect(Unit) {
        allOverrides =
            LedgerStore.loadInfluenceOverrides(context)
                .mapValues { (_, override) ->
                    override.copy(
                        type = normalizeCategory(override.type),
                        subType = override.subType?.trim()
                    )
                }
    }

    val availableTypes: List<String> =
        remember(allOverrides) {
            allOverrides.values
                .mapNotNull { it.type }
                .map { normalizeCategory(it) }
                .filter { it.isNotBlank() }
                .distinct()
                .sorted()
        }



    val typeSuggestions =
        allOverrides.values
            .mapNotNull { it.type }
            .distinct()
            .sorted()

    val subTypeSuggestions =
        remember(type, allOverrides) {
            allOverrides.values
                .filter { it.type == type }
                .mapNotNull { it.subType }
                .distinct()
                .sorted()
        }




    var subTypeExpanded by remember { mutableStateOf(false) }
    var showNewSubTypeDialog by remember { mutableStateOf(false) }
    var newSubTypeText by remember { mutableStateOf("") }

    var description by remember { mutableStateOf(card.description) }
    var imagePath by remember { mutableStateOf(card.imagePath) }

    val pickImageLauncher =
        rememberLauncherForActivityResult(
            contract = ActivityResultContracts.GetContent()
        ) { uri: Uri? ->
            if (uri != null) {
                scope.launch {
                    imagePath = normalizeCardArt(
                        context = context,
                        sourceUri = uri,
                        tag = card.name
                    )
                }
            }
        }


    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {

        Text(
            text = "Edit Influence",
            style = MaterialTheme.typography.headlineSmall
        )

        OutlinedTextField(
            value = name,
            onValueChange = { name = it },
            label = { Text("Name") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )




        var showNewTypeDialog by remember { mutableStateOf(false) }
        var newTypeText by remember { mutableStateOf("") }


        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { typeExpanded = true }
        ) {
            OutlinedTextField(
                value = type,
                onValueChange = { },
                readOnly = true,
                enabled = false,
                label = { Text("Type") },
                modifier = Modifier.fillMaxWidth()
            )



            DropdownMenu(
                expanded = typeExpanded,
                onDismissRequest = { typeExpanded = false },
                modifier = Modifier.fillMaxWidth()
            ) {

                // Scrollable options (max ~6 items visible)
                Column(
                    modifier = Modifier
                        .heightIn(max = 48.dp * 6) // ~6 menu items

                        .verticalScroll(rememberScrollState())
                ) {
                    availableTypes.forEach { option ->
                        DropdownMenuItem(
                            text = { Text(option) },
                            onClick = {
                                type = option
                                typeExpanded = false
                            }
                        )
                    }
                }

                Divider()

// Sticky action
                DropdownMenuItem(
                    text = { Text("New…") },
                    onClick = {
                        typeExpanded = false
                        newTypeText = ""
                        showNewTypeDialog = true
                    }
                )


            }
        }


        var subTypeExpanded by remember { mutableStateOf(false) }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(enabled = type.isNotBlank()) {
                    subTypeExpanded = true
                }
        ) {
            OutlinedTextField(
                value = subType,
                onValueChange = { },
                readOnly = true,
                enabled = false,
                label = { Text("Sub-type") },
                modifier = Modifier.fillMaxWidth()
            )

            DropdownMenu(
                expanded = subTypeExpanded && type.isNotBlank(),
                onDismissRequest = { subTypeExpanded = false },
                modifier = Modifier.fillMaxWidth()
            ) {

                Column(
                    modifier = Modifier
                        .heightIn(max = 48.dp * 6) // ~6 menu items

                        .verticalScroll(rememberScrollState())
                ) {
                    subTypeSuggestions.forEach { option ->
                        DropdownMenuItem(
                            text = { Text(option) },
                            onClick = {
                                subType = option
                                subTypeExpanded = false
                            }
                        )
                    }
                }

                Divider()

                DropdownMenuItem(
                    text = { Text("New…") },
                    onClick = {
                        subTypeExpanded = false
                        newSubTypeText = ""
                        showNewSubTypeDialog = true
                    }
                )

            }
        }
        if (showNewSubTypeDialog) {
            AlertDialog(
                onDismissRequest = { showNewSubTypeDialog = false },
                title = { Text("New Sub-type") },
                text = {
                    OutlinedTextField(
                        value = newSubTypeText,
                        onValueChange = { newSubTypeText = it },
                        label = { Text("Sub-type name") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            val trimmed = newSubTypeText.trim()
                            if (trimmed.isNotEmpty()) {
                                subType = trimmed
                                showNewSubTypeDialog = false
                            }
                        }
                    ) {
                        Text("Add")
                    }
                },
                dismissButton = {
                    TextButton(
                        onClick = { showNewSubTypeDialog = false }
                    ) {
                        Text("Cancel")
                    }
                }
            )
        }





        OutlinedTextField(
            value = description,
            onValueChange = { description = it },
            label = { Text("Description") },
            modifier = Modifier.fillMaxWidth(),
            minLines = 3
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedButton(
                onClick = { pickImageLauncher.launch("image/*") }
            ) {
                Text("Choose Image")
            }

            if (imagePath != null) {
                Spacer(Modifier.width(8.dp))
                Text(
                    text = "Selected",
                    color = Color.White.copy(alpha = 0.7f),
                    style = MaterialTheme.typography.labelMedium
                )
            }

            Spacer(Modifier.weight(1f))

            OutlinedButton(
                onClick = {
                    onDone(
                        card.copy(
                            name = name.trim(),
                            type = type,
                            subType = subType.ifBlank { null },
                            description = description,
                            imagePath = imagePath
                        )
                    )

                }
            ) {
                Text("Update")
            }
        }



        Spacer(Modifier.height(8.dp))



        if (showNewTypeDialog) {
            AlertDialog(
                onDismissRequest = { showNewTypeDialog = false },
                title = { Text("New Type") },
                text = {
                    OutlinedTextField(
                        value = newTypeText,
                        onValueChange = { newTypeText = it },
                        label = { Text("Type name") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            val trimmed = newTypeText.trim()
                            if (trimmed.isNotEmpty()) {
                                type = trimmed
                                showNewTypeDialog = false
                            }
                        }
                    ) {
                        Text("Add")
                    }
                },
                dismissButton = {
                    TextButton(
                        onClick = { showNewTypeDialog = false }
                    ) {
                        Text("Cancel")
                    }
                }
            )
        }

    }
}
private fun normalizeCardArt(
    context: android.content.Context,
    sourceUri: Uri,
    tag: String,
    targetWidth: Int = 1024,
    targetHeight: Int = 552, // matches ~260x140 ratio
    quality: Int = 82
): String? {
    return runCatching {
        val resolver = context.contentResolver

        // Decode bounds
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        resolver.openInputStream(sourceUri)?.use { BitmapFactory.decodeStream(it, null, bounds) }

        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null

        // Downsample decode close to target
        val sample = run {
            var s = 1
            while ((bounds.outWidth / s) > targetWidth * 2 && (bounds.outHeight / s) > targetHeight * 2) {
                s *= 2
            }
            s
        }

        val opts = BitmapFactory.Options().apply { inSampleSize = sample }
        val decoded =
            resolver.openInputStream(sourceUri)?.use { BitmapFactory.decodeStream(it, null, opts) }
                ?: return null

        // Center-crop to target aspect ratio
        val targetRatio = targetWidth.toFloat() / targetHeight.toFloat()
        val srcRatio = decoded.width.toFloat() / decoded.height.toFloat()

        val crop = if (srcRatio > targetRatio) {
            // too wide: crop left/right
            val newWidth = (decoded.height * targetRatio).toInt()
            val x = (decoded.width - newWidth) / 2
            Bitmap.createBitmap(decoded, x, 0, newWidth, decoded.height)
        } else {
            // too tall: crop top/bottom
            val newHeight = (decoded.width / targetRatio).toInt()
            val y = (decoded.height - newHeight) / 2
            Bitmap.createBitmap(decoded, 0, y, decoded.width, newHeight)
        }

        val scaled = Bitmap.createScaledBitmap(crop, targetWidth, targetHeight, true)

        // Save into app-private storage
        val dir = File(context.filesDir, "influence_art").apply { mkdirs() }
        val safeName = tag.lowercase().replace(Regex("[^a-z0-9_\\-]"), "_")
        val outFile = File(dir, "${safeName}.jpg")

        FileOutputStream(outFile).use { fos ->
            scaled.compress(Bitmap.CompressFormat.JPEG, quality, fos)
        }

        // cleanup
        decoded.recycle()
        if (crop != decoded) crop.recycle()
        if (scaled != crop) scaled.recycle()

        outFile.absolutePath
    }.getOrNull()
}
internal fun formatInfluenceName(raw: String): String {
    return raw
        .split(" ")
        .filter { it.isNotBlank() }
        .joinToString(" ") { word ->
            if (word.all { it.isUpperCase() }) {
                word
            } else {
                word.lowercase()
                    .replaceFirstChar { it.uppercase() }
            }
        }
}



