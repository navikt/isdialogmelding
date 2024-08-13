package no.nav.syfo.cronjob

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.time.*

internal val log: Logger = LoggerFactory.getLogger("no.nav.syfo.cronjob.CronjobUtils")

fun calculateInitialDelay(cronJobName: String, runAtHour: Int): Long {
    val from = LocalDateTime.now()
    val nowDate = LocalDate.now()
    val nextTimeToRun = LocalDateTime.of(
        if (from.hour < runAtHour) nowDate else nowDate.plusDays(1),
        LocalTime.of(runAtHour, 0),
    )
    val initialDelay = Duration.between(from, nextTimeToRun).toMinutes()
    log.info("$cronJobName will run in $initialDelay minutes at $nextTimeToRun")
    return initialDelay
}

fun calculateWeeklyInitialDelay(cronJobName: String, runDay: DayOfWeek, runAtHour: Int): Long {
    val from = LocalDateTime.now()
    val nowDate = LocalDate.now()
    val nowDay = nowDate.dayOfWeek
    val daysUntilRunDay = if (nowDay <= runDay) (runDay.value - nowDay.value) else (7 - nowDay.value + runDay.value)
    val nextTimeToRun = LocalDateTime.of(
        nowDate.plusDays(if (daysUntilRunDay == 0 && runAtHour < from.hour) 7 else daysUntilRunDay.toLong()),
        LocalTime.of(runAtHour, 0),
    )
    val initialDelay = Duration.between(from, nextTimeToRun).toMinutes()
    log.info("$cronJobName will run in $initialDelay minutes at $nextTimeToRun")
    return initialDelay
}
