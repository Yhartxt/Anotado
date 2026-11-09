package com.yharim.anotado.data.repository

import com.example.taskmanager.session.SessionManager
import com.yharim.anotado.data.remote.AuthApiService
import com.yharim.anotado.data.remote.dto.ErrorResponseDto
import com.yharim.anotado.data.remote.dto.AuthResponseDto
import com.yharim.anotado.data.remote.dto.RegisterRequestDto
import com.yharim.anotado.data.remote.dto.LoginRequestDto
import com.yharim.anotado.domain.repository.AuthException
import com.yharim.anotado.domain.repository.AuthRepository
import kotlinx.coroutines.CancellationException
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import retrofit2.HttpException
import java.io.IOException

class AuthRepositoryImpl(
    private val api: AuthApiService?,
    private val session: SessionManager,
    private val json: Json
) : AuthRepository {
    override val isAuthenticated = session.isAuthenticated

    override suspend fun login(email: String, password: String) = authenticate {
        it.login(LoginRequestDto(email.trim(), password))
    }

    override suspend fun register(name: String, email: String, password: String) = authenticate {
        it.register(RegisterRequestDto(name.trim(), email.trim(), password))
    }

    private suspend fun authenticate(request: suspend (AuthApiService) -> AuthResponseDto) {
        val service = api ?: throw AuthException.Unavailable()
        try {
            val response = request(service)
            if (response.token.isBlank() || response.token.any { it.isWhitespace() }) {
                throw AuthException.InvalidResponse()
            }
            session.saveUserName(response.user.name)
            session.saveToken(response.token)
        } catch (e: CancellationException) {
            throw e
        } catch (e: HttpException) {
            throw when (e.code()) {
                401 -> AuthException.Credentials()
                400, 409, 422 -> {
                    val error = try {
                        e.response()?.errorBody()?.use { body ->
                            json.decodeFromString<ErrorResponseDto>(body.string())
                        }
                    } catch (_: SerializationException) { null }
                      catch (_: IOException) { null }
                    val fields = error?.errors.orEmpty().ifEmpty {
                        if (e.code() == 409) mapOf("email" to "Este correo ya tiene una cuenta.") else emptyMap()
                    }
                    AuthException.Validation(fields, error?.message ?: "Revisa los datos e inténtalo de nuevo.")
                }
                else -> AuthException.Server()
            }
        } catch (e: SerializationException) {
            throw AuthException.InvalidResponse()
        } catch (e: IOException) {
            throw AuthException.Connection()
        }
    }

    override fun getUserName(): String? = session.getUserName()

    override fun logout() = session.clear()
}
