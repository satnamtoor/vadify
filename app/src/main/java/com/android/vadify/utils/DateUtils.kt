package com.android.vadify.utils

import org.threeten.bp.ZoneId
import org.threeten.bp.ZoneOffset
import org.threeten.bp.ZonedDateTime
import org.threeten.bp.format.DateTimeFormatter
import org.threeten.bp.format.FormatStyle
import java.util.*


val dateTimePattern: DateTimeFormatter =
    DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss", Locale.ENGLISH)
val dateTimeFormatter: DateTimeFormatter = DateTimeFormatter.ofLocalizedDateTime(FormatStyle.SHORT)
val dateFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("yyy MMM dd", Locale.ENGLISH)
val timeFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm", Locale.ENGLISH)
val timeFormatter2: DateTimeFormatter = DateTimeFormatter.ofPattern("hh:mm a", Locale.ENGLISH)


fun dateTime(dateTime: ZonedDateTime?): CharSequence? {
    if (dateTime == null) return null

    return dateFormatter.format(dateTime.toLocalDateTime())
}


fun time(locationDateTime: String?): String? {
    if (locationDateTime.isNullOrBlank()) return null
    return timeFormatter2.format(
        ZonedDateTime.parse(locationDateTime).withZoneSameInstant(ZoneId.systemDefault())
            .toLocalTime()
    )
}

fun date(locationDateTime: String?): String? {
    if (locationDateTime.isNullOrBlank()) return null
    return dateFormatter.format(
        ZonedDateTime.parse(locationDateTime).withZoneSameInstant(ZoneId.systemDefault())
            .toLocalDate()
    )
}

fun getLocalDateTimeToUtc(): String {
    return ZonedDateTime.now().withZoneSameInstant(ZoneOffset.UTC).toString()
}


fun isPreviousDate(startDate: String?, nextDate: String?): Boolean {
    return if (startDate.isNullOrBlank() || nextDate.isNullOrBlank()) {
        false
    } else {
        ZonedDateTime.parse(startDate).withZoneSameInstant(ZoneId.systemDefault())
            .toLocalDate() > ZonedDateTime.parse(nextDate)
            .withZoneSameInstant(ZoneId.systemDefault()).toLocalDate()
    }
}













