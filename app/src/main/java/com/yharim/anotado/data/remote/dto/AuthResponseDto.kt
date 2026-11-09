package com.yharim.anotado.data.remote.dto

import kotlinx.serialization.Serializable

@Serializable
data class UserDto(val id: Long, val name: String, val email: String)

@Serializable
data class AuthResponseDto(val token: String, val user: UserDto)
