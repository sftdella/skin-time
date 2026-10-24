package com.della.skintime.data.db

data class ActivitySchedule(
    val id: Int = 0,
    val name: String,
    val days: List<Int> = emptyList()  // 1=Senin ... 7=Minggu (ISO DayOfWeek)
)
