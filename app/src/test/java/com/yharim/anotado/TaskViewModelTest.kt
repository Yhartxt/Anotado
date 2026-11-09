package com.yharim.anotado

import com.yharim.anotado.domain.model.*
import com.yharim.anotado.domain.repository.*
import com.yharim.anotado.ui.task.TaskViewModel
import kotlinx.coroutines.*
import kotlinx.coroutines.test.*
import org.junit.*
import org.junit.Assert.*

@OptIn(ExperimentalCoroutinesApi::class)
class TaskViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    private val repository = FakeTasks()
    private lateinit var vm: TaskViewModel
    @Before fun setup() { Dispatchers.setMain(dispatcher); vm = TaskViewModel(repository) }
    @After fun teardown() { Dispatchers.resetMain() }

    @Test fun emptyTitleBlocksSaving() = runTest(dispatcher) {
        vm.onTitleChange("   "); vm.onSaveClick(); advanceUntilIdle()
        assertNotNull(vm.uiState.value.titleError); assertEquals(0, repository.calls)
    }
    @Test fun titleLimitAccepts120AndRejects121() = runTest(dispatcher) {
        vm.onTitleChange("x".repeat(121)); vm.onSaveClick(); advanceUntilIdle()
        assertNotNull(vm.uiState.value.titleError); assertEquals(0, repository.calls)
        vm.onTitleChange("x".repeat(120)); vm.onSaveClick(); advanceUntilIdle()
        assertTrue(vm.uiState.value.saveSuccess); assertEquals(1, repository.calls)
    }
    @Test fun invalidCalendarDateAndPriorityAreRejected() = runTest(dispatcher) {
        vm.onTitleChange("Tarea"); vm.onDueDateChange("2026-02-30"); vm.onPriorityChange(6)
        vm.onSaveClick(); advanceUntilIdle()
        assertNotNull(vm.uiState.value.dueDateError)
        assertNotNull(vm.uiState.value.priorityError)
        assertEquals(0, repository.calls)
    }
    @Test fun nonIsoDateIsRejected() = runTest(dispatcher) {
        vm.onTitleChange("Tarea"); vm.onDueDateChange("1/2/2026")
        vm.onSaveClick(); advanceUntilIdle()
        assertNotNull(vm.uiState.value.dueDateError); assertEquals(0, repository.calls)
    }
    @Test fun optionalFieldsBecomeNullAndCategoryDefaultsToGeneral() = runTest(dispatcher) {
        vm.onTitleChange("  Estudiar  "); vm.onDescriptionChange(" "); vm.onCategoryChange(" ")
        vm.onSaveClick(); advanceUntilIdle()
        assertEquals(TaskDraft("Estudiar"), repository.draft)
        assertTrue(vm.uiState.value.saveSuccess)
    }
    @Test fun allFieldsReachRepositoryWithoutFakeId() = runTest(dispatcher) {
        vm.onTitleChange("Estudiar"); vm.onDescriptionChange(" Kotlin "); vm.onPriorityChange(5)
        vm.onDueDateChange("2028-02-29"); vm.onCategorySelected("Estudio"); vm.onCompletedChange(true)
        vm.onSaveClick(); advanceUntilIdle()
        assertEquals(TaskDraft("Estudiar", "Kotlin", 5, "2028-02-29", true, "Estudio"), repository.draft)
    }
    @Test fun duplicateSavesAndEditsAreBlockedWhileSavingAndAfterSuccess() = runTest(dispatcher) {
        vm.onTitleChange("Original"); vm.onSaveClick(); vm.onSaveClick(); vm.onTitleChange("Cambio")
        assertTrue(vm.uiState.value.isSaving)
        advanceUntilIdle(); vm.onSaveClick()
        assertEquals(1, repository.calls); assertEquals("Original", repository.draft?.title)
        assertFalse(vm.uiState.value.isSaving)
    }
    @Test fun failurePreservesDraftAndAllowsRetry() = runTest(dispatcher) {
        repository.failure = TaskException("Sin conexión")
        vm.onTitleChange("Estudiar"); vm.onSaveClick(); advanceUntilIdle()
        assertFalse(vm.uiState.value.saveSuccess); assertEquals("Estudiar", vm.uiState.value.title)
        assertEquals("Sin conexión", vm.uiState.value.generalError)
        repository.failure = null; vm.onSaveClick(); advanceUntilIdle()
        assertTrue(vm.uiState.value.saveSuccess)
    }
    @Test fun serverFieldErrorsAreExposedInUiState() = runTest(dispatcher) {
        repository.failure = TaskException("Inválido", mapOf("title" to "Revisa el título", "dueDate" to "Fecha inválida"))
        vm.onTitleChange("Tarea"); vm.onSaveClick(); advanceUntilIdle()
        assertEquals("Revisa el título", vm.uiState.value.titleError)
        assertEquals("Fecha inválida", vm.uiState.value.dueDateError)
        assertNull(vm.uiState.value.generalError)
    }
    @Test fun cancellationDoesNotBecomeSuccessOrError() = runTest(dispatcher) {
        repository.failure = CancellationException()
        vm.onTitleChange("Tarea"); vm.onSaveClick(); advanceUntilIdle()
        assertFalse(vm.uiState.value.saveSuccess); assertFalse(vm.uiState.value.isSaving)
        assertNull(vm.uiState.value.generalError)
    }
    private class FakeTasks : TaskRepository {
        var calls = 0
        var draft: TaskDraft? = null
        var failure: Exception? = null
        override suspend fun createTask(draft: TaskDraft): Task {
            calls++; this.draft = draft; failure?.let { throw it }
            return Task(7, draft.title, draft.description, draft.priority, draft.dueDate, draft.completed, draft.category)
        }
        override suspend fun getTasks() = emptyList<Task>()
        override suspend fun setCompleted(id: Long, completed: Boolean): Task = error("Unexpected update")
    }
}
