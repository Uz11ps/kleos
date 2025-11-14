package com.example.kleos.data.model

data class AdmissionApplication(
    val id: String,
    val fullName: String,
    val phone: String,
    val email: String,
    val dateOfBirth: String? = null,
    val placeOfBirth: String? = null,
    val nationality: String? = null,
    val passportNumber: String? = null,
    val passportIssue: String? = null,
    val passportExpiry: String? = null,
    val visaCity: String? = null,
    val program: String,
    val comment: String?
)


