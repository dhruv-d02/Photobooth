package com.dj.photobooth.gallery

import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.format.MonthNames
import kotlinx.datetime.format.Padding
import kotlinx.datetime.format.char
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

/**
 * The "JUL 25, 2026"-style date stamp used on HistoryEntry.stamp and the Gallery card
 * (design/handoff/README.md §4: "the stamp (monospace 9px ..., e.g. `JUL 25, 2026 · F01`)").
 */
private val StampFormat = LocalDate.Format {
    monthName(MonthNames.ENGLISH_ABBREVIATED)
    char(' ')
    day(Padding.NONE)
    chars(", ")
    year()
}

/** Today's date, formatted and upper-cased to match the design's stamp copy exactly. */
@OptIn(ExperimentalTime::class)
fun currentDateStamp(): String {
    val date = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date
    // StampFormat.format(date), not date.format(StampFormat) - the latter is a top-level
    // extension in package kotlinx.datetime that collides by simple name with the
    // kotlinx.datetime.format *package* imported above (MonthNames/Padding/char), so calling
    // through the DateTimeFormat instance itself sidesteps the import ambiguity entirely.
    return StampFormat.format(date).uppercase()
}

@OptIn(ExperimentalTime::class)
fun currentEpochMillis(): Long = Clock.System.now().toEpochMilliseconds()
