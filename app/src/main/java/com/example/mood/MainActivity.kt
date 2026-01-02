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
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.interaction.MutableInteractionSource
import java.time.DayOfWeek
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import android.content.Intent
import androidx.compose.material.icons.filled.Download
import android.content.Context
import android.net.Uri
import android.provider.DocumentsContract
import java.time.temporal.TemporalAdjusters
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.withFrameMillis
import androidx.compose.runtime.withFrameNanos
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import com.example.mood.data.loadBallLayout
import com.example.mood.data.saveBallLayout
import androidx.compose.ui.graphics.Brush
import android.os.Vibrator
import android.os.VibrationEffect
import android.os.Build




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

private fun impactOrderIndices(
    entries: List<LedgerEntry>
): List<Int> {
    return entries
        .mapIndexed { index, entry -> index to entry.delta }
        .sortedWith(
            compareByDescending<Pair<Int, Float>> { it.second }
        )
        .map { it.first }
}


private fun angularDistance(a: Float, b: Float): Float {
    val diff = kotlin.math.abs(a - b) % 360f
    return if (diff > 180f) 360f - diff else diff
}

private fun normalizeAngle(a: Float): Float =
    ((a % 360f) + 360f) % 360f


private fun DrawScope.drawInfluenceBands(
    entries: List<LedgerEntry>
) {
    if (entries.isEmpty()) return

    val radius = size.minDimension / 2.15f
    val baseThickness = 10.dp.toPx()
    val maxExtraThickness = 14.dp.toPx()

    val totalAbs =
        entries.sumOf { kotlin.math.abs(it.delta).toDouble() }.toFloat()
            .coerceAtLeast(0.01f)

    var startAngle = -160f
    val availableSweep = 320f

    entries.forEach { entry ->
        val weight =
            kotlin.math.abs(entry.delta) / totalAbs

        val sweep = availableSweep * weight

        val thickness =
            baseThickness +
                    (kotlin.math.abs(entry.delta).coerceIn(0f, 2f) / 2f) *
                    maxExtraThickness

        val color =
            if (entry.delta >= 0f)
                Color(0xFF66BB6A)
            else
                Color(0xFFE57373)

        drawArc(
            color = color,
            startAngle = startAngle,
            sweepAngle = sweep,
            useCenter = false,
            topLeft = Offset(
                center.x - radius,
                center.y - radius
            ),
            size = Size(
                radius * 2,
                radius * 2
            ),
            style = Stroke(
                width = thickness,
                cap = StrokeCap.Round
            )
        )

        startAngle += sweep
    }

    // Subtle outer guide ring
    drawCircle(
        color = Color.White.copy(alpha = 0.12f),
        radius = radius + baseThickness,
        style = Stroke(width = 2.dp.toPx())
    )
}





