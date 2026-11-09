package com.yharim.anotado.data.remote.dto

import kotlinx.serialization.Serializable

@Serializable
data class RegisterRequestDto(val name: String, val email: String, val password: String)
