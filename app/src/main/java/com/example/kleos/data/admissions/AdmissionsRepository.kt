package com.example.kleos.data.admissions

import android.content.Context
import android.content.SharedPreferences
import com.example.kleos.data.model.AdmissionApplication
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
                obj.put("fullName", a.fullName)
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
                        fullName = obj.optString("fullName"),
                        phone = obj.optString("phone"),
                        email = obj.optString("email"),
                        program = obj.optString("program"),
                        comment = obj.optString("comment")
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
            val api = com.example.kleos.data.network.ApiClient.retrofit
                .create(com.example.kleos.data.network.AdmissionsApi::class.java)
            val body = com.example.kleos.data.network.AdmissionRequest(
                fullName = application.fullName,
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

