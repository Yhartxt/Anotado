package com.yharim.anotado.data.repository

import com.yharim.anotado.data.mapper.TaskMapper
import com.yharim.anotado.data.remote.TaskApiService
import com.yharim.anotado.data.remote.dto.ErrorResponseDto
import kotlinx.serialization.json.Json
import com.yharim.anotado.domain.model.TaskDraft
import com.yharim.anotado.domain.model.Task
import com.yharim.anotado.domain.repository.TaskException
import com.yharim.anotado.domain.repository.TaskRepository
import kotlinx.coroutines.CancellationException
import kotlinx.serialization.SerializationException
import retrofit2.HttpException
import java.io.IOException

class TaskRepositoryImpl(private val api: TaskApiService?, private val json: Json) : TaskRepository {
    override suspend fun createTask(draft: TaskDraft): Task = request { service ->
        TaskMapper.toDomain(service.createTask(TaskMapper.toRequest(draft)))
    }

    override suspend fun getTasks(): List<Task> = request { service ->
        service.getTasks().map(TaskMapper::toDomain)
    }

    override suspend fun setCompleted(id: Long, completed: Boolean): Task = request { service ->
        val current = TaskMapper.toDomain(service.getTask(id))
        TaskMapper.toDomain(service.updateTask(id, TaskMapper.toRequest(current.copy(completed = completed))))
    }

    private suspend fun <T> request(action: suspend (TaskApiService) -> T): T {
        val service = api ?: throw TaskException("El servicio de tareas aún no está disponible.")
        try { return action(service) }
        catch (e: CancellationException) { throw e }
        catch (e: HttpException) {
            if (e.code() == 400) {
                val error = try {
                    e.response()?.errorBody()?.use { json.decodeFromString<ErrorResponseDto>(it.string()) }
                } catch (_: SerializationException) { null } catch (_: IOException) { null }
                throw TaskException(error?.message ?: "Revisa los datos de la tarea.", error?.errors.orEmpty())
            }
            throw TaskException(when (e.code()) {
                401 -> "Tu sesión venció. Vuelve a iniciar sesión."
                404 -> "Esta tarea ya no está disponible. Actualiza la lista."
                else -> "El servicio no está disponible. Inténtalo de nuevo."
            })
        } catch (_: SerializationException) {
            throw TaskException("No pudimos leer las tareas. Inténtalo de nuevo.")
        } catch (_: IOException) {
            throw TaskException("No pudimos conectar. Revisa tu conexión e inténtalo de nuevo.")
        }
    }
}
