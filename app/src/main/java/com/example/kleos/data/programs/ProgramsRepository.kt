package com.example.kleos.data.programs

import com.example.kleos.data.network.ApiClient
import com.example.kleos.data.network.ProgramDto
import com.example.kleos.data.network.ProgramsApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class ProgramsRepository {
    private val api = ApiClient.retrofit.create(ProgramsApi::class.java)

    suspend fun list(q: String?, language: String?, level: String?, university: String?): List<ProgramDto> =
        withContext(Dispatchers.IO) { api.list(q, language, level, university) }

    suspend fun get(id: String): ProgramDto =
        withContext(Dispatchers.IO) { api.get(id) }
}

