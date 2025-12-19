package com.example.mood

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.mood.ui.theme.*
import java.util.*
import androidx.compose.runtime.*
import com.example.mood.model.LedgerEntry
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.imePadding
import androidx.compose.material.icons.filled.Close
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.clickable
import androidx.compose.foundation.border
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource




private fun snapToQuarter(value: Float): Float {
    val step = 0.25f
    val snapped = kotlin.math.round(value / step) * step
    return when {
        kotlin.math.abs(snapped) < 0.125f -> 0f
        snapped < -2f -> -2f
        snapped > 2f -> 2f
        else -> snapped
    }
}



@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEntrySheet(
    existingEntry: LedgerEntry? = null,
    onSave: (LedgerEntry) -> Unit,
    onDismiss: () -> Unit
) {
    var delta by remember { mutableStateOf(existingEntry?.delta ?: 0f) }
    var tagsText by remember { mutableStateOf("") }
    var tags by remember { mutableStateOf(existingEntry?.tags ?: emptyList()) }

    var note by remember { mutableStateOf(existingEntry?.note ?: "") }

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var title by remember { mutableStateOf(existingEntry?.title ?: "") }
    val feelingOptions = listOf(
        "Excited",
        "Happy",
        "Content",
        "Embarrassed",
        "Frustrated",
        "Overwhelmed",
        "Sad",
        "Angry",
        "Anxious",
        "Hopeful"
    )

    val pastTags = listOf(
        "love", "Jeremy", "phone call", "smile", "anxiety", "dream", "work", "family"
    )

    var feeling by remember { mutableStateOf(existingEntry?.feeling ?: "") }
    var feelingsExpanded by remember { mutableStateOf(false) }


    // 👉 EXPAND SHEET ON OPEN
    LaunchedEffect(Unit) {
        sheetState.show()
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = DeepNavyCharcoal.copy(alpha = 0.92f),
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
    ) {
        SheetContent(
            delta = delta,
            onDeltaChange = { delta = it },

            title = title,
            onTitleChange = { title = it },

            feeling = feeling,
            onFeelingChange = { feeling = it },
            feelingOptions = feelingOptions,
            feelingsExpanded = feelingsExpanded,
            onFeelingsExpandedChange = { feelingsExpanded = it },

            tags = tags,
            onTagsChange = { tags = it },

            tagsText = tagsText,
            onTagsTextChange = { tagsText = it },

            note = note,
            onNoteChange = { note = it },

            pastTags = pastTags,

            onSave = {
                onSave(
                    LedgerEntry(
                        id = existingEntry?.id ?: UUID.randomUUID().mostSignificantBits,
                        timestamp = existingEntry?.timestamp ?: System.currentTimeMillis(),
                        delta = delta,
                        tags = tags,
                        note = note,
                        title = title,
                        feeling = feeling
                    )
                )
            }
        )

    }





}

@Composable
fun TagChip(label: String, onRemove: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .background(
                color = LedgerTeal.copy(alpha = 0.18f),
                shape = RoundedCornerShape(50)
            )
            .padding(horizontal = 12.dp, vertical = 6.dp)
    ) {
        Text(
            text = label,
            color = Color.White,
            style = MaterialTheme.typography.bodySmall
        )
        Spacer(Modifier.width(8.dp))
        IconButton(
            onClick = onRemove,
            modifier = Modifier.size(18.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Close,
                contentDescription = "Remove tag",
                tint = LedgerTeal
            )
        }
    }
}
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TagInputField(
    tags: List<String>,
    onTagsChange: (List<String>) -> Unit,
    text: String,
    onTextChange: (String) -> Unit
) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(1.dp, LedgerTeal),
        color = Color.Transparent,
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 4.dp)
    ) {

        Row(
            modifier = Modifier
                .padding(8.dp)
                .wrapContentHeight()
                .fillMaxWidth()
                .background(Color.Transparent)
        ) {

            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.weight(1f)
            ) {

                // Existing tag chips
                tags.forEach { tag ->
                    TagChip(
                        label = tag,
                        onRemove = { onTagsChange(tags - tag) }
                    )
                }

                // The actual tag text input field
                BasicTextField(
                    value = text,
                    onValueChange = { input ->
                        when {
                            input.endsWith(",") -> {
                                val newTag = input.dropLast(1).trim()
                                if (newTag.isNotEmpty()) onTagsChange(tags + newTag)
                                onTextChange("")
                            }

                            else -> onTextChange(input)
                        }
                    },
                    singleLine = true,
                    cursorBrush = SolidColor(LedgerTeal),
                    textStyle = LocalTextStyle.current.copy(color = Color.White),
                    keyboardOptions = KeyboardOptions.Default.copy(
                        imeAction = ImeAction.Done
                    ),
                    keyboardActions = KeyboardActions(
                        onDone = {
                            val newTag = text.trim()
                            if (newTag.isNotEmpty()) onTagsChange(tags + newTag)
                            onTextChange("")
                        }
                    ),
                    modifier = Modifier
                        .padding(start = 4.dp)
                        .weight(1f, false)
                )
            }
        }
    }
}
    @Composable
    fun AutocompleteTagSuggestions(
        query: String,
        selectedTags: List<String>,
        pastTags: List<String>,
        onTagSelected: (String) -> Unit
    ) {
        if (query.isBlank()) return

        val filtered = pastTags
            .filter { t ->
                t.contains(query, ignoreCase = true) &&
                        t !in selectedTags
            }
            .take(5) // limit suggestions

        if (filtered.isEmpty()) return

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 4.dp, top = 4.dp)
                .background(
                    color = Color(0xFF0F1A24),
                    shape = RoundedCornerShape(8.dp)
                )
                .border(
                    width = 1.dp,
                    color = LedgerTeal.copy(alpha = 0.4f),
                    shape = RoundedCornerShape(8.dp)
                )
        ) {
            filtered.forEach { tag ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onTagSelected(tag) }
                        .padding(10.dp)
                ) {
                    Text(tag, color = Color.White, fontSize = 14.sp)
                }
            }
        }
    }

