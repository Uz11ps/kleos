package com.kleos.education.data.programs

import com.kleos.education.data.network.ApiClient
import com.kleos.education.data.network.ProgramDto
import com.kleos.education.data.network.ProgramsApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class ProgramsRepository {
    private val api = ApiClient.retrofit.create(ProgramsApi::class.java)

    suspend fun list(q: String?, language: String?, level: String?, university: String?, universityId: String? = null): List<ProgramDto> =
        withContext(Dispatchers.IO) {
            try {
                android.util.Log.d("ProgramsRepository", "Calling API with params: q=$q, language=$language, level=$level, university=$university, universityId=$universityId")
                val result = api.list(q, language, level, university, universityId)
                android.util.Log.d("ProgramsRepository", "API returned ${result.size} programs")
                result.forEachIndexed { index, program ->
                    android.util.Log.d("ProgramsRepository", "Program $index: ${program.title}, language=${program.language}, level=${program.level}, university=${program.university}")
                }
                result
            } catch (e: Exception) {
                android.util.Log.e("ProgramsRepository", "Error calling API", e)
                e.printStackTrace()
                throw e
            }
        }

    suspend fun get(id: String): ProgramDto =
        withContext(Dispatchers.IO) { api.get(id) }
}


