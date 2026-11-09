package com.yharim.anotado.data.remote.dto

import kotlinx.serialization.Serializable

@Serializable
data class TaskDto(
    val id: Long,
    val title: String,
    val description: String? = null,
    val priority: Int = 3,
    val dueDate: String? = null,
    val completed: Boolean = false,
    val category: String = "General"
)
