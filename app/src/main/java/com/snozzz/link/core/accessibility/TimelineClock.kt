package com.snozzz.link.core.accessibility

import java.time.LocalDate
import java.time.ZoneId

object TimelineClock {
    private val zoneId: ZoneId = ZoneId.systemDefault()

    fun startOfTodayMillis(): Long {
        return LocalDate.now(zoneId)
            .atStartOfDay(zoneId)
            .toInstant()
            .toEpochMilli()
    }
}
