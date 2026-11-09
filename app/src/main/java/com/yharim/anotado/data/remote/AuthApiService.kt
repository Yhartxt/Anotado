package com.yharim.anotado.data.remote

import com.yharim.anotado.data.remote.dto.AuthResponseDto
import com.yharim.anotado.data.remote.dto.LoginRequestDto
import com.yharim.anotado.data.remote.dto.RegisterRequestDto
import retrofit2.http.Body
import retrofit2.http.POST

interface AuthApiService {
    @POST("register")
    suspend fun register(@Body request: RegisterRequestDto): AuthResponseDto

    @POST("login")
    suspend fun login(@Body request: LoginRequestDto): AuthResponseDto
}
