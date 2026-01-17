@file:OptIn(kotlinx.serialization.InternalSerializationApi::class)
package com.example.mood.ui.draw

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.unit.dp
import com.example.mood.model.LedgerEntry

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.unit.sp
import com.example.mood.model.TagStats
import com.example.mood.deltaColor
import com.example.mood.tagImpactColor
import com.example.mood.ui.theme.LedgerTeal
import kotlin.math.max

fun DrawScope.drawInfluenceBands(
    entries: List<LedgerEntry>
) {
    if (entries.isEmpty()) return

    val radius = size.minDimension / 2.15f
    val baseThickness = 10.dp.toPx()
    val maxExtraThickness = 14.dp.toPx()

    val totalAbs =
        entries.sumOf { kotlin.math.abs(it.delta).toDouble() }
            .toFloat()
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

fun DrawScope.drawTealHalo() {

    val ringRadius = size.minDimension / 2.15f
    val ringThickness = 26.dp.toPx()

    val tubeGradient = Brush.linearGradient(
        colors = listOf(
            LedgerTeal.copy(alpha = 0.95f),
            LedgerTeal.copy(alpha = 0.75f),
            LedgerTeal.copy(alpha = 0.55f),
            LedgerTeal.copy(alpha = 0.80f)
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
fun DrawScope.drawEntryDots(
    entries: List<LedgerEntry>,
    angles: List<Float>
) {
    if (entries.isEmpty()) return
    if (angles.size < entries.size) return

    val ringRadius = size.minDimension / 2.15f
    val dotRadius = 9.dp.toPx()

    entries.forEachIndexed { index, entry ->
        val angleRad = Math.toRadians(angles[index].toDouble())

        val x =
            center.x + kotlin.math.cos(angleRad).toFloat() * ringRadius
        val y =
            center.y + kotlin.math.sin(angleRad).toFloat() * ringRadius

        val baseColor = deltaColor(entry.delta)

        drawCircle(
            color = Color.Black.copy(alpha = 0.28f),
            radius = dotRadius * 1.05f,
            center = Offset(
                x + 2.dp.toPx(),
                y + 2.dp.toPx()
            )
        )

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
fun DrawScope.drawMoodSeal(
    tags: List<String>,
    tagStats: Map<String, TagStats>,
    rotationDeg: Float,
    touchAngle: Float?,
    activeTagIndex: Int?,
    onActiveTagChange: (Int?) -> Unit
) {
    drawIntoCanvas { canvas ->
        val paint = android.graphics.Paint().apply {
            isAntiAlias = true
            textSize = 14.sp.toPx()
            alpha = 180
        }

        val fontMetrics = paint.fontMetrics
        val textOffset = -fontMetrics.ascent / 2f

        val tagPaddingDeg = 6f
        val minSweepDeg = 14f

        val radius = size.minDimension / 2.2f
        val textRadius = radius
        val bandThickness =
            (fontMetrics.descent - fontMetrics.ascent) + 10.dp.toPx()

        if (tags.isEmpty()) return@drawIntoCanvas

        val baseStartAngle = -160f + rotationDeg
        val normalizedBaseStart =
            ((-160f + rotationDeg) % 360f + 360f) % 360f

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

        val tagSweeps = tags.map { tag ->
            val raw =
                paint.measureText(tag) /
                        (2 * Math.PI.toFloat() * textRadius) * 360f
            max(raw, minSweepDeg) + tagPaddingDeg
        }

        val totalSweep = tagSweeps.sum()
        val sweepScale =
            if (totalSweep > 282f) 282f / totalSweep else 1f

        var arcOffset = 0f
        val separator = " · "

        tags.forEachIndexed { index, tag ->
            val sweep = tagSweeps[index] * sweepScale
            val avg = tagStats[tag]?.average ?: 0f
            val color = Color(tagImpactColor(avg))

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

            if (isActive) onActiveTagChange(index)

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
                    size = Size(textRadius * 2, textRadius * 2),
                    style = Stroke(
                        width = bandThickness + 12.dp.toPx(),
                        cap = StrokeCap.Round
                    )
                )
            }

            drawArc(
                color = Color.Black,
                startAngle = baseStartAngle + arcOffset,
                sweepAngle = sweep,
                useCenter = false,
                topLeft = Offset(
                    center.x - textRadius,
                    center.y - textRadius
                ),
                size = Size(textRadius * 2, textRadius * 2),
                style = Stroke(
                    width = bandThickness + 2.dp.toPx(),
                    cap = StrokeCap.Round
                )
            )

            drawArc(
                color = color,
                startAngle = baseStartAngle + arcOffset,
                sweepAngle = sweep,
                useCenter = false,
                topLeft = Offset(
                    center.x - textRadius,
                    center.y - textRadius
                ),
                size = Size(textRadius * 2, textRadius * 2),
                style = Stroke(
                    width = bandThickness,
                    cap = StrokeCap.Round
                )
            )

            paint.color = android.graphics.Color.parseColor("#0F1A24")

            canvas.nativeCanvas.drawTextOnPath(
                tag,
                path,
                arcOffset * radius * Math.PI.toFloat() / 180f,
                textOffset,
                paint
            )

            arcOffset += sweep

            if (index < tags.lastIndex) {
                canvas.nativeCanvas.drawTextOnPath(
                    separator,
                    path,
                    arcOffset * radius * Math.PI.toFloat() / 180f,
                    textOffset,
                    paint
                )
            }
        }
    }
}
