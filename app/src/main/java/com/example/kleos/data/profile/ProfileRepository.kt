package com.example.kleos.data.profile

import com.example.kleos.data.network.ApiClient
import com.example.kleos.data.network.UpdateProfileRequest
import com.example.kleos.data.network.UserProfileDto
import com.example.kleos.data.network.UsersApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class ProfileRepository {
    private val api = ApiClient.retrofit.create(UsersApi::class.java)

    suspend fun getProfile(): UserProfileDto = withContext(Dispatchers.IO) {
        api.getProfile()
    }

    suspend fun updateProfile(
        fullName: String? = null,
        phone: String? = null,
        course: String? = null,
        speciality: String? = null,
        status: String? = null,
        university: String? = null,
        payment: String? = null,
        penalties: String? = null,
        notes: String? = null
    ): Result<Unit> = runCatching {
        withContext(Dispatchers.IO) {
            api.updateProfile(
                UpdateProfileRequest(
                    fullName = fullName,
                    phone = phone,
                    course = course,
                    speciality = speciality,
                    status = status,
                    university = university,
                    payment = payment,
                    penalties = penalties,
                    notes = notes
                )
            )
        }
    }
}

