package com.example.taskmanager.session

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

interface SessionManager {
    val isAuthenticated: StateFlow<Boolean>
    fun getUserName(): String?
    fun saveUserName(name: String)
    fun getToken(): String?
    fun saveToken(token: String)
    fun clear()
    fun clearIfTokenMatches(token: String)
}

class SharedPrefsSessionManager(context: Context) : SessionManager {
    private val prefs = context.applicationContext.getSharedPreferences("anotado_session", Context.MODE_PRIVATE)
    private val authenticated = MutableStateFlow(!getToken().isNullOrBlank())
    override val isAuthenticated = authenticated.asStateFlow()

    override fun getUserName(): String? = prefs.getString("user_name", null)
    override fun saveUserName(name: String) { prefs.edit().putString("user_name", name).apply() }

    @Synchronized
    override fun getToken(): String? = prefs.getString("jwt_token", null)

    @Synchronized
    override fun saveToken(token: String) {
        require(token.isNotBlank())
        prefs.edit().putString("jwt_token", token).apply()
        authenticated.value = true
    }

    @Synchronized
    override fun clear() {
        prefs.edit().clear().apply()
        authenticated.value = false
    }

    @Synchronized
    override fun clearIfTokenMatches(token: String) {
        if (getToken() == token) clear()
    }
}
