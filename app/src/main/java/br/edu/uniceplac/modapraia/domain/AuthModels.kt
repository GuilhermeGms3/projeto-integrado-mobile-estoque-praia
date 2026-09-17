package br.edu.uniceplac.modapraia.domain

data class LoginRequest(
    val email: String,
    val password: String
)

data class RegistrationRequest(
    val fullName: String,
    val email: String,
    val password: String
)

data class AuthenticatedUser(
    val id: String,
    val fullName: String,
    val email: String,
    val role: String
)