@Composable
fun DailyMoodSeal(
    mood: Float,
    baseline: Float,
    drift: Float?,
    entries: List<LedgerEntry>,
    dayKey: String,
    isObsidianConnected: Boolean,
    onCenterTap: () -> Unit,
    onExportTap: () -> Unit,
    modifier: Modifier = Modifier
)




 {

     // --- Ball drag state ---
     val context = LocalContext.current


     val ballAngles = remember(entries) {
         mutableStateListOf<Float>()
     }

     val ballVelocities = remember(entries) {
         mutableStateListOf<Float>()
     }

     val vibrator =
         remember {
             context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
         }

     var lastHapticTime by remember { mutableStateOf(0L) }


     LaunchedEffect(entries, dayKey) {
         ballAngles.clear()

         val saved =
             loadBallLayout(context, dayKey)

         if (saved != null && saved.size == entries.size) {
             ballAngles.addAll(saved)
         } else {
             val step =
                 if (entries.isNotEmpty()) 360f / entries.size else 0f

             val orderedIndices = impactOrderIndices(entries)

             orderedIndices.forEachIndexed { visualIndex, entryIndex ->
                 ballAngles.add(-90f + visualIndex * step)
             }
         }


         // 🔒 KEEP velocities aligned with balls
         ballVelocities.clear()
         repeat(ballAngles.size) {
             ballVelocities.add(0f)
         }
     }

     val ballRadiusDeg = 10f
     val minSeparationDeg = ballRadiusDeg * 2f



     var activeBallIndex by remember { mutableStateOf<Int?>(null) }


     var lastDragAngle by remember { mutableStateOf<Float?>(null) }
     var lastDragTime by remember { mutableStateOf<Long?>(null) }
     var lastFrameTimeNanos by remember { mutableStateOf<Long?>(null) }
     var peakVelocity by remember { mutableStateOf(0f) }



     Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(entries) {
                    detectDragGestures(
                        onDragStart = { offset ->
                            val center = Offset(size.width / 2f, size.height / 2f)
                            val dx = offset.x - center.x
                            val dy = offset.y - center.y

                            val angle =
                                ((Math.toDegrees(kotlin.math.atan2(dy, dx).toDouble())).toFloat() + 360f) % 360f

                            var closestIndex: Int? = null
                            var closestDistance = Float.MAX_VALUE

                            for (i in ballAngles.indices) {
                                val dist = angularDistance(ballAngles[i], angle)
                                if (dist < closestDistance) {
                                    closestDistance = dist
                                    closestIndex = i
                                }
                            }

                            activeBallIndex =
                                if (closestDistance < 24f) closestIndex else null

                            lastDragAngle = angle
                            lastDragTime = System.currentTimeMillis()
                        }

                        ,
                        onDrag = { change, _ ->
                            activeBallIndex?.let { index ->
                                val center = Offset(size.width / 2f, size.height / 2f)
                                val dx = change.position.x - center.x
                                val dy = change.position.y - center.y

                                val angle =
                                    ((Math.toDegrees(kotlin.math.atan2(dy, dx).toDouble())).toFloat() + 360f) % 360f

                                val now = System.currentTimeMillis()

                                val lastAngle = lastDragAngle
                                val lastTime = lastDragTime

                                if (lastAngle != null && lastTime != null) {
                                    val dt = (now - lastTime).coerceAtLeast(1)
                                    val da = angularDistance(angle, lastAngle)

                                    // direction-aware velocity
                                    val direction =
                                        if (((angle - lastAngle + 540f) % 360f) - 180f > 0) 1f else -1f

                                    // velocity in degrees per SECOND
                                    val velocity =
                                        direction * (da / dt) * 500f

                                    // Track the strongest recent velocity
                                    if (kotlin.math.abs(velocity) > kotlin.math.abs(peakVelocity)) {
                                        peakVelocity = velocity.coerceIn(-720f, 720f)
                                    }


                                }

                                ballAngles[index] = angle
                                lastDragAngle = angle
                                lastDragTime = now
                            }
                        }
                        ,
                        onDragEnd = {
                            activeBallIndex?.let { index ->
                                ballVelocities[index] = peakVelocity
                            }

                            // Persist layout
                            CoroutineScope(Dispatchers.IO).launch {
                                saveBallLayout(
                                    context,
                                    dayKey,
                                    ballAngles.toList()
                                )

                            }

                            peakVelocity = 0f
                            activeBallIndex = null
                            lastDragAngle = null
                            lastDragTime = null
                        }


                        ,
                        onDragCancel = {
                            activeBallIndex = null
                        }
                    )
                }


        )
        {

            drawTealHalo()
            drawEntryDots(entries, ballAngles)




        }




        // Center number (keep Compose Text – very important)
         Column(
             horizontalAlignment = Alignment.CenterHorizontally,
             verticalArrangement = Arrangement.Center,
             modifier = Modifier.clickable(
                 indication = null,
                 interactionSource = remember { MutableInteractionSource() }
             ) {
                 onCenterTap(/* dayKey will be provided by caller */)
             }
         ) {


         Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = String.format("%.2f", mood),
                style = MaterialTheme.typography.displaySmall.copy(
                    fontWeight = FontWeight.Medium
                ),
                color =
                    if (mood >= baseline)
                        Color(0xFF4CAF50)   // green
                    else
                        Color(0xFFE57373)   // red

            )

            Spacer(modifier = Modifier.height(2.dp))

             Text(
                 text = "Mood Score",
                 style = MaterialTheme.typography.labelSmall.copy(
                     fontSize = 11.sp,
                     letterSpacing = 0.6.sp
                 ),
                 color = Color.White.copy(alpha = 0.65f)
             )

             // --- Fixed slot for export button (prevents vertical shifting) ---
             // Reflection text (may appear or not — does NOT affect icon position)
             if (drift != null && drift != 0f) {
                 Spacer(modifier = Modifier.height(4.dp))

                 Text(
                     text = when {
                         drift > 0f -> "▲ %.2f reflection".format(drift)
                         else -> "▼ %.2f reflection".format(kotlin.math.abs(drift))
                     },
                     style = MaterialTheme.typography.labelSmall.copy(
                         fontSize = 11.sp,
                         letterSpacing = 0.4.sp
                     ),
                     color = deltaColor(drift).copy(alpha = 0.75f)
                 )
             }

