package com.example.mood.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

import com.example.mood.ui.theme.DeepNavyCharcoal
import com.example.mood.ui.theme.LedgerGold
import androidx.compose.foundation.shape.RoundedCornerShape


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DailyReflectionSheet(
    currentComputedScore: Float,
    initialText: String,
    initialScore: Float?,
    onSave: (reflectionText: String, reflectionScore: Float) -> Unit,
    onDismiss: () -> Unit
)
{
    var reflectionText by remember(initialText) {
        mutableStateOf(initialText)
    }

    var reflectionScore by remember(initialScore, currentComputedScore) {
        mutableStateOf(initialScore ?: currentComputedScore)
    }


    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = DeepNavyCharcoal
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp)
                .verticalScroll(rememberScrollState())
        ) {

            Text(
                "Daily Reflection",
                style = MaterialTheme.typography.headlineSmall,
                color = Color.White
            )

            Spacer(Modifier.height(16.dp))

            OutlinedTextField(
                value = reflectionText,
                onValueChange = { reflectionText = it },
                minLines = 6,
                maxLines = 12,
                placeholder = { Text("Looking back on today, how did it really feel?") },
                textStyle = LocalTextStyle.current.copy(color = Color.White),
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 180.dp)
            )

            Spacer(Modifier.height(20.dp))

            Text("Overall Day Score", color = Color(0xFFAEC7D2))

            Text(
                "%.2f / 10.00".format(reflectionScore),
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White
            )

            Slider(
                value = reflectionScore,
                onValueChange = { reflectionScore = it },
                valueRange = 0f..10f,
                steps = 39, // quarter points
                colors = SliderDefaults.colors(
                    thumbColor = LedgerGold,
                    activeTrackColor = LedgerGold
                )
            )

            Spacer(Modifier.height(24.dp))

            Button(
                onClick = { onSave(reflectionText, reflectionScore) },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp)
            ) {
                Text("Save Reflection")
            }
        }
    }

}
