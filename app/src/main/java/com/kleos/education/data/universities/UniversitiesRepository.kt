package com.kleos.education.data.universities

import com.kleos.education.data.network.ApiClient
import com.kleos.education.data.network.UniversitiesApi
import com.kleos.education.data.network.UniversityDto
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class UniversitiesRepository {
    private val api = ApiClient.retrofit.create(UniversitiesApi::class.java)

    suspend fun list(): List<UniversityDto> =
        withContext(Dispatchers.IO) { api.list() }

    suspend fun get(id: String): UniversityDto =
        withContext(Dispatchers.IO) { api.get(id) }
}


