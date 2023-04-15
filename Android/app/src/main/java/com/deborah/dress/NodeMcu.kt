package com.deborah.dress

import retrofit2.Response
import retrofit2.http.GET

interface NodeMcu {

    @GET("/")
    suspend fun connect(): Response<String>
}