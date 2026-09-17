package br.edu.uniceplac.modapraia.data

import br.edu.uniceplac.modapraia.domain.AuthenticatedUser
import br.edu.uniceplac.modapraia.domain.LoginRequest
import br.edu.uniceplac.modapraia.domain.RegistrationRequest
import java.util.UUID

/** Dados voláteis exclusivos do protótipo funcional; nada é persistido no dispositivo. */
class InMemoryAuthRepository : AuthRepository {
    private data class DemoAccount(
        val user: AuthenticatedUser,
        val password: String
    )

    private val accounts = mutableMapOf<String, DemoAccount>()

    init {
        val email = normalizeEmail("gestora@estoquepraia.com")
        accounts[email] = DemoAccount(
            user = AuthenticatedUser(
                id = UUID.randomUUID().toString(),
                fullName = "Gestora Estoque Praia",
                email = email,
                role = "GESTOR"
            ),
            password = "12345678"
        )
    }

    override fun login(request: LoginRequest): Result<AuthenticatedUser> {
        val account = accounts[normalizeEmail(request.email)]
        return if (account != null && account.password == request.password) {
            Result.success(account.user)
        } else {
            Result.failure(IllegalArgumentException("Credenciais não reconhecidas."))
        }
    }

    override fun register(request: RegistrationRequest): Result<Unit> {
        val email = normalizeEmail(request.email)
        if (accounts.containsKey(email)) {
            return Result.failure(IllegalArgumentException("Já existe uma conta com este e-mail."))
        }
        accounts[email] = DemoAccount(
            user = AuthenticatedUser(
                id = UUID.randomUUID().toString(),
                fullName = request.fullName.trim(),
                email = email,
                role = "GESTOR"
            ),
            password = request.password
        )
        return Result.success(Unit)
    }

    private fun normalizeEmail(value: String) = value.trim().lowercase()
}
