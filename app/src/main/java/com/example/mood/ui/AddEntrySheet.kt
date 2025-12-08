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




@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEntrySheet(
    onSave: (LedgerEntry) -> Unit,
    onDismiss: () -> Unit
) {
    var delta by remember { mutableStateOf(0f) }
    var tagsText by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = DeepNavyCharcoal.copy(alpha = 0.92f),
        tonalElevation = 0.dp,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
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

            // HEADER
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

            // DELTA SLIDER
            Text(
                "Adjustment (Delta)",
                color = Color(0xFFAEC7D2),
                fontSize = 14.sp
            )

            Spacer(Modifier.height(6.dp))

            Text(
                text = String.format("%.2f", delta),
                color = Color.White,
                fontSize = 18.sp
            )

            Slider(
                value = delta,
                onValueChange = { delta = it },
                valueRange = -5f..5f,
                steps = 40,
                colors = SliderDefaults.colors(
                    thumbColor = LedgerTeal,
                    activeTrackColor = LedgerTeal,
                    inactiveTrackColor = Color(0xFF1B3845)
                )
            )

            Spacer(Modifier.height(24.dp))

            // TAG INPUT
            Text("Tags", color = Color(0xFFAEC7D2), fontSize = 14.sp)

            OutlinedTextField(
                value = tagsText,
                onValueChange = { tagsText = it },
                placeholder = { Text("e.g. dream, David, phone call…") },
                textStyle = LocalTextStyle.current.copy(color = Color.White),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = LedgerTeal,
                    unfocusedBorderColor = Color(0xFF31505C),
                    cursorColor = LedgerTeal
                ),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(24.dp))

            // NOTES
            Text("Notes", color = Color(0xFFAEC7D2), fontSize = 14.sp)

            OutlinedTextField(
                value = note,
                onValueChange = { note = it },
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

            // SAVE BUTTON
            Button(
                onClick = {
                    val tags = tagsText.split(",").map { it.trim() }.filter { it.isNotEmpty() }

                    val entry = LedgerEntry(
                        id = UUID.randomUUID().mostSignificantBits,
                        timestamp = System.currentTimeMillis(),
                        delta = delta,
                        tags = tags,
                        note = note
                    )
                    onSave(entry)
                },
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
}
