package com.example.taskmanager.ui.auth.login

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yharim.anotado.domain.repository.AuthException
import com.yharim.anotado.domain.repository.AuthRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.CancellationException

data class LoginUiState(
    val email: String = "",
    val password: String = "",
    val passwordVisible: Boolean = false,
    val emailError: String? = null,
    val passwordError: String? = null,
    val generalError: String? = null,
    val isLoading: Boolean = false,
    val loginSuccess: Boolean = false
)

class LoginViewModel(
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(LoginUiState())
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

    fun onEmailChange(value: String) {
        if (_uiState.value.isLoading || _uiState.value.loginSuccess) return
        _uiState.update { it.copy(email = value, emailError = null, generalError = null) }
    }

    fun onPasswordChange(value: String) {
        if (_uiState.value.isLoading || _uiState.value.loginSuccess) return
        _uiState.update { it.copy(password = value, passwordError = null, generalError = null) }
    }

    fun onTogglePassword() {
        _uiState.update {
            if (it.isLoading || it.loginSuccess) it else it.copy(passwordVisible = !it.passwordVisible)
        }
    }

    fun onLoginClick() {
        val current = _uiState.value
        if (current.isLoading || current.loginSuccess) return

        val emailError = validateEmail(current.email)
        val passwordError = if (current.password.isEmpty()) "Ingresa tu contraseña" else null
        if (emailError != null || passwordError != null) {
            _uiState.update { it.copy(emailError = emailError, passwordError = passwordError) }
            return
        }

        _uiState.update { it.copy(isLoading = true, generalError = null) }

        viewModelScope.launch {
            try {
                authRepository.login(current.email.trim(), current.password)
                _uiState.update { it.copy(isLoading = false, loginSuccess = true, password = "", passwordVisible = false) }
            } catch (e: CancellationException) {
                throw e
            } catch (e: AuthException) {
                _uiState.update { it.withError(e) }
            } catch (_: Exception) {
                _uiState.update { it.copy(generalError = "No pudimos iniciar sesión. Inténtalo de nuevo.") }
            } finally {
                _uiState.update { it.copy(isLoading = false) }
            }
        }
    }

    private fun validateEmail(email: String): String? = when {
        email.isBlank() -> "Ingresa tu correo"
        !Regex("[^\\s@]+@[^\\s@]+\\.[^\\s@]+").matches(email.trim()) -> "El correo no tiene un formato válido"
        else -> null
    }

    private fun LoginUiState.withError(error: AuthException): LoginUiState = when (error) {
        is AuthException.Validation -> copy(
            isLoading = false,
            emailError = error.fieldErrors["email"],
            passwordError = error.fieldErrors["password"],
            generalError = if (error.fieldErrors.keys.none { it == "email" || it == "password" }) error.message else null
        )
        else -> copy(isLoading = false, generalError = error.message)
    }
}