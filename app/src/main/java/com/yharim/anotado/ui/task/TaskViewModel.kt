package com.yharim.anotado.ui.task

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yharim.anotado.domain.model.TaskDraft
import com.yharim.anotado.domain.repository.TaskException
import com.yharim.anotado.domain.repository.TaskRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeParseException

data class TaskFormUiState(
    val title: String = "", val description: String = "", val priority: Int = 3,
    val category: String = "General", val dueDate: String = "", val completed: Boolean = false,
    val titleCounter: String = "0/120", val priorityLabel: String = "Media",
    val priorities: List<Int> = (1..5).toList(),
    val categories: List<String> = listOf("General", "Estudio", "Trabajo", "Personal"),
    val categoryMenuOpen: Boolean = false,
    val titleError: String? = null, val descriptionError: String? = null,
    val priorityError: String? = null, val categoryError: String? = null,
    val dueDateError: String? = null, val generalError: String? = null,
    val isSaving: Boolean = false, val saveSuccess: Boolean = false
)

class TaskViewModel(private val taskRepository: TaskRepository) : ViewModel() {
    private val _uiState = MutableStateFlow(TaskFormUiState())
    val uiState: StateFlow<TaskFormUiState> = _uiState.asStateFlow()

    private fun edit(transform: TaskFormUiState.() -> TaskFormUiState) {
        _uiState.update { if (it.isSaving || it.saveSuccess) it else it.transform().copy(generalError = null) }
    }
    fun onTitleChange(value: String) = edit { copy(title = value, titleCounter = "${value.length}/120", titleError = null) }
    fun onDescriptionChange(value: String) = edit { copy(description = value, descriptionError = null) }
    fun onPriorityChange(value: Int) = edit { copy(priority = value, priorityError = null, priorityLabel = when (value) {
        1 -> "Muy baja"; 2 -> "Baja"; 3 -> "Media"; 4 -> "Alta"; 5 -> "Muy alta"; else -> "Prioridad no válida"
    }) }
    fun onCategoryChange(value: String) = edit { copy(category = value, categoryError = null) }
    fun onCategorySelected(value: String) = edit { copy(category = value, categoryError = null, categoryMenuOpen = false) }
    fun onCategoryMenuChange(open: Boolean) = edit { copy(categoryMenuOpen = open) }
    fun onDueDateChange(value: String) = edit { copy(dueDate = value, dueDateError = null) }
    fun onCompletedChange(value: Boolean) = edit { copy(completed = value) }

    fun onSaveClick() {
        val state = _uiState.value
        if (state.isSaving || state.saveSuccess) return
        val title = state.title.trim()
        val date = state.dueDate.trim()
        val titleError = when {
            title.isBlank() -> "Escribe un título"
            title.length > 120 -> "El título admite un máximo de 120 caracteres"
            else -> null
        }
        val priorityError = if (state.priority !in 1..5) "Selecciona una prioridad entre 1 y 5" else null
        val dateError = if (date.isNotEmpty() && !isValidDate(date)) "Usa una fecha válida con formato yyyy-MM-dd" else null
        _uiState.update { it.copy(titleError = titleError, priorityError = priorityError,
            dueDateError = dateError, generalError = null) }
        if (titleError != null || priorityError != null || dateError != null) return
        val draft = TaskDraft(title, state.description.trim().ifEmpty { null }, state.priority,
            date.ifEmpty { null }, state.completed, state.category.trim().ifEmpty { "General" })
        _uiState.update { it.copy(isSaving = true, categoryMenuOpen = false) }
        viewModelScope.launch {
            try {
                taskRepository.createTask(draft)
                _uiState.update { it.copy(saveSuccess = true) }
            } catch (e: CancellationException) { throw e }
            catch (e: TaskException) {
                val fields = e.fieldErrors
                _uiState.update { it.copy(titleError = fields["title"], descriptionError = fields["description"],
                    priorityError = fields["priority"], categoryError = fields["category"], dueDateError = fields["dueDate"],
                    generalError = if (fields.keys.none { field -> field in setOf("title", "description", "priority", "category", "dueDate") }) e.message else null) }
            } catch (_: Exception) {
                _uiState.update { it.copy(generalError = "No pudimos guardar la tarea. Inténtalo de nuevo.") }
            } finally { _uiState.update { it.copy(isSaving = false) } }
        }
    }

    private fun isValidDate(value: String): Boolean = try {
        Regex("[0-9]{4}-[0-9]{2}-[0-9]{2}").matches(value) && LocalDate.parse(value).year in 1..9999
    } catch (_: DateTimeParseException) { false }
}