// --- Fixed slot for export button (ALWAYS same position) ---
             Box(
                 modifier = Modifier
                     .height(36.dp)   // reserved space
                     .padding(top = 4.dp),
                 contentAlignment = Alignment.Center
             ) {
                 if (isObsidianConnected) {
                     IconButton(
                         onClick = onExportTap,
                         modifier = Modifier.size(28.dp)
                     ) {
                         Icon(
                             imageVector = Icons.Default.Download,
                             contentDescription = "Export to Obsidian",
                             tint = Color.White.copy(alpha = 0.75f),
                             modifier = Modifier.size(18.dp)
                         )
                     }
                 }
             }


         }

    }
     LaunchedEffect(entries.size) {
         while (true) {
             withFrameNanos { now ->

                 val last = lastFrameTimeNanos
                 lastFrameTimeNanos = now
                 if (last == null) return@withFrameNanos

                 val deltaSeconds = (now - last) / 1_000_000_000f

                 // --- 1️⃣ Integrate motion ---
                 ballAngles.indices.forEach { i ->
                     ballAngles[i] =
                         normalizeAngle(
                             ballAngles[i] + ballVelocities[i] * deltaSeconds
                         )
                 }

                 // --- 2️⃣ Resolve collisions (solid beads) ---
                 for (i in 0 until ballAngles.size) {
                     for (j in i + 1 until ballAngles.size) {
                         val a = ballAngles[i]
                         val b = ballAngles[j]

                         val dist = angularDistance(a, b)


                         if (dist < minSeparationDeg) {

                             val overlap = minSeparationDeg - dist

                             val direction =
                                 if (((b - a + 540f) % 360f) - 180f > 0) 1f else -1f

                             // Push apart
                             ballAngles[i] =
                                 normalizeAngle(ballAngles[i] - direction * overlap / 2f)
                             ballAngles[j] =
                                 normalizeAngle(ballAngles[j] + direction * overlap / 2f)

                             // Momentum exchange
                             val vi = ballVelocities[i]
                             val vj = ballVelocities[j]

                             ballVelocities[i] = vj * 0.9f
                             ballVelocities[j] = vi * 0.9f

                             // --- HAPTICS ---
                             val relativeVelocity = kotlin.math.abs(vi - vj)
                             val normalizedImpact =
                                 (relativeVelocity / 720f).coerceIn(0f, 1f)

                             val nowMs = System.currentTimeMillis()
                             if (
                                 normalizedImpact > 0.15f &&
                                 nowMs - lastHapticTime > 40
                             ) {
                                 vibrateImpact(vibrator, normalizedImpact)
                                 lastHapticTime = nowMs
                             }

                         }



                     }
                 }

                 // --- 3️⃣ Friction ---
                 ballVelocities.indices.forEach { i ->
                     ballVelocities[i] *= 0.97f
                     if (kotlin.math.abs(ballVelocities[i]) < 0.05f) {
                         ballVelocities[i] = 0f
                     }
                 }
             }
         }
     }




 }
