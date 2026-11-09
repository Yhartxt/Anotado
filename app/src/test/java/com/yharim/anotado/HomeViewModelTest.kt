package com.yharim.anotado

import androidx.lifecycle.viewModelScope
import com.yharim.anotado.domain.model.Task
import com.yharim.anotado.domain.repository.*
import com.yharim.anotado.ui.home.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.*
import org.junit.*
import org.junit.Assert.*

@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    private val tasks = FakeTasks()
    private val auth = FakeAuth()
    private val models = mutableListOf<HomeViewModel>()
    @Before fun setup() { Dispatchers.setMain(dispatcher) }
    @After fun teardown() { models.forEach { it.viewModelScope.cancel() }; Dispatchers.resetMain() }
    private fun model() = HomeViewModel(auth, tasks).also { models.add(it) }

    @Test fun loadsRealDomainTasksAndCalculatesSummary() = runTest(dispatcher) {
        val vm = model()
        assertTrue(vm.uiState.value.isLoading)
        advanceUntilIdle()
        with(vm.uiState.value) {
            assertTrue(hasLoaded); assertFalse(isLoading)
            assertEquals("Hola, Ana", greeting)
            assertEquals("Tienes 1 tarea por completar.", pendingLabel)
            assertEquals("2 en total", totalLabel)
            assertEquals("50%", progressLabel)
            assertEquals(2, tasks.size)
            assertEquals("MUY ALTA", tasks.first().priorityLabel)
        }
    }
    @Test fun filteringDoesNotChangeGlobalProgressOrCallApiAgain() = runTest(dispatcher) {
        val vm = model(); advanceUntilIdle()
        vm.onFilterChange(TaskFilter.COMPLETED)
        assertEquals(listOf(2L), vm.uiState.value.tasks.map { it.id })
        assertEquals("50%", vm.uiState.value.progressLabel)
        vm.onFilterChange(TaskFilter.PENDING)
        assertEquals(listOf(1L), vm.uiState.value.tasks.map { it.id })
        assertEquals(1, tasks.loads)
    }
    @Test fun emptyResultIsDifferentFromConnectionFailureAndRetryWorks() = runTest(dispatcher) {
        tasks.failure = TaskException("Sin conexión")
        val vm = model(); advanceUntilIdle()
        assertFalse(vm.uiState.value.hasLoaded)
        assertEquals("Sin conexión", vm.uiState.value.error)
        tasks.failure = null; tasks.values = emptyList()
        vm.onRefresh(); advanceUntilIdle()
        assertTrue(vm.uiState.value.hasLoaded)
        assertNull(vm.uiState.value.error)
        assertEquals("0%", vm.uiState.value.progressLabel)
        assertEquals("Aún no tienes tareas.", vm.uiState.value.emptyMessage)
    }
    @Test fun markingCompleteUpdatesProgressAndPendingFilterAfterServerSuccess() = runTest(dispatcher) {
        val vm = model(); advanceUntilIdle(); vm.onFilterChange(TaskFilter.PENDING)
        vm.onToggleCompleted(1); vm.onToggleCompleted(1)
        assertEquals(setOf(1L), vm.uiState.value.updatingIds)
        assertFalse(vm.uiState.value.tasks.first().completed)
        advanceUntilIdle()
        assertEquals(1, tasks.updates)
        assertEquals("100%", vm.uiState.value.progressLabel)
        assertTrue(vm.uiState.value.tasks.isEmpty())
        assertTrue(vm.uiState.value.updatingIds.isEmpty())
    }
    @Test fun failedWriteDoesNotPretendTaskWasCompleted() = runTest(dispatcher) {
        val vm = model(); advanceUntilIdle()
        tasks.failure = TaskException("No se pudo guardar")
        vm.onToggleCompleted(1); advanceUntilIdle()
        assertFalse(vm.uiState.value.tasks.first().completed)
        assertEquals("50%", vm.uiState.value.progressLabel)
        assertNotNull(vm.uiState.value.error)
        assertTrue(vm.uiState.value.updatingIds.isEmpty())
    }
    @Test fun failedRefreshPreservesPreviouslyLoadedTasks() = runTest(dispatcher) {
        val vm = model(); advanceUntilIdle()
        tasks.failure = TaskException("Sin conexión")
        vm.onRefresh(); advanceUntilIdle()
        assertEquals(2, vm.uiState.value.tasks.size)
        assertNotNull(vm.uiState.value.error)
    }
    @Test fun taskSelectionAndRefreshAfterCreationAreStateEvents() = runTest(dispatcher) {
        val vm = model(); advanceUntilIdle()
        vm.onTaskClick(1)
        assertEquals("Detalles", vm.uiState.value.selectedTask?.description)
        vm.onDismissDialog(); assertNull(vm.uiState.value.selectedTask)
        tasks.values = tasks.values + Task(9, "Nueva")
        vm.onTaskCreated(); advanceUntilIdle()
        assertEquals(3, vm.uiState.value.tasks.size)
    }
    @Test fun absentNameAndInvalidDateNeverInventValuesOrCrash() = runTest(dispatcher) {
        auth.name = null
        tasks.values = listOf(Task(3, "Ejemplo", dueDate = "invalid"))
        val vm = model(); advanceUntilIdle()
        assertEquals("Hola", vm.uiState.value.greeting)
        assertEquals("Fecha no válida", vm.uiState.value.tasks.first().dateLabel)
    }
    private class FakeTasks : TaskRepository {
        var values = listOf(Task(1, "Primera", "Detalles", priority = 5), Task(2, "Segunda", completed = true))
        var failure: Exception? = null
        var loads = 0
        var updates = 0
        override suspend fun createTask(draft: com.yharim.anotado.domain.model.TaskDraft): com.yharim.anotado.domain.model.Task = error("Unexpected create")
        override suspend fun getTasks(): List<Task> { loads++; failure?.let { throw it }; return values }
        override suspend fun setCompleted(id: Long, completed: Boolean): Task {
            updates++; failure?.let { throw it }
            val task = values.first { it.id == id }.copy(completed = completed)
            values = values.map { if (it.id == id) task else it }
            return task
        }
    }
    private class FakeAuth : AuthRepository {
        var name: String? = "Ana"
        override val isAuthenticated = MutableStateFlow(true)
        override fun getUserName() = name
        override suspend fun login(email: String, password: String) = Unit
        override suspend fun register(name: String, email: String, password: String) = Unit
        override fun logout() { isAuthenticated.value = false }
    }
}
