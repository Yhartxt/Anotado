package com.yharim.anotado.di

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.yharim.anotado.ui.task.TaskViewModel
import com.example.taskmanager.ui.navigation.AppViewModel
import com.yharim.anotado.ui.home.HomeViewModel
import com.example.taskmanager.ui.auth.login.LoginViewModel
import com.yharim.anotado.ui.auth.register.RegisterViewModel

class ViewModelFactory(private val container: AppContainer) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass == LoginViewModel::class.java) {
            @Suppress("UNCHECKED_CAST")
            return LoginViewModel(container.authRepository) as T
        }
        if (modelClass == RegisterViewModel::class.java) {
            @Suppress("UNCHECKED_CAST")
            return RegisterViewModel(container.authRepository) as T
        }
        if (modelClass == AppViewModel::class.java) {
            @Suppress("UNCHECKED_CAST")
            return AppViewModel(container.authRepository) as T
        }
        if (modelClass == HomeViewModel::class.java) {
            @Suppress("UNCHECKED_CAST")
            return HomeViewModel(container.authRepository, container.taskRepository) as T
        }
        if (modelClass == TaskViewModel::class.java) {
            @Suppress("UNCHECKED_CAST")
            return TaskViewModel(container.taskRepository) as T
        }
        throw IllegalArgumentException("ViewModel no registrado: ${modelClass.name}")
    }
}
