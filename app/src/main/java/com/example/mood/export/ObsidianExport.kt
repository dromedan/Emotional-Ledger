package com.example.mood.export

import android.content.Context
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import java.time.LocalDate
import java.time.format.DateTimeFormatter

suspend fun exportWeeklyReflectionToObsidian(
    context: Context,
    folderUri: Uri,
    weekStart: LocalDate,
    content: String
) {
    val baseline = 5.0f
    val weekEnd = weekStart.plusDays(6)

    val weeklyData =
        com.example.mood.data.LedgerStore
            .loadWeeklyMood(context, weekStart, baseline)

    val weeklyAverage =
        if (weeklyData.isNotEmpty())
            weeklyData.map { it.score }.average()
        else
            0.0

    val vibesSection = buildString {
        appendLine("### 🎭 Weekly Mood Vibes")
        appendLine()

        weeklyData.forEach { point ->
            val dayName =
                point.date.dayOfWeek.name
                    .lowercase()
                    .replaceFirstChar { it.uppercase() }

            val percent =
                ((point.score / 10f) * 100).toInt()

            val barColor =
                if (point.score >= baseline)
                    "#34c759"
                else
                    "#ff453a"

            appendLine(
                """
<div style="display:flex; align-items:center; gap:12px; background:rgba(255,255,255,0.04); padding:8px 10px; border-radius:10px; margin:6px 0;">
  <div style="min-width:110px; font-weight:600;">$dayName</div>
  <div style="flex:1; height:8px; max-width:50%; background:#444; border-radius:5px; overflow:hidden;">
    <div style="height:100%; width:${percent}%; background:$barColor; border-radius:5px;"></div>
  </div>
  <span style="min-width:32px; text-align:right; font-weight:600;">${"%.2f".format(point.score)}</span>
</div>
""".trimIndent()
            )
        }

        appendLine()
        appendLine("**Weekly average mood: ${"%.2f".format(weeklyAverage)}**")
        appendLine()
    }

    val markdown = buildString {
        appendLine("---")
        appendLine("week_start: $weekStart")
        appendLine("week_end: $weekEnd")
        appendLine("---")
        appendLine()
        appendLine("# Week ending $weekEnd")
        appendLine()
        appendLine("- **Start:** $weekStart (Sunday)")
        appendLine("- **End:** $weekEnd (Saturday)")
        appendLine()
        append(vibesSection)
        appendLine()
        appendLine("### Weekly Reflection")
        appendLine()
        appendLine(
            if (content.isBlank())
                "_No weekly reflection written._"
            else
                content
        )
    }

    writeMarkdownFile(
        context,
        folderUri,
        "weekly-$weekStart.md",
        markdown
    )
}



fun exportMonthlyReflectionToObsidian(
    context: Context,
    folderUri: Uri,
    visibleMonth: LocalDate,
    content: String
) {
    val key =
        visibleMonth.format(DateTimeFormatter.ofPattern("yyyy-MM"))

    val title =
        visibleMonth.format(DateTimeFormatter.ofPattern("MMMM yyyy"))

    val filename = "monthly-$key.md"

    val header =
        "# Monthly Reflection — $title\n\n"

    writeMarkdownFile(
        context,
        folderUri,
        filename,
        header + content
    )
}

private fun writeMarkdownFile(
    context: Context,
    folderUri: Uri,
    filename: String,
    text: String
) {
    val folder =
        DocumentFile.fromTreeUri(context, folderUri)
            ?: return

    val file =
        folder.findFile(filename)
            ?: folder.createFile("text/markdown", filename)
            ?: return

    context.contentResolver.openOutputStream(file.uri, "w")?.use {
        it.write(text.toByteArray())
    }
}
