package com.yharim.anotado.domain.repository

import com.yharim.anotado.domain.model.TaskDraft
import com.yharim.anotado.domain.model.Task

interface TaskRepository {
    suspend fun createTask(draft: TaskDraft): Task
    suspend fun getTasks(): List<Task>
    suspend fun setCompleted(id: Long, completed: Boolean): Task
}

class TaskException(message: String, val fieldErrors: Map<String, String> = emptyMap()) : Exception(message)
