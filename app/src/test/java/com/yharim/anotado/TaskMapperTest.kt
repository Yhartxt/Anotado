package com.yharim.anotado

import com.yharim.anotado.data.mapper.TaskMapper
import com.yharim.anotado.data.remote.dto.TaskDto
import com.yharim.anotado.domain.model.Task
import org.junit.Test
import org.junit.Assert.*

class TaskMapperTest {
    @Test fun responseMapsEveryFieldIntoDomain() {
        val dto = TaskDto(42, "Estudiar", "Kotlin", 5, "2026-11-10", true, "Estudio")
        assertEquals(Task(42, "Estudiar", "Kotlin", 5, "2026-11-10", true, "Estudio"), TaskMapper.toDomain(dto))
    }
    @Test fun fullUpdatePreservesNullableFieldsAndFalse() {
        val task = Task(42, "Estudiar", null, 1, null, false, "General")
        val body = TaskMapper.toRequest(task)
        assertEquals(task.title, body.title)
        assertNull(body.description)
        assertEquals(1, body.priority)
        assertNull(body.dueDate)
        assertFalse(body.completed)
        assertEquals("General", body.category)
    }
}