fun vibrateImpact(
    vibrator: Vibrator,
    strength: Float
) {
    if (!vibrator.hasVibrator()) return

    val clamped = strength.coerceIn(0f, 1f)

    val durationMs = (8 + clamped * 20).toLong()
    val amplitude  = (40 + clamped * 180).toInt()

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        vibrator.vibrate(
            VibrationEffect.createOneShot(durationMs, amplitude)
        )
    } else {
        @Suppress("DEPRECATION")
        vibrator.vibrate(durationMs)
    }
}



private fun DrawScope.drawMoodSeal(
    tags: List<String>,
    tagStats: Map<String, TagStats>,
    rotationDeg: Float,
    touchAngle: Float?,
    activeTagIndex: Int?,
    onActiveTagChange: (Int?) -> Unit
)

 {

    drawIntoCanvas { canvas ->
        val paint = android.graphics.Paint().apply {
            isAntiAlias = true
            textSize = 14.sp.toPx()
            alpha = 180
        }
    val fontMetrics = paint.fontMetrics
    val textOffset = -fontMetrics.ascent / 2f
        val tagPaddingDeg = 6f   // fixed angular padding per tag
        val minSweepDeg = 14f    // minimum arc span so short words don’t crowd


        val radius = size.minDimension / 2.2f
    val strokeWidth = 3.dp.toPx()
    val textRadius = radius








    val textHeight = fontMetrics.descent - fontMetrics.ascent
    val bandThickness = textHeight + 10.dp.toPx()

// Move arc inward so it surrounds text
    val arcRadius = textRadius - bandThickness / 2f
    // Outer circle


        if (tags.isEmpty()) return


        val baseStartAngle = -160f + rotationDeg
        val path = android.graphics.Path().apply {
            addArc(
                center.x - textRadius,
                center.y - textRadius,
                center.x + textRadius,
                center.y + textRadius,

                baseStartAngle,
                320f
            )
        }


        var horizontalOffset = 0f




        var arcOffset = 0f

// Normalize base angle to 0–360 for hit testing
        val normalizedBaseStart =
            ((-160f + rotationDeg) % 360f + 360f) % 360f



        val separator = " · "

// ---- PRECOMPUTE TAG SWEEPS (for normalization) ----
        val tagSweeps = mutableListOf<Float>()

        tags.forEach { tag ->
            val textWidth = paint.measureText(tag)
            val rawSweep =
                textWidth / (2 * Math.PI.toFloat() * arcRadius) * 360f

            val sweep =
                maxOf(rawSweep, minSweepDeg) + tagPaddingDeg

            tagSweeps += sweep
        }

        val totalSweep = tagSweeps.sum()
        val maxSweep = 282f

// Scale down uniformly if we overflow the available arc
        val sweepScale =
            if (totalSweep > maxSweep)
                maxSweep / totalSweep
            else
                1f


        tags.forEachIndexed { index, tag ->
            val avg = tagStats[tag]?.average ?: 0f
            val color = tagImpactColor(avg)

            // Measure text width
            val textWidth = paint.measureText(tag)
            val separatorWidth = paint.measureText(separator)

            // Convert text width → sweep angle
            val sweep = tagSweeps[index] * sweepScale

            val midAngle =
                (baseStartAngle + arcOffset + sweep / 2f + 360f) % 360f

            val isUpsideDown = midAngle in 90f..270f




// --- HIT TEST FOR ACTIVE TAG ---
            val startAngle =
                (normalizedBaseStart + arcOffset) % 360f
            val endAngle =
                (startAngle + sweep) % 360f

            val isActive =
                touchAngle != null &&
                        if (startAngle <= endAngle)
                            touchAngle in startAngle..endAngle
                        else
                            touchAngle >= startAngle || touchAngle <= endAngle

            if (isActive) {
                onActiveTagChange(index)
            }



            // Draw colored arc segment (background)
            val outlineWidth = bandThickness + 2.dp.toPx()

// ✨ Glow for active tag (drawn first)
            if (index == activeTagIndex) {
                drawArc(
                    color = Color.White.copy(alpha = 0.28f),
                    startAngle = baseStartAngle + arcOffset,
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
                        width = bandThickness + 12.dp.toPx(),
                        cap = StrokeCap.Round
                    )
                )
            }

// 1️⃣ Black outline (drawn first)
            drawArc(
                color = Color.Black,
                startAngle = baseStartAngle + arcOffset,
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
                    width = outlineWidth,
                    cap = StrokeCap.Round
                )
            )

// 2️⃣ Colored pill (drawn on top)
            drawArc(
                color = Color(color),
                startAngle = baseStartAngle + arcOffset,

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
                arcOffset * radius * Math.PI.toFloat() / 180f
                ,
                textOffset,
                paint
            )

            arcOffset += sweep

            // Separator (no colored band)
            if (index < tags.lastIndex) {
                canvas.nativeCanvas.drawTextOnPath(
                    separator,
                    path,
                    arcOffset * radius * Math.PI.toFloat() / 180f
                    ,
                    textOffset,
                    paint
                )
                arcOffset +=
                    separatorWidth / (2 * Math.PI.toFloat() * radius) * 360f * 0.6f

            }
        }

    }

}

