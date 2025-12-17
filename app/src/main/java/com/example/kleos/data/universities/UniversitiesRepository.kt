package com.example.kleos.data.universities

import com.example.kleos.data.network.ApiClient
import com.example.kleos.data.network.UniversitiesApi
import com.example.kleos.data.network.UniversityDto
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class UniversitiesRepository {
    private val api = ApiClient.retrofit.create(UniversitiesApi::class.java)

    suspend fun list(): List<UniversityDto> =
        withContext(Dispatchers.IO) { api.list() }

    suspend fun get(id: String): UniversityDto =
        withContext(Dispatchers.IO) { api.get(id) }
}

