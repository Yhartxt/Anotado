package com.yharim.anotado.domain.repository

import kotlinx.coroutines.flow.StateFlow

interface AuthRepository {
    val isAuthenticated: StateFlow<Boolean>
    suspend fun login(email: String, password: String)
    suspend fun register(name: String, email: String, password: String)
    fun getUserName(): String?
    fun logout()
}

sealed class AuthException(message: String) : Exception(message) {
    class Validation(val fieldErrors: Map<String, String>, message: String) : AuthException(message)
    class Credentials : AuthException("El correo o la contraseña son incorrectos.")
    class Connection : AuthException("No pudimos conectar. Revisa tu conexión e inténtalo de nuevo.")
    class Unavailable : AuthException("El servicio de acceso no está disponible en este momento. Inténtalo más tarde.")
    class InvalidResponse : AuthException("No pudimos completar la solicitud. Inténtalo de nuevo.")
    class Server : AuthException("El servicio no está disponible. Inténtalo en unos minutos.")
}
