package com.yharim.anotado

import com.yharim.anotado.ui.auth.register.RegisterViewModel
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
class RegisterViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    private val repo = FakeAuthRepository()
    private lateinit var vm: RegisterViewModel
    @Before fun setup() { Dispatchers.setMain(dispatcher); vm = RegisterViewModel(repo) }
    @After fun teardown() { Dispatchers.resetMain() }

    @Test fun emptyFieldsNeverReachRepository() = runTest(dispatcher) {
        vm.onRegisterClick(); advanceUntilIdle()
        assertNotNull(vm.uiState.value.nameError)
        assertNotNull(vm.uiState.value.emailError)
        assertNotNull(vm.uiState.value.passwordError)
        assertNotNull(vm.uiState.value.confirmationError)
        assertEquals(0, repo.calls)
    }
    @Test fun malformedEmailAndShortPasswordAreRejected() = runTest(dispatcher) {
        ready(); vm.onEmailChange("incorrecto"); vm.onPasswordChange("corta")
        vm.onConfirmationChange("corta"); vm.onRegisterClick(); advanceUntilIdle()
        assertNotNull(vm.uiState.value.emailError)
        assertNotNull(vm.uiState.value.passwordError)
        assertEquals(0, repo.calls)
    }
    @Test fun blankNameAndWhitespacePasswordAreRejected() = runTest(dispatcher) {
        ready(); vm.onNameChange("   "); vm.onPasswordChange("        ")
        vm.onConfirmationChange("        "); vm.onRegisterClick(); advanceUntilIdle()
        assertNotNull(vm.uiState.value.nameError)
        assertNotNull(vm.uiState.value.passwordError)
        assertEquals(0, repo.calls)
    }
    @Test fun mismatchingConfirmationNeverReachesRepository() = runTest(dispatcher) {
        ready(); vm.onConfirmationChange("otra clave"); vm.onRegisterClick(); advanceUntilIdle()
        assertNotNull(vm.uiState.value.confirmationError)
        assertEquals(0, repo.calls)
    }
    @Test fun successTrimsNameEmailPreservesPasswordAndClearsSecrets() = runTest(dispatcher) {
        ready(); vm.onTogglePassword(); vm.onToggleConfirmation()
        vm.onRegisterClick(); advanceUntilIdle()
        assertEquals(listOf("María", "maria@example.com", " secreto123 "), repo.arguments)
        assertTrue(vm.uiState.value.registerSuccess)
        assertEquals("", vm.uiState.value.password)
        assertEquals("", vm.uiState.value.confirmPassword)
        assertFalse(vm.uiState.value.passwordVisible)
        assertFalse(vm.uiState.value.confirmationVisible)
        assertFalse(vm.uiState.value.isLoading)
    }
    @Test fun duplicateClicksAndEditsDuringRequestAreIgnored() = runTest(dispatcher) {
        ready(); vm.onRegisterClick(); vm.onRegisterClick(); vm.onNameChange("Otro")
        assertTrue(vm.uiState.value.isLoading)
        advanceUntilIdle(); vm.onRegisterClick()
        assertEquals(1, repo.calls)
        assertEquals("María", repo.arguments[0])
    }
    @Test fun duplicateEmailShowsFieldErrorAndAllowsRetry() = runTest(dispatcher) {
        ready(); repo.failure = AuthException.Validation(mapOf("email" to "Correo ya registrado"), "Duplicado")
        vm.onRegisterClick(); advanceUntilIdle()
        assertEquals("Correo ya registrado", vm.uiState.value.emailError)
        assertFalse(vm.uiState.value.registerSuccess)
        assertFalse(vm.uiState.value.isLoading)
        vm.onEmailChange("otro@example.com")
        assertNull(vm.uiState.value.emailError)
        repo.failure = null; vm.onRegisterClick(); advanceUntilIdle()
        assertTrue(vm.uiState.value.registerSuccess)
    }
    @Test fun connectionFailureDoesNotAuthenticate() = runTest(dispatcher) {
        ready(); repo.failure = AuthException.Connection()
        vm.onRegisterClick(); advanceUntilIdle()
        assertNotNull(vm.uiState.value.generalError)
        assertFalse(vm.uiState.value.registerSuccess)
        assertFalse(vm.uiState.value.isLoading)
    }
    @Test fun cancellationDoesNotBecomeErrorOrSuccess() = runTest(dispatcher) {
        ready(); repo.failure = CancellationException()
        vm.onRegisterClick(); advanceUntilIdle()
        assertNull(vm.uiState.value.generalError)
        assertFalse(vm.uiState.value.registerSuccess)
        assertFalse(vm.uiState.value.isLoading)
    }
    @Test fun visibilityIsIndependentAndOwnedByViewModel() {
        vm.onTogglePassword()
        assertTrue(vm.uiState.value.passwordVisible)
        assertFalse(vm.uiState.value.confirmationVisible)
        vm.onToggleConfirmation()
        assertTrue(vm.uiState.value.confirmationVisible)
    }
    private fun ready() {
        vm.onNameChange("  María  "); vm.onEmailChange(" maria@example.com ")
        vm.onPasswordChange(" secreto123 "); vm.onConfirmationChange(" secreto123 ")
    }
    private class FakeAuthRepository : AuthRepository {
        override val isAuthenticated = MutableStateFlow(false)
        var calls = 0
        var arguments = emptyList<String>()
        var failure: Exception? = null
        override suspend fun login(email: String, password: String) = error("Unexpected login")
        override suspend fun register(name: String, email: String, password: String) {
            calls++; arguments = listOf(name, email, password)
            failure?.let { throw it }
        }
        override fun getUserName(): String? = null
        override fun logout() { isAuthenticated.value = false }
    }
}