private fun DrawScope.drawTealHalo() {

    val ringRadius = size.minDimension / 2.15f
    val ringThickness = 26.dp.toPx()

    // Single, consistent light model
    val tubeGradient = Brush.linearGradient(
        colors = listOf(
            LedgerTeal.copy(alpha = 0.95f),   // light edge (top-left)
            LedgerTeal.copy(alpha = 0.75f),
            LedgerTeal.copy(alpha = 0.55f),
            LedgerTeal.copy(alpha = 0.80f)    // shadow edge (bottom-right)
        ),
        start = Offset(
            center.x - ringRadius,
            center.y - ringRadius
        ),
        end = Offset(
            center.x + ringRadius,
            center.y + ringRadius
        )
    )

    drawCircle(
        brush = tubeGradient,
        radius = ringRadius,
        style = Stroke(
            width = ringThickness,
            cap = StrokeCap.Round
        )
    )
}



private fun DrawScope.drawEntryDots(
    entries: List<LedgerEntry>,
    angles: List<Float>
) {
    if (entries.isEmpty()) return
    if (angles.size < entries.size) return


    val ringRadius = size.minDimension / 2.15f
    val ringThickness = 26.dp.toPx()

    val tubeCenterRadius = ringRadius
    val dotRadius = 9.dp.toPx()

    entries.forEachIndexed { index, entry ->
        val angleDeg = angles[index]
        val angleRad = Math.toRadians(angleDeg.toDouble())

        val x =
            center.x + kotlin.math.cos(angleRad).toFloat() * tubeCenterRadius
        val y =
            center.y + kotlin.math.sin(angleRad).toFloat() * tubeCenterRadius

        val baseColor = deltaColor(entry.delta)

        // 1️⃣ Soft shadow (lifts marble off tube)
        drawCircle(
            color = Color.Black.copy(alpha = 0.28f),
            radius = dotRadius * 1.05f,
            center = Offset(
                x + 2.dp.toPx(),
                y + 2.dp.toPx()
            )
        )

        // 2️⃣ Glassy marble body (radial gradient)
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    baseColor.copy(alpha = 0.95f),
                    baseColor.copy(alpha = 0.75f),
                    Color.Black.copy(alpha = 0.25f)
                ),
                center = Offset(
                    x - dotRadius * 0.35f,
                    y - dotRadius * 0.35f
                ),
                radius = dotRadius * 1.3f
            ),
            radius = dotRadius,
            center = Offset(x, y)
        )

        // 3️⃣ Specular highlight (glass reflection)
        drawCircle(
            color = Color.White.copy(alpha = 0.35f),
            radius = dotRadius * 0.35f,
            center = Offset(
                x - dotRadius * 0.4f,
                y - dotRadius * 0.4f
            )
        )
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
        "frustrated"  -> "😖"
        "anxious"     -> "😰"
        "overwhelmed" -> "😵‍💫"
        "sad"         -> "😞"
        "angry"       -> "😠"
        "embarrassed" -> "😳"

        else -> "•"   // neutral fallback
    }

