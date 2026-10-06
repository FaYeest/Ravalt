package com.ravalt.app.core.network

import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Headers
import retrofit2.http.Path

interface HaveIBeenPwnedApiService {

    @GET("range/{hashPrefix}")
    @Headers("User-Agent: Ravalt-Password-Manager-Android")
    suspend fun checkHashRange(
        @Path("hashPrefix") hashPrefix: String
    ): Response<ResponseBody>
}
