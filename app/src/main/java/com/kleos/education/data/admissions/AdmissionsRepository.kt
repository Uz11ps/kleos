package com.kleos.education.data.admissions

import android.content.Context
import android.content.SharedPreferences
import com.kleos.education.data.model.AdmissionApplication
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

interface AdmissionsRepository {
    fun submit(application: AdmissionApplication)
    fun list(): List<AdmissionApplication>

    class Local(context: Context) : AdmissionsRepository {
        private val prefs: SharedPreferences =
            context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

        override fun submit(application: AdmissionApplication) {
            val current = list().toMutableList()
            current.add(application.copy(id = UUID.randomUUID().toString()))
            val json = JSONArray()
            current.forEach { a ->
                val obj = JSONObject()
                obj.put("id", a.id)
                obj.put("firstName", a.firstName)
                obj.put("lastName", a.lastName)
                obj.put("patronymic", a.patronymic ?: "")
                obj.put("phone", a.phone)
                obj.put("email", a.email)
                obj.put("program", a.program)
                obj.put("comment", a.comment ?: "")
                json.put(obj)
            }
            prefs.edit().putString(KEY_APPLICATIONS, json.toString()).apply()
        }

        override fun list(): List<AdmissionApplication> {
            val raw = prefs.getString(KEY_APPLICATIONS, null) ?: return emptyList()
            val array = JSONArray(raw)
            val result = mutableListOf<AdmissionApplication>()
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                result.add(
                    AdmissionApplication(
                        id = obj.optString("id"),
                        firstName = obj.optString("firstName", obj.optString("fullName", "")),
                        lastName = obj.optString("lastName", ""),
                        patronymic = obj.optString("patronymic").takeIf { it.isNotEmpty() },
                        phone = obj.optString("phone"),
                        email = obj.optString("email"),
                        program = obj.optString("program"),
                        comment = obj.optString("comment").takeIf { it.isNotEmpty() }
                    )
                )
            }
            return result
        }

        companion object {
            private const val PREFS = "kleos_admissions_prefs"
            private const val KEY_APPLICATIONS = "applications"
        }
    }

    class Http(private val context: Context) : AdmissionsRepository {
        private val scope = CoroutineScope(Dispatchers.IO)
        override fun submit(application: AdmissionApplication) {
            val api = com.kleos.education.data.network.ApiClient.retrofit
                .create(com.kleos.education.data.network.AdmissionsApi::class.java)
            val body = com.kleos.education.data.network.AdmissionRequest(
                firstName = application.firstName,
                lastName = application.lastName,
                patronymic = application.patronymic,
                phone = application.phone,
                email = application.email,
                dateOfBirth = application.dateOfBirth,
                placeOfBirth = application.placeOfBirth,
                nationality = application.nationality,
                passportNumber = application.passportNumber,
                passportIssue = application.passportIssue,
                passportExpiry = application.passportExpiry,
                visaCity = application.visaCity,
                program = application.program,
                comment = application.comment
            )
            // fire-and-forget
            scope.launch {
                kotlin.runCatching { api.create(body) }
            }
        }

        override fun list(): List<AdmissionApplication> = emptyList()
    }
}