fun deltaColor(delta: Float): Color {
    return when {
        delta == 0f ->
            Color.White

        delta > 0f && delta < 0.75f ->
            Color(0xFFA5D6A7) // soft green

        delta >= 0.75f && delta < 1.25f ->
            Color(0xFF66BB6A) // medium green

        delta >= 1.25f ->
            Color(0xFF388E3C) // strong green

        delta < 0f && delta > -0.75f ->
            Color(0xFFE5A0A0) // soft red

        delta <= -0.75f && delta > -1.25f ->
            Color(0xFFE57373) // medium red

        else ->
            Color(0xFFB73F3F) // strong red
    }
}

fun tagImpactColor(avg: Float): Int {
    return when {
        avg == 0f ->
            android.graphics.Color.WHITE

        avg > 0f && avg < 0.75f ->
            android.graphics.Color.argb(240, 165, 214, 167) // soft green

        avg >= 0.75f && avg < 1.25f ->
            android.graphics.Color.argb(245, 102, 187, 106) // medium green

        avg >= 1.25f ->
            android.graphics.Color.argb(255, 56, 142, 60)   // strong green

        avg < 0f && avg > -0.75f ->
            android.graphics.Color.argb(240, 229, 157, 157) // soft red

        avg <= -0.75f && avg > -1.25f ->
            android.graphics.Color.argb(245, 229, 115, 115) // medium red

        else ->
            android.graphics.Color.argb(255, 183, 63, 63)   // strong red
    }
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
    var showMenuSheet by remember { mutableStateOf(false) }
    var entryBeingEdited by remember { mutableStateOf<LedgerEntry?>(null) }
    var activeDayKey by remember { mutableStateOf(todayKey()) }
    val baseline = 5.00f
    var reflectionText by remember(activeDayKey) {
        mutableStateOf("")
    }
    var trendsWeekStart by remember { mutableStateOf<LocalDate?>(null) }

    var reflectionScore by remember(activeDayKey) {
        mutableStateOf<Float?>(null)
    }
    val activeDate =
        LocalDate.parse(activeDayKey)

    val activeWeekStart =
        activeDate.with(TemporalAdjusters.previousOrSame(DayOfWeek.SUNDAY))


    var showTrends by remember { mutableStateOf(false) }

    if (showTrends && trendsWeekStart != null) {
        MoodTrendsScreen(
            initialWeekStart = trendsWeekStart!!,
            onBack = {
                showTrends = false
                trendsWeekStart = null
            },
            onSelectDay = { selectedDate ->
                activeDayKey = selectedDate.toString()
                showTrends = false
                trendsWeekStart = null
            }
        )
        return
    }


    var isObsidianConnected by remember { mutableStateOf(false) }
    var obsidianFolderUri by remember { mutableStateOf<android.net.Uri?>(null) }
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

    val obsidianFolderLauncher =
        rememberLauncherForActivityResult(
            contract = ActivityResultContracts.OpenDocumentTree()
        ) { uri ->
            if (uri != null) {
                // Persist permission
                context.contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION or
                            Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                )

                android.util.Log.d(
                    "Obsidian",
                    "Selected folder URI: $uri"
                )

                obsidianFolderUri = uri
                isObsidianConnected = true   // ✅ IMMEDIATE UI UPDATE

                coroutineScope.launch {
                    com.example.mood.data.ObsidianStore.saveFolder(context, uri)
                }



            }
        }

    LaunchedEffect(Unit) {
        val savedUri =
            com.example.mood.data.ObsidianStore.loadFolder(context)

        if (savedUri != null) {
            val hasPermission =
                context.contentResolver.persistedUriPermissions.any { perm ->
                    perm.uri == savedUri && perm.isReadPermission && perm.isWritePermission
                }

            if (hasPermission) {
                obsidianFolderUri = savedUri
                isObsidianConnected = true
            } else {
                // Permission genuinely lost (user revoked or storage reset)
                com.example.mood.data.ObsidianStore.clear(context)
                obsidianFolderUri = null
                isObsidianConnected = false
            }
        }
    }




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
        val saved = LedgerStore.loadDailyReflection(context, activeDayKey)

        if (saved != null) {
            reflectionText = saved.note
            reflectionScore =
                baseline +
                        entries.fold(0f) { acc, e -> acc + e.delta } +
                        saved.drift
        } else {
            // ✅ reset for days with no reflection
            reflectionText = ""
            reflectionScore = null
        }
    }


    Box(
        modifier = Modifier
            .fillMaxSize()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(
                    start = 24.dp,
                    end = 24.dp,
                    top = 24.dp,
                    bottom = 356.dp
                )
            ,
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

                // 📅 Calendar
                IconButton(onClick = { showCalendar = true }) {
                    Icon(
                        imageVector = Icons.Default.CalendarMonth,
                        contentDescription = "Open calendar",
                        tint = Color.White
                    )
                }

                // ☰ Menu
                IconButton(onClick = { showMenuSheet = true }) {
                    Icon(
                        imageVector = Icons.Default.Menu,
                        contentDescription = "Open menu",
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
                    .height(400.dp) // ⬅ Box owns the height
                    .background(
                        color = Color.White.copy(alpha = 0.04f),
                        shape = RoundedCornerShape(16.dp)
                    )
                    .padding(horizontal = 12.dp, vertical = 8.dp)
            ) {

                if (entries.isEmpty()) {
                    // ✅ EMPTY STATE (static, centered)
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "No entries yet today",
                            style = MaterialTheme.typography.bodyLarge.copy(
                                fontWeight = FontWeight.Medium
                            ),
                            color = Color.White.copy(alpha = 0.75f)
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = "Add moments that influenced your mood.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.White.copy(alpha = 0.45f),
                            textAlign = TextAlign.Center
                        )
                    }

                } else {
                    // ✅ ENTRIES LIST (must be a Column!)
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .verticalScroll(rememberScrollState())
                            .clip(RoundedCornerShape(16.dp))
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
            }



            Spacer(modifier = Modifier.height(24.dp))




        }
        val orderedTags =
            if (todaysTags.isNotEmpty())
                orderTagsByImpact(todaysTags, tagStats)
            else
                emptyList()

        DailyMoodSeal(
            mood = finalScore,
            baseline = baseline,
            drift = if (reflectionScore != null) drift else null,
            entries = entries,
            dayKey = activeDayKey,
            isObsidianConnected = isObsidianConnected,
            onCenterTap = {
                trendsWeekStart = activeWeekStart
                showTrends = true
            },
            onExportTap = {
                val folder = obsidianFolderUri
                if (folder != null) {
                    writeDayMarkdown(
                        context = context,
                        folderUri = folder,
                        dayKey = activeDayKey,
                        entriesForDay = entries,
                        reflectionText = reflectionText,
                        finalScore = finalScore
                    )

                } else {
                    android.util.Log.w(
                        "Obsidian",
                        "Export tapped but no folder selected"
                    )
                }
            },


            modifier = Modifier
                .fillMaxWidth()
                .height(240.dp)
                .aspectRatio(1f)
                .align(Alignment.BottomCenter)
                .padding(bottom = 210.dp)
        )





        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(24.dp),
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
                Text(
                    if (reflectionScore == null && reflectionText.isBlank())
                        "Daily Reflection"
                    else
                        "Edit Reflection"
                )
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
    if (showMenuSheet) {
        ModalBottomSheet(
            onDismissRequest = { showMenuSheet = false },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            containerColor = MaterialTheme.colorScheme.background
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {

                Text(
                    text = "Menu",
                    style = MaterialTheme.typography.headlineSmall,
                    color = Color.White
                )

                Spacer(Modifier.height(8.dp))

                ListItem(
                    headlineContent = { Text("Connect to Obsidian") },
                    supportingContent = {
                        Text(
                            "Choose the folder where .md files will be written",
                            color = Color.White.copy(alpha = 0.6f)
                        )
                    },
                    modifier = Modifier.clickable {
                        showMenuSheet = false
                        obsidianFolderLauncher.launch(null)
                    }
                )

                ListItem(
                    headlineContent = { Text("Settings") },
                    modifier = Modifier.clickable {
                        showMenuSheet = false
                        // TODO: settings screen
                    }
                )

                Spacer(Modifier.height(16.dp))

                TextButton(
                    onClick = { showMenuSheet = false },
                    modifier = Modifier.align(Alignment.End)
                ) {
                    Text("Close")
                }
            }
        }
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

fun buildDailyMarkdown(
    dayKey: String,
    entries: List<LedgerEntry>,
    reflectionText: String,
    finalScore: Float
): String {

    val positives =
        entries
            .filter { it.delta > 0f }
            .sortedByDescending { it.delta }

    val negatives =
        entries
            .filter { it.delta < 0f }
            .sortedBy { it.delta } // more negative first

    val sb = StringBuilder()

    sb.appendLine("### <u>Daily Summary</u>")
    sb.appendLine()

    // --- Positive Impacts ---
    sb.appendLine("#### **Positive Impacts**")
    sb.appendLine()

    if (positives.isEmpty()) {
        sb.appendLine("_No positive impacts recorded._")
        sb.appendLine()
    } else {
        positives.forEach { entry ->
            sb.appendLine(
                ">[!success]- ${entry.title} - ${"%.2f".format(entry.delta)}"
            )

            if (entry.note.isNotBlank()) {
                sb.appendLine("> ${entry.note}")
            }

            sb.appendLine()
        }

    }

    // --- Negative Impacts ---
    sb.appendLine("#### **Negative Impacts**")
    sb.appendLine()

    if (negatives.isEmpty()) {
        sb.appendLine("_No negative impacts recorded._")
        sb.appendLine()
    } else {
        negatives.forEach { entry ->
            val callout =
                if (entry.delta <= -0.75f) "danger" else "error"

            sb.appendLine(
                ">[!$callout]- ${entry.title} - ${"%.2f".format(kotlin.math.abs(entry.delta))}"
            )

            if (entry.note.isNotBlank()) {
                sb.appendLine("> ${entry.note}")
            }

            sb.appendLine()
        }

    }

    // --- Reflection ---
    sb.appendLine("### <u>Daily Reflection</u>")
    sb.appendLine()

    if (reflectionText.isBlank()) {
        sb.appendLine("_No reflection recorded._")
    } else {
        sb.appendLine(reflectionText)
    }

    sb.appendLine()
    sb.appendLine()
    sb.appendLine("#### Daily Score: ${"%.2f".format(finalScore)}")

    return sb.toString()
}

fun writeDayMarkdown(
    context: Context,
    folderUri: Uri,
    dayKey: String,
    entriesForDay: List<LedgerEntry>,
    reflectionText: String,
    finalScore: Float
)
 {
    val resolver = context.contentResolver
    val fileName = "mobile-$dayKey.md"

    try {
        val treeDocId =
            android.provider.DocumentsContract.getTreeDocumentId(folderUri)

        val parentUri =
            android.provider.DocumentsContract.buildDocumentUriUsingTree(
                folderUri,
                treeDocId
            )

        val fileUri =
            android.provider.DocumentsContract.createDocument(
                resolver,
                parentUri,
                "text/markdown",
                fileName
            )

        if (fileUri == null) {
            android.util.Log.e(
                "Obsidian",
                "Failed to create $fileName (null URI)"
            )
            return
        }

        val markdown =
            buildDailyMarkdown(
                dayKey = dayKey,
                entries = entriesForDay,
                reflectionText = reflectionText,
                finalScore = finalScore
            )

        resolver.openOutputStream(fileUri, "w")?.use { stream ->
            stream.write(markdown.toByteArray())
        }


        android.util.Log.d(
            "Obsidian",
            "$fileName written successfully at $fileUri"
        )

    } catch (e: Exception) {
        android.util.Log.e(
            "Obsidian",
            "Failed to write $fileName",
            e
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
                                        enabled = !isFuture
                                    )
                                    {
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
                                        Color.White.copy(alpha = 0.55f)
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