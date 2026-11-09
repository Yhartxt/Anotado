package com.yharim.anotado

import androidx.lifecycle.viewModelScope
import com.example.taskmanager.ui.navigation.AppViewModel
import com.yharim.anotado.ui.home.HomeViewModel
import com.yharim.anotado.domain.repository.AuthRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.Assert.*

@OptIn(ExperimentalCoroutinesApi::class)
class SessionViewModelsTest {
    private val dispatcher = StandardTestDispatcher()
    private val repository = FakeRepository()
    private val models = mutableListOf<AppViewModel>()
    @Before fun setup() { Dispatchers.setMain(dispatcher) }
    @After fun teardown() {
        models.forEach { it.viewModelScope.cancel() }
        Dispatchers.resetMain()
    }
    private fun app() = AppViewModel(repository).also { models.add(it) }

    @Test fun persistedSessionIsAvailableImmediatelyWithoutLoginFlash() {
        repository.isAuthenticated.value = true
        assertTrue(app().uiState.value.isAuthenticated)
    }
    @Test fun noSessionStartsUnauthenticated() {
        assertFalse(app().uiState.value.isAuthenticated)
    }
    @Test fun appTracksLoginAndRemoteSessionInvalidation() = runTest(dispatcher) {
        val app = app()
        advanceUntilIdle()
        repository.isAuthenticated.value = true
        advanceUntilIdle()
        assertTrue(app.uiState.value.isAuthenticated)
        repository.isAuthenticated.value = false
        advanceUntilIdle()
        assertFalse(app.uiState.value.isAuthenticated)
    }
    @Test fun logoutEventReachesRepositoryAndUpdatesAppState() = runTest(dispatcher) {
        repository.isAuthenticated.value = true
        val app = app()
        val home = HomeViewModel(repository, EmptyTasks())
        advanceUntilIdle()
        home.onLogoutClick()
        advanceUntilIdle()
        assertEquals(1, repository.logoutCalls)
        assertFalse(app.uiState.value.isAuthenticated)
        assertFalse(home.uiState.value.isLoggingOut)
        assertNull(home.uiState.value.error)
    }
    @Test fun logoutFailureKeepsSessionAndAllowsRetry() = runTest(dispatcher) {
        repository.isAuthenticated.value = true
        repository.logoutFails = true
        val app = app()
        val home = HomeViewModel(repository, EmptyTasks())
        home.onLogoutClick()
        advanceUntilIdle()
        assertTrue(app.uiState.value.isAuthenticated)
        assertNotNull(home.uiState.value.error)
        assertFalse(home.uiState.value.isLoggingOut)
        repository.logoutFails = false
        home.onLogoutClick()
        advanceUntilIdle()
        assertFalse(app.uiState.value.isAuthenticated)
        assertNull(home.uiState.value.error)
    }
    private class EmptyTasks : com.yharim.anotado.domain.repository.TaskRepository {
        override suspend fun createTask(draft: com.yharim.anotado.domain.model.TaskDraft): com.yharim.anotado.domain.model.Task = error("Unexpected create")
        override suspend fun getTasks() = emptyList<com.yharim.anotado.domain.model.Task>()
        override suspend fun setCompleted(id: Long, completed: Boolean): com.yharim.anotado.domain.model.Task = error("Unexpected update")
    }
    private class FakeRepository : AuthRepository {
        override val isAuthenticated = MutableStateFlow(false)
        var logoutCalls = 0
        var logoutFails = false
        override suspend fun login(email: String, password: String) = error("Unexpected login")
        override suspend fun register(name: String, email: String, password: String) = error("Unexpected registration")
        override fun getUserName(): String? = null
        override fun logout() {
            logoutCalls++
            if (logoutFails) throw IllegalStateException("Storage failure")
            isAuthenticated.value = false
        }
    }
}
