package com.della.skintime.data.db

data class RoutineSession(
    val id: Int = 0,
    val date: String,          // "yyyy-MM-dd"
    val time: String,          // "HH:mm"
    val sessionType: String,   // "Morning", "Night", "Aktivitas Tambahan", "Custom"
    val isCompleted: Boolean = false
)
