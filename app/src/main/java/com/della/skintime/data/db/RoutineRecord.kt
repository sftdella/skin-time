package com.della.skintime.data.db

data class RoutineRecord(
    val date: String,                         // format "yyyy-MM-dd"
    val completedStepIds: String = "",        // comma-separated step ids
    val isFullyCompleted: Boolean = false,
    val completedActivityIds: String = "",    // comma-separated activity ids
    val completedAt: String = ""              // format "HH:mm" waktu selesai
)
