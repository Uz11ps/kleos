package com.kleos.education.data.network

import retrofit2.http.GET
import retrofit2.http.Path

data class CountriesResponse(
    val countries: List<String>
)

data class ConsentTextResponse(
    val text: String
)

interface SettingsApi {
    @GET("settings/countries")
    suspend fun getCountries(): CountriesResponse
    
    @GET("settings/consent/{lang}")
    suspend fun getConsentText(@Path("lang") lang: String): ConsentTextResponse
}


