package com.yharim.anotado.di

import android.content.Context
import com.yharim.anotado.domain.repository.TaskRepository
import com.yharim.anotado.data.repository.TaskRepositoryImpl
import com.yharim.anotado.data.remote.TaskApiService
import com.example.taskmanager.session.SharedPrefsSessionManager
import com.yharim.anotado.BuildConfig
import com.yharim.anotado.data.remote.AuthApiService
import com.yharim.anotado.data.repository.AuthRepositoryImpl
import com.yharim.anotado.domain.repository.AuthRepository
import com.yharim.anotado.network.AuthInterceptor
import com.yharim.anotado.network.RetrofitClient

class AppContainer(context: Context) {
    private val sessionManager = SharedPrefsSessionManager(context)
    private val retrofit = BuildConfig.API_BASE_URL.takeIf { it.isNotBlank() }
        ?.let { RetrofitClient.create(it, AuthInterceptor(sessionManager)) }
    val taskRepository: TaskRepository = TaskRepositoryImpl(retrofit?.create(TaskApiService::class.java), RetrofitClient.json)
    val authRepository: AuthRepository = AuthRepositoryImpl(
        retrofit?.create(AuthApiService::class.java), sessionManager, RetrofitClient.json
    )
}
