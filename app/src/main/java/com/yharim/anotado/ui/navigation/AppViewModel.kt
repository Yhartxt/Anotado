package com.example.taskmanager.ui.navigation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yharim.anotado.domain.repository.AuthRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class AppUiState(val isAuthenticated: Boolean)

class AppViewModel(authRepository: AuthRepository) : ViewModel() {
    private val _uiState = MutableStateFlow(AppUiState(authRepository.isAuthenticated.value))
    val uiState: StateFlow<AppUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            authRepository.isAuthenticated.collect { authenticated ->
                _uiState.value = AppUiState(authenticated)
            }
        }
    }
}
