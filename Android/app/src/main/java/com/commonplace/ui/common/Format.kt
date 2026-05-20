package com.commonplace.ui.common

import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId

/**
 * Mirrors src/lib/format.ts: "Apr 26 · 14:32" within the current year,
 * "Apr 26, 2025 · 14:32" otherwise.
 */
private val MONTHS = arrayOf(
    "Jan", "Feb", "Mar", "Apr", "May", "Jun",
    "Jul", "Aug", "Sep", "Oct", "Nov", "Dec",
)

fun formatTimestamp(iso: String): String {
    val instant = runCatching { Instant.parse(iso) }.getOrNull() ?: return iso
    val local = LocalDateTime.ofInstant(instant, ZoneId.systemDefault())
    val now = LocalDateTime.now()
    val sameYear = local.year == now.year
    val month = MONTHS[local.monthValue - 1]
    val day = local.dayOfMonth
    val hh = local.hour.toString().padStart(2, '0')
    val mm = local.minute.toString().padStart(2, '0')
    val datePart = if (sameYear) "$month $day" else "$month $day, ${local.year}"
    return "$datePart · $hh:$mm"
}
