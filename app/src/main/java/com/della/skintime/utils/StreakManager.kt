package com.della.skintime.utils

import java.time.LocalDate
import java.time.format.DateTimeFormatter

object StreakManager {

    private val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")

    fun calculateStreak(completedDates: Set<String>): Int {
        if (completedDates.isEmpty()) return 0

        val dates = completedDates
            .map { LocalDate.parse(it, formatter) }
            .toSortedSet(reverseOrder())

        var streak = 0
        var checkDate = LocalDate.now()

        if (!dates.contains(checkDate)) {
            checkDate = checkDate.minusDays(1)
        }

        for (date in dates) {
            if (date == checkDate) {
                streak++
                checkDate = checkDate.minusDays(1)
            } else {
                break
            }
        }
        return streak
    }

    fun today(): String = LocalDate.now().format(formatter)

    fun formatDate(date: LocalDate): String = date.format(formatter)

    fun parseDate(dateStr: String): LocalDate = LocalDate.parse(dateStr, formatter)
}
