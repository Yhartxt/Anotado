package com.yharim.anotado.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yharim.anotado.domain.model.Task
import com.yharim.anotado.domain.repository.AuthRepository
import com.yharim.anotado.domain.repository.TaskRepository
import com.yharim.anotado.domain.repository.TaskException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
import java.util.Locale

enum class TaskFilter(val label: String) { ALL("Todas"), PENDING("Pendientes"), COMPLETED("Completadas") }

data class TaskItemUiState(
    val id: Long, val title: String, val description: String,
    val priority: Int, val priorityLabel: String, val category: String,
    val dateLabel: String, val completed: Boolean
)

data class HomeUiState(
    val greeting: String = "Hola",
    val pendingLabel: String = "",
    val totalLabel: String = "",
    val progress: Float = 0f,
    val progressLabel: String = "0%",
    val tasks: List<TaskItemUiState> = emptyList(),
    val filter: TaskFilter = TaskFilter.ALL,
    val emptyMessage: String = "Aún no tienes tareas.",
    val isLoading: Boolean = false,
    val hasLoaded: Boolean = false,
    val updatingIds: Set<Long> = emptySet(),
    val isLoggingOut: Boolean = false,
    val error: String? = null,
    val selectedTask: TaskItemUiState? = null
)

class HomeViewModel(
    private val authRepository: AuthRepository,
    private val taskRepository: TaskRepository
) : ViewModel() {
    private var tasks = emptyList<Task>()
    private var loadJob: Job? = null
    private val updateJobs = mutableMapOf<Long, Job>()
    private val _uiState = MutableStateFlow(HomeUiState(
        greeting = authRepository.getUserName()?.trim()?.takeIf { it.isNotEmpty() }?.let { "Hola, $it" } ?: "Hola"
    ))
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()
    init { onRefresh() }

    fun onRefresh() {
        if (_uiState.value.isLoading || _uiState.value.updatingIds.isNotEmpty()) return
        _uiState.update { it.copy(isLoading = true, error = null) }
        loadJob = viewModelScope.launch {
            try {
                tasks = taskRepository.getTasks()
                _uiState.update { it.copy(hasLoaded = true) }
                renderTasks()
            } catch (e: CancellationException) { throw e }
            catch (e: Exception) { showError(e) }
            finally { _uiState.update { it.copy(isLoading = false) } }
        }
    }

    fun onFilterChange(filter: TaskFilter) {
        _uiState.update { it.copy(filter = filter) }
        renderTasks()
    }

    fun onToggleCompleted(id: Long) {
        val state = _uiState.value
        if (state.isLoading || id in state.updatingIds || state.isLoggingOut) return
        val task = tasks.find { it.id == id } ?: return
        _uiState.update { it.copy(updatingIds = it.updatingIds + id, error = null) }
        updateJobs[id] = viewModelScope.launch {
            try {
                val updated = taskRepository.setCompleted(id, !task.completed)
                tasks = tasks.map { if (it.id == id) updated else it }
                renderTasks()
            } catch (e: CancellationException) { throw e }
            catch (e: Exception) { showError(e) }
            finally {
                _uiState.update { it.copy(updatingIds = it.updatingIds - id) }
                updateJobs.remove(id)
            }
        }
    }

    fun onTaskClick(id: Long) {
        _uiState.update { it.copy(selectedTask = tasks.find { task -> task.id == id }?.toUi()) }
    }
    fun onTaskCreated() {
        viewModelScope.launch {
            loadJob?.join()
            updateJobs.values.toList().joinAll()
            onFilterChange(TaskFilter.ALL)
            onRefresh()
        }
    }
    fun onDismissDialog() { _uiState.update { it.copy(selectedTask = null) } }

    fun onLogoutClick() {
        if (_uiState.value.isLoggingOut) return
        _uiState.update { it.copy(isLoggingOut = true, error = null) }
        try {
            authRepository.logout()
            loadJob?.cancel()
            updateJobs.values.toList().forEach { it.cancel() }
        } catch (e: CancellationException) { throw e }
        catch (_: Exception) {
            _uiState.update { it.copy(error = "No pudimos cerrar la sesión. Inténtalo de nuevo.") }
        } finally { _uiState.update { it.copy(isLoggingOut = false) } }
    }

    private fun showError(error: Exception) {
        _uiState.update { it.copy(error = if (error is TaskException) error.message
            else "No pudimos completar la operación. Inténtalo de nuevo.") }
    }

    private fun renderTasks() {
        val completed = tasks.count { it.completed }
        val pending = tasks.size - completed
        val progress = if (tasks.isEmpty()) 0f else completed.toFloat() / tasks.size
        _uiState.update { state ->
            val filtered = tasks.filter {
                when (state.filter) {
                    TaskFilter.ALL -> true
                    TaskFilter.PENDING -> !it.completed
                    TaskFilter.COMPLETED -> it.completed
                }
            }
            state.copy(
                tasks = filtered.map { it.toUi() },
                pendingLabel = if (pending == 1) "Tienes 1 tarea por completar." else "Tienes $pending tareas por completar.",
                totalLabel = "${tasks.size} en total", progress = progress,
                progressLabel = "${(progress * 100).toInt()}%",
                emptyMessage = when (state.filter) {
                    TaskFilter.ALL -> "Aún no tienes tareas."
                    TaskFilter.PENDING -> "No tienes tareas pendientes."
                    TaskFilter.COMPLETED -> "Aún no tienes tareas completadas."
                },
                selectedTask = state.selectedTask?.let { selected -> tasks.find { it.id == selected.id }?.toUi() }
            )
        }
    }

    private fun Task.toUi() = TaskItemUiState(
        id, title, description?.takeIf { it.isNotBlank() } ?: "Sin descripción",
        priority, when (priority) {
            1 -> "MUY BAJA"; 2 -> "BAJA"; 3 -> "MEDIA"; 4 -> "ALTA"; 5 -> "MUY ALTA"; else -> "SIN PRIORIDAD"
        }, category, formatDate(dueDate), completed
    )

    private fun formatDate(value: String?): String = if (value.isNullOrBlank()) "Sin fecha" else try {
        LocalDate.parse(value).format(DateTimeFormatter.ofPattern("d MMM yyyy", Locale.forLanguageTag("es")))
    } catch (_: DateTimeParseException) { "Fecha no válida" }
}
