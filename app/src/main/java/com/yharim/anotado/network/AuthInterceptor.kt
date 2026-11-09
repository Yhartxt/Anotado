package com.yharim.anotado.network

import com.example.taskmanager.session.SessionManager
import okhttp3.Interceptor
import okhttp3.Response

class AuthInterceptor(private val session: SessionManager) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val original = chain.request()
        val protected = "tasks" in original.url.pathSegments
        val token = if (protected) session.getToken() else null
        val request = if (!token.isNullOrBlank()) {
            original.newBuilder().header("Authorization", "Bearer $token").build()
        } else original
        return chain.proceed(request).also { response ->
            if (protected && response.code == 401 && token != null) {
                session.clearIfTokenMatches(token)
            }
        }
    }
}
