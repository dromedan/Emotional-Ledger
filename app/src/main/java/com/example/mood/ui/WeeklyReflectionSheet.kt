package com.example.mood.ui


// ─────────────────────────────────────────────
// Compose runtime
// ─────────────────────────────────────────────
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue

// ─────────────────────────────────────────────
// Compose UI / layout
// ─────────────────────────────────────────────
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

// ─────────────────────────────────────────────
// Material 3 experimental
// ─────────────────────────────────────────────
import androidx.compose.material3.ExperimentalMaterial3Api


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WeeklyReflectionSheet(
    title: String,
    initialText: String,
    onSave: (String) -> Unit,
    onDismiss: () -> Unit
)
 {
    var text by remember { mutableStateOf(initialText) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(
            skipPartiallyExpanded = true
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                title,
                style = MaterialTheme.typography.headlineSmall
            )

            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                placeholder = {
                    Text("What stood out this week?")
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(160.dp)
            )

            Button(
                onClick = { onSave(text) },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Save Reflection")
            }
        }
    }
}