@Composable
fun SheetContent(
    delta: Float,
    onDeltaChange: (Float) -> Unit,

    title: String,
    onTitleChange: (String) -> Unit,

    feeling: String,
    onFeelingChange: (String) -> Unit,
    feelingOptions: List<String>,
    feelingsExpanded: Boolean,
    onFeelingsExpandedChange: (Boolean) -> Unit,

    tags: List<String>,
    onTagsChange: (List<String>) -> Unit,

    tagsText: String,
    onTagsTextChange: (String) -> Unit,

    pastTags: List<String>,

    note: String,
    onNoteChange: (String) -> Unit,

    onSave: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .navigationBarsPadding()
            .imePadding()
            .background(
                Brush.verticalGradient(
                    listOf(
                        DeepNavyCharcoal,
                        Color(0xFF0C141A)
                    )
                )
            )
            .padding(22.dp)
    ) {

        // --- HEADER ---
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = Icons.Default.MenuBook,
                contentDescription = null,
                tint = LedgerGold,
                modifier = Modifier.size(26.dp)
            )
            Spacer(Modifier.width(12.dp))
            Text(
                text = "New Entry",
                style = MaterialTheme.typography.headlineSmall.copy(
                    color = Color.White,
                    fontSize = 22.sp
                )
            )
        }

        Spacer(Modifier.height(28.dp))

        // --- DELTA ---
        Text("Adjustment (Delta)", color = Color(0xFFAEC7D2), fontSize = 14.sp)
        Spacer(Modifier.height(6.dp))
        Text(String.format("%.2f", delta), color = Color.White, fontSize = 18.sp)

        Slider(
            value = delta,
            onValueChange = { raw -> onDeltaChange(snapToQuarter(raw)) },
            valueRange = -2f..2f,
            steps = 0,
            colors = SliderDefaults.colors(
                thumbColor = LedgerTeal,
                activeTrackColor = LedgerTeal,
                inactiveTrackColor = Color(0xFF1B3845)
            )
        )

        Spacer(Modifier.height(24.dp))

        // --- TITLE ---
        Text("Title", color = Color(0xFFAEC7D2), fontSize = 14.sp)
        OutlinedTextField(
            value = title,
            onValueChange = onTitleChange,
            singleLine = true,
            placeholder = { Text("e.g. David’s Phone Call") },
            textStyle = LocalTextStyle.current.copy(color = Color.White),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = LedgerTeal,
                unfocusedBorderColor = Color(0xFF31505C),
                cursorColor = LedgerTeal
            ),
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(Modifier.height(24.dp))

        // --- FEELING / CATEGORY ---
        Text("Feeling", color = Color(0xFFAEC7D2), fontSize = 14.sp)

        Box(modifier = Modifier.fillMaxWidth()) {

            OutlinedTextField(
                value = feeling,
                onValueChange = {},
                readOnly = true,
                singleLine = true,
                trailingIcon = {
                    Icon(
                        imageVector = if (feelingsExpanded)
                            Icons.Default.KeyboardArrowUp
                        else Icons.Default.KeyboardArrowDown,
                        contentDescription = null,
                        tint = LedgerTeal
                    )
                },
                placeholder = { Text("Select a feeling…") },
                textStyle = LocalTextStyle.current.copy(color = Color.White),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = LedgerTeal,
                    unfocusedBorderColor = Color(0xFF31505C),
                    cursorColor = LedgerTeal
                ),
                modifier = Modifier.fillMaxWidth()
            )

            // Transparent tap layer
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .clickable { onFeelingsExpandedChange(!feelingsExpanded) }
            )
        }

        DropdownMenu(
            expanded = feelingsExpanded,
            onDismissRequest = { onFeelingsExpandedChange(false) }
        ) {
            feelingOptions.forEach { option ->
                DropdownMenuItem(
                    text = { Text(option) },
                    onClick = {
                        onFeelingChange(option)
                        onFeelingsExpandedChange(false)
                    }
                )
            }
        }

        Spacer(Modifier.height(24.dp))

        // --- TAGS ---
        Text("Tags", color = Color(0xFFAEC7D2), fontSize = 14.sp)

        TagInputField(
            tags = tags,
            onTagsChange = onTagsChange,
            text = tagsText,
            onTextChange = onTagsTextChange
        )

        AutocompleteTagSuggestions(
            query = tagsText,
            selectedTags = tags,
            pastTags = pastTags,
            onTagSelected = { tag ->
                onTagsChange(tags + tag)
                onTagsTextChange("")
            }
        )

        Spacer(Modifier.height(24.dp))


        // --- NOTES ---
        Text("Notes", color = Color(0xFFAEC7D2), fontSize = 14.sp)
        OutlinedTextField(
            value = note,
            onValueChange = onNoteChange,
            minLines = 4,
            maxLines = 8,
            placeholder = { Text("Describe what happened…") },
            textStyle = LocalTextStyle.current.copy(color = Color.White),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = LedgerTeal,
                unfocusedBorderColor = Color(0xFF31505C),
                cursorColor = LedgerTeal
            ),
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 120.dp)
        )

        Spacer(Modifier.height(30.dp))

        Button(
            onClick = onSave,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            shape = RoundedCornerShape(18.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = LedgerTeal,
                contentColor = Color.Black
            )
        ) {
            Text("Save Entry", fontSize = 17.sp)
        }

        Spacer(Modifier.height(14.dp))
    }
}