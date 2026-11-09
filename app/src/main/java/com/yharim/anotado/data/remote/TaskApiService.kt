package com.yharim.anotado.data.remote

import com.yharim.anotado.data.remote.dto.TaskDto
import com.yharim.anotado.data.remote.dto.TaskRequestDto
import retrofit2.http.*

interface TaskApiService {
    @POST("tasks") suspend fun createTask(@Body request: TaskRequestDto): TaskDto
    @GET("tasks") suspend fun getTasks(): List<TaskDto>
    @GET("tasks/{id}") suspend fun getTask(@Path("id") id: Long): TaskDto
    @PUT("tasks/{id}") suspend fun updateTask(@Path("id") id: Long, @Body request: TaskRequestDto): TaskDto
}
