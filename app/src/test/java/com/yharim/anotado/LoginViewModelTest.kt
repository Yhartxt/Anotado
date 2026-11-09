package com.yharim.anotado

import com.example.taskmanager.ui.auth.login.LoginViewModel
import com.yharim.anotado.domain.repository.AuthException
import com.yharim.anotado.domain.repository.AuthRepository
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.Assert.*

@OptIn(ExperimentalCoroutinesApi::class)
class LoginViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    private val repo = FakeAuthRepository()
    private lateinit var vm: LoginViewModel

    @Before fun setup() {
        Dispatchers.setMain(dispatcher)
        vm = LoginViewModel(repo)
    }
    @After fun tearDown() { Dispatchers.resetMain() }

    @Test fun emptyFieldsDoNotCallApi() = runTest(dispatcher) {
        vm.onLoginClick()
        advanceUntilIdle()
        assertNotNull(vm.uiState.value.emailError)
        assertNotNull(vm.uiState.value.passwordError)
        assertEquals(0, repo.calls)
    }

    @Test fun invalidEmailDoesNotCallApi() = runTest(dispatcher) {
        vm.onEmailChange("correo-invalido")
        vm.onPasswordChange("secreto123")
        vm.onLoginClick()
        advanceUntilIdle()
        assertNotNull(vm.uiState.value.emailError)
        assertEquals(0, repo.calls)
    }

    @Test fun trimsEmailPreservesPasswordAndClearsPasswordAfterSuccess() = runTest(dispatcher) {
        vm.onEmailChange("  maria@example.com  ")
        vm.onPasswordChange(" secreto123 ")
        vm.onLoginClick()
        advanceUntilIdle()
        assertEquals("maria@example.com", repo.email)
        assertEquals(" secreto123 ", repo.password)
        assertTrue(vm.uiState.value.loginSuccess)
        assertEquals("", vm.uiState.value.password)
        assertFalse(vm.uiState.value.isLoading)
    }

    @Test fun repeatedClicksOnlySendOneRequest() = runTest(dispatcher) {
        ready()
        vm.onLoginClick()
        vm.onLoginClick()
        assertTrue(vm.uiState.value.isLoading)
        advanceUntilIdle()
        vm.onLoginClick()
        assertEquals(1, repo.calls)
    }

    @Test fun credentialsFailureAllowsRetry() = runTest(dispatcher) {
        ready()
        repo.failure = AuthException.Credentials()
        vm.onLoginClick()
        advanceUntilIdle()
        assertFalse(vm.uiState.value.loginSuccess)
        assertFalse(vm.uiState.value.isLoading)
        assertNotNull(vm.uiState.value.generalError)
        repo.failure = null
        vm.onLoginClick()
        advanceUntilIdle()
        assertTrue(vm.uiState.value.loginSuccess)
        assertNull(vm.uiState.value.generalError)
    }

    @Test fun serverFieldErrorsAreDisplayedAndClearedWhenEdited() = runTest(dispatcher) {
        ready()
        repo.failure = AuthException.Validation(mapOf("email" to "Revisa el correo"), "Datos inválidos")
        vm.onLoginClick()
        advanceUntilIdle()
        assertEquals("Revisa el correo", vm.uiState.value.emailError)
        vm.onEmailChange("otra@example.com")
        assertNull(vm.uiState.value.emailError)
    }

    @Test fun cancellationNeverShowsSuccessOrNetworkError() = runTest(dispatcher) {
        ready()
        repo.failure = CancellationException()
        vm.onLoginClick()
        advanceUntilIdle()
        assertFalse(vm.uiState.value.isLoading)
        assertFalse(vm.uiState.value.loginSuccess)
        assertNull(vm.uiState.value.generalError)
    }

    @Test fun passwordVisibilityIsOwnedByViewModelAndResetAfterSuccess() = runTest(dispatcher) {
        ready()
        vm.onTogglePassword()
        assertTrue(vm.uiState.value.passwordVisible)
        vm.onLoginClick()
        advanceUntilIdle()
        assertFalse(vm.uiState.value.passwordVisible)
        vm.onTogglePassword()
        assertFalse(vm.uiState.value.passwordVisible)
    }

    @Test fun visibilityDoesNotChangeDuringRequestOrClearServerErrors() = runTest(dispatcher) {
        ready()
        repo.failure = AuthException.Credentials()
        vm.onLoginClick()
        vm.onTogglePassword()
        assertFalse(vm.uiState.value.passwordVisible)
        advanceUntilIdle()
        val error = vm.uiState.value.generalError
        vm.onTogglePassword()
        assertTrue(vm.uiState.value.passwordVisible)
        assertEquals(error, vm.uiState.value.generalError)
    }

    private fun ready() {
        vm.onEmailChange("maria@example.com")
        vm.onPasswordChange("secreto123")
    }

    private class FakeAuthRepository : AuthRepository {
        override val isAuthenticated = MutableStateFlow(false)
        var calls = 0
        var email = ""
        var password = ""
        var failure: Exception? = null
        override suspend fun login(email: String, password: String) {
            calls++
            this.email = email
            this.password = password
            failure?.let { throw it }
        }
        override suspend fun register(name: String, email: String, password: String) = error("Unexpected registration")
        override fun getUserName(): String? = null
        override fun logout() { isAuthenticated.value = false }
    }
}
