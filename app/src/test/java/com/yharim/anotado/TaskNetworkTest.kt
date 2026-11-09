package com.yharim.anotado

import com.example.taskmanager.session.SessionManager
import com.yharim.anotado.data.remote.TaskApiService
import com.yharim.anotado.data.repository.TaskRepositoryImpl
import com.yharim.anotado.network.AuthInterceptor
import com.yharim.anotado.network.RetrofitClient
import com.yharim.anotado.domain.repository.TaskException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.*
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.*
import org.junit.Assert.*

class TaskNetworkTest {
    private lateinit var server: MockWebServer
    private lateinit var repository: TaskRepositoryImpl
    private val session = MemorySession()
    @Before fun setup() {
        server = MockWebServer(); server.start()
        val retrofit = RetrofitClient.create(server.url("/").toString(), AuthInterceptor(session))
        repository = TaskRepositoryImpl(retrofit.create(TaskApiService::class.java), RetrofitClient.json)
    }
    @After fun teardown() { server.shutdown() }
    private fun response(body: String) = MockResponse().setHeader("Content-Type", "application/json").setBody(body)
    private val taskJson = """{"id":1,"title":"Estudiar","description":null,"priority":3,"dueDate":null,"completed":true,"category":"General"}"""

    @Test fun listUsesBearerAndReturnsDomainModels() = runTest {
        server.enqueue(response("[$taskJson]"))
        val tasks = repository.getTasks()
        val request = server.takeRequest()
        assertEquals("/tasks", request.path)
        assertEquals("Bearer test-token", request.getHeader("Authorization"))
        assertEquals("Estudiar", tasks.single().title)
        assertTrue(tasks.single().completed)
    }
    @Test fun completionSendsFullPutIncludingNullsAndFalse() = runTest {
        server.enqueue(response(taskJson))
        server.enqueue(response(taskJson.replace("true", "false")))
        assertFalse(repository.setCompleted(1, false).completed)
        assertEquals("GET", server.takeRequest().method)
        val update = server.takeRequest()
        assertEquals("PUT", update.method)
        assertEquals("/tasks/1", update.path)
        val body = Json.parseToJsonElement(update.body.readUtf8()).jsonObject
        assertEquals(setOf("title", "description", "priority", "dueDate", "completed", "category"), body.keys)
        assertEquals(JsonNull, body["description"])
        assertEquals(JsonNull, body["dueDate"])
        assertEquals(JsonPrimitive(false), body["completed"])
    }
    @Test fun protected401InvalidatesSessionAndReportsFailure() = runTest {
        server.enqueue(MockResponse().setResponseCode(401))
        try { repository.getTasks(); fail("Expected task failure") } catch (_: TaskException) { }
        assertFalse(session.isAuthenticated.value)
    }
    @Test fun createPostsOnlyWritableFieldsAndReturnsServerId() = runTest {
        server.enqueue(response(taskJson).setResponseCode(201))
        val task = repository.createTask(com.yharim.anotado.domain.model.TaskDraft("Estudiar"))
        assertEquals(1L, task.id)
        val request = server.takeRequest()
        assertEquals("POST", request.method)
        assertEquals("/tasks", request.path)
        val body = Json.parseToJsonElement(request.body.readUtf8()).jsonObject
        assertEquals(setOf("title", "description", "priority", "dueDate", "completed", "category"), body.keys)
        assertEquals(JsonPrimitive(false), body["completed"])
        assertEquals(JsonNull, body["dueDate"])
    }

    @Test fun create400PreservesFieldErrors() = runTest {
        server.enqueue(response("""{"message":"Inválido","errors":{"title":"Título requerido"}}""").setResponseCode(400))
        try {
            repository.createTask(com.yharim.anotado.domain.model.TaskDraft("Tarea"))
            fail("Expected validation error")
        } catch (e: TaskException) { assertEquals("Título requerido", e.fieldErrors["title"]) }
    }

    private class MemorySession : SessionManager {
        private var token: String? = "test-token"
        override val isAuthenticated = MutableStateFlow(true)
        override fun getUserName(): String? = null
        override fun saveUserName(name: String) {}
        override fun getToken() = token
        override fun saveToken(token: String) { this.token = token; isAuthenticated.value = true }
        override fun clear() { token = null; isAuthenticated.value = false }
        override fun clearIfTokenMatches(token: String) { if (this.token == token) clear() }
    }
}
