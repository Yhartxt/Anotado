package com.yharim.anotado.data.remote.dto

import kotlinx.serialization.Serializable

@Serializable
data class TaskRequestDto(
    val title: String,
    val description: String?,
    val priority: Int,
    val dueDate: String?,
    val completed: Boolean,
    val category: String
)
