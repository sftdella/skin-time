package com.della.skintime.data.db

data class ChecklistStep(
    val id: Int = 0,
    val name: String,
    val description: String,
    val orderIndex: Int,
    val isOptional: Boolean = false
)
