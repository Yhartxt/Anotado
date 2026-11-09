package com.yharim.anotado.domain.model

data class Task(
    val id: Long,
    val title: String,
    val description: String? = null,
    val priority: Int = 3,
    val dueDate: String? = null,
    val completed: Boolean = false,
    val category: String = "General"
)
