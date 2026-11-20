package com.example.media

import com.example.model.SubtitleCue
import java.util.UUID

object SrtParser {

    fun parse(srtContent: String): List<SubtitleCue> {
        if (srtContent.isBlank()) return emptyList()
        val normalized = srtContent.replace("\r\n", "\n").replace("\r", "\n").trim()
        val blocks = normalized.split(Regex("\n\\s*\n"))
        val cues = mutableListOf<SubtitleCue>()

        for (block in blocks) {
            val lines = block.lines().map { it.trimEnd() }.filter { it.isNotBlank() }
            if (lines.isEmpty()) continue

            val timeLineIndex = lines.indexOfFirst { it.contains("-->") }
            if (timeLineIndex == -1) continue

            val timeParts = lines[timeLineIndex].split("-->")
            if (timeParts.size != 2) continue

            val startMs = parseSrtTimeMs(timeParts[0].trim())
            val endMs = parseSrtTimeMs(timeParts[1].trim().split(" ").first())
            if (startMs == null || endMs == null || endMs <= startMs) continue

            val text = lines.drop(timeLineIndex + 1).joinToString("\n").trim()
            if (text.isBlank()) continue

            cues.add(
                SubtitleCue(
                    id = UUID.randomUUID().toString(),
                    startMs = startMs,
                    endMs = endMs,
                    text = text
                )
            )
        }
        return cues.sortedBy { it.startMs }
    }

    fun format(cues: List<SubtitleCue>): String {
        val sorted = cues.sortedBy { it.startMs }
        return buildString {
            sorted.forEachIndexed { index, cue ->
                append(index + 1)
                append('\n')
                append(formatSrtTimeMs(cue.startMs))
                append(" --> ")
                append(formatSrtTimeMs(cue.endMs))
                append('\n')
                append(cue.text.trim())
                append("\n\n")
            }
        }.trimEnd() + "\n"
    }

    private fun parseSrtTimeMs(raw: String): Long? {
        val clean = raw.replace('.', ',')
        val parts = clean.split(':')
        if (parts.size != 3) return null
        val hours = parts[0].toLongOrNull() ?: return null
        val minutes = parts[1].toLongOrNull() ?: return null
        val secMillis = parts[2].split(',')
        if (secMillis.size != 2) return null
        val seconds = secMillis[0].toLongOrNull() ?: return null
        val millis = secMillis[1].padEnd(3, '0').take(3).toLongOrNull() ?: return null
        return ((hours * 3600L) + (minutes * 60L) + seconds) * 1000L + millis
    }

    fun formatSrtTimeMs(ms: Long): String {
        val safe = ms.coerceAtLeast(0L)
        val hours = safe / 3_600_000L
        val minutes = (safe % 3_600_000L) / 60_000L
        val seconds = (safe % 60_000L) / 1000L
        val millis = safe % 1000L
        return String.format("%02d:%02d:%02d,%03d", hours, minutes, seconds, millis)
    }
}
