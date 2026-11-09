package com.yharim.anotado

import com.example.taskmanager.session.SessionManager
import com.yharim.anotado.data.remote.AuthApiService
import com.yharim.anotado.data.repository.AuthRepositoryImpl
import com.yharim.anotado.domain.repository.AuthException
import com.yharim.anotado.network.AuthInterceptor
import com.yharim.anotado.network.RetrofitClient
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.Assert.*
import retrofit2.http.GET

class AuthNetworkTest {
    private lateinit var server: MockWebServer
    private lateinit var repository: AuthRepositoryImpl
    private val session = MemorySession()

    @Before fun setup() {
        server = MockWebServer()
        server.start()
        val retrofit = RetrofitClient.create(server.url("/").toString(), AuthInterceptor(session))
        repository = AuthRepositoryImpl(retrofit.create(AuthApiService::class.java), session, RetrofitClient.json)
    }
    @After fun tearDown() { server.shutdown() }

    @Test fun sendsDocumentedLoginBodyAndPersistsTokenWithoutBearer() = runTest {
        session.saveToken("old-test-token")
        server.enqueue(jsonResponse(200, """{"token":"new-test-token","user":{"id":1,"name":"María","email":"maria@example.com"}}"""))
        repository.login(" maria@example.com ", "test-password")
        val request = server.takeRequest()
        assertEquals("POST", request.method)
        assertEquals("/login", request.path)
        assertNull(request.getHeader("Authorization"))
        assertEquals("""{"email":"maria@example.com","password":"test-password"}""", request.body.readUtf8())
        assertEquals("new-test-token", session.getToken())
        assertTrue(repository.isAuthenticated.value)
        repository.logout()
        assertNull(session.getToken())
    }

    @Test fun login401DoesNotDeleteAnExistingSession() = runTest {
        session.saveToken("existing-test-token")
        server.enqueue(jsonResponse(401, """{"message":"Invalid credentials"}"""))
        try {
            repository.login("maria@example.com", "wrong")
            fail("Expected credentials error")
        } catch (_: AuthException.Credentials) { }
        assertEquals("existing-test-token", session.getToken())
    }

    @Test fun invalidPayloadNeverCreatesSession() = runTest {
        server.enqueue(jsonResponse(200, """{"unexpected":true}"""))
        try {
            repository.login("maria@example.com", "test-password")
            fail("Expected invalid response")
        } catch (_: AuthException.InvalidResponse) { }
        assertFalse(repository.isAuthenticated.value)
    }

    @Test fun validationErrorsPreserveFieldMessages() = runTest {
        server.enqueue(jsonResponse(400, """{"message":"Datos inválidos","errors":{"email":"Correo inválido"}}"""))
        try {
            repository.login("maria@example.com", "test-password")
            fail("Expected validation error")
        } catch (e: AuthException.Validation) {
            assertEquals("Correo inválido", e.fieldErrors["email"])
        }
    }

    @Test fun protected401AttachesBearerAndClearsSession() = runTest {
        session.saveToken("expired-test-token")
        server.enqueue(MockResponse().setResponseCode(401))
        val api = RetrofitClient.create(server.url("/").toString(), AuthInterceptor(session))
            .create(ProtectedProbe::class.java)
        try { api.tasks() } catch (_: retrofit2.HttpException) { }
        assertEquals("Bearer expired-test-token", server.takeRequest().getHeader("Authorization"))
        assertNull(session.getToken())
        assertFalse(session.isAuthenticated.value)
    }

    @Test fun missingConfigurationNeverAuthenticates() = runTest {
        val unconfigured = AuthRepositoryImpl(null, session, RetrofitClient.json)
        try {
            unconfigured.login("maria@example.com", "test-password")
            fail("Expected unavailable service")
        } catch (_: AuthException.Unavailable) { }
        assertFalse(session.isAuthenticated.value)
    }

    @Test fun registrationSendsOnlyDocumentedFieldsAndPersistsToken() = runTest {
        server.enqueue(jsonResponse(201, """{"token":"registered-test-token","user":{"id":2,"name":"María","email":"maria@example.com"}}"""))
        repository.register(" María ", " maria@example.com ", "test-password")
        val request = server.takeRequest()
        assertEquals("/register", request.path)
        assertEquals("POST", request.method)
        assertNull(request.getHeader("Authorization"))
        assertEquals("""{"name":"María","email":"maria@example.com","password":"test-password"}""", request.body.readUtf8())
        assertEquals("registered-test-token", session.getToken())
        assertTrue(repository.isAuthenticated.value)
    }

    @Test fun duplicateRegistrationMaps409ToEmailWithoutSavingSession() = runTest {
        server.enqueue(jsonResponse(409, """{"message":"Correo repetido"}"""))
        try {
            repository.register("María", "maria@example.com", "test-password")
            fail("Expected duplicate email error")
        } catch (e: AuthException.Validation) {
            assertNotNull(e.fieldErrors["email"])
        }
        assertFalse(repository.isAuthenticated.value)
    }

    interface ProtectedProbe { @GET("tasks") suspend fun tasks(): Unit }

    private fun jsonResponse(code: Int, body: String) = MockResponse()
        .setResponseCode(code).setHeader("Content-Type", "application/json").setBody(body)

    private class MemorySession : SessionManager {
        private var token: String? = null
        override val isAuthenticated = MutableStateFlow(false)
        override fun getUserName(): String? = null
        override fun saveUserName(name: String) {}
        override fun getToken() = token
        override fun saveToken(token: String) { this.token = token; isAuthenticated.value = true }
        override fun clear() { token = null; isAuthenticated.value = false }
        override fun clearIfTokenMatches(token: String) { if (this.token == token) clear() }
    }
}
