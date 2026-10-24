package com.della.skintime.data.db

data class RoutineSessionDetail(
    val id: Int = 0,
    val sessionId: Int,
    val stepName: String,
    val isDone: Boolean = false,
    val isOptional: Boolean = false
)
