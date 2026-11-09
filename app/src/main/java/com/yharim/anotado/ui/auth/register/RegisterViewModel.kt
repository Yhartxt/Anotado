package com.yharim.anotado.ui.auth.register

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yharim.anotado.domain.repository.AuthException
import com.yharim.anotado.domain.repository.AuthRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class RegisterUiState(
    val name: String = "",
    val email: String = "",
    val password: String = "",
    val confirmPassword: String = "",
    val passwordVisible: Boolean = false,
    val confirmationVisible: Boolean = false,
    val nameError: String? = null,
    val emailError: String? = null,
    val passwordError: String? = null,
    val confirmationError: String? = null,
    val generalError: String? = null,
    val isLoading: Boolean = false,
    val registerSuccess: Boolean = false
)

class RegisterViewModel(private val authRepository: AuthRepository) : ViewModel() {
    private val _uiState = MutableStateFlow(RegisterUiState())
    val uiState = _uiState.asStateFlow()

    fun onNameChange(value: String) = edit { copy(name = value, nameError = null) }
    fun onEmailChange(value: String) = edit { copy(email = value, emailError = null) }
    fun onPasswordChange(value: String) = edit {
        copy(password = value, passwordError = null, confirmationError = null)
    }
    fun onConfirmationChange(value: String) = edit {
        copy(confirmPassword = value, confirmationError = null)
    }
    fun onTogglePassword() = edit { copy(passwordVisible = !passwordVisible) }
    fun onToggleConfirmation() = edit { copy(confirmationVisible = !confirmationVisible) }

    private fun edit(transform: RegisterUiState.() -> RegisterUiState) {
        _uiState.update {
            if (it.isLoading || it.registerSuccess) it else it.transform().copy(generalError = null)
        }
    }

    fun onRegisterClick() {
        val state = _uiState.value
        if (state.isLoading || state.registerSuccess) return
        val nameError = if (state.name.isBlank()) "Ingresa tu nombre" else null
        val emailError = when {
            state.email.isBlank() -> "Ingresa tu correo"
            !Regex("[^\\s@]+@[^\\s@]+\\.[^\\s@]+").matches(state.email.trim()) -> "El correo no tiene un formato válido"
            else -> null
        }
        val passwordError = when {
            state.password.isBlank() -> "Ingresa una contraseña"
            state.password.length < 8 -> "Usa al menos 8 caracteres"
            else -> null
        }
        val confirmationError = when {
            state.confirmPassword.isEmpty() -> "Confirma tu contraseña"
            state.password != state.confirmPassword -> "Las contraseñas no coinciden"
            else -> null
        }
        _uiState.update { it.copy(nameError = nameError, emailError = emailError,
            passwordError = passwordError, confirmationError = confirmationError, generalError = null) }
        if (listOf(nameError, emailError, passwordError, confirmationError).any { it != null }) return

        _uiState.update { it.copy(isLoading = true) }
        viewModelScope.launch {
            try {
                authRepository.register(state.name.trim(), state.email.trim(), state.password)
                _uiState.update { it.copy(registerSuccess = true, password = "", confirmPassword = "",
                    passwordVisible = false, confirmationVisible = false) }
            } catch (e: CancellationException) {
                throw e
            } catch (e: AuthException.Validation) {
                _uiState.update { it.copy(
                    nameError = e.fieldErrors["name"], emailError = e.fieldErrors["email"],
                    passwordError = e.fieldErrors["password"],
                    generalError = if (e.fieldErrors.keys.none { key -> key in setOf("name", "email", "password") }) e.message else null
                ) }
            } catch (e: AuthException) {
                _uiState.update { it.copy(generalError = e.message) }
            } catch (_: Exception) {
                _uiState.update { it.copy(generalError = "No pudimos crear tu cuenta. Inténtalo de nuevo.") }
            } finally {
                _uiState.update { it.copy(isLoading = false) }
            }
        }
    }
}
