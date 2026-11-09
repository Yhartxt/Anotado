package com.yharim.anotado.data.remote.dto

import kotlinx.serialization.Serializable

@Serializable
data class ErrorResponseDto(val message: String = "Hay datos inválidos.", val errors: Map<String, String> = emptyMap())
