package com.yharim.anotado.data.mapper

import com.yharim.anotado.data.remote.dto.TaskDto
import com.yharim.anotado.data.remote.dto.TaskRequestDto
import com.yharim.anotado.domain.model.TaskDraft
import com.yharim.anotado.domain.model.Task

object TaskMapper {
    fun toDomain(dto: TaskDto) = Task(dto.id, dto.title, dto.description, dto.priority,
        dto.dueDate, dto.completed, dto.category)
    fun toRequest(draft: TaskDraft) = TaskRequestDto(draft.title, draft.description, draft.priority,
        draft.dueDate, draft.completed, draft.category)
    fun toRequest(task: Task) = TaskRequestDto(task.title, task.description, task.priority,
        task.dueDate, task.completed, task.category)
}
