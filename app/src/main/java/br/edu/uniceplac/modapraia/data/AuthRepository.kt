package br.edu.uniceplac.modapraia.data

import br.edu.uniceplac.modapraia.domain.AuthenticatedUser
import br.edu.uniceplac.modapraia.domain.LoginRequest
import br.edu.uniceplac.modapraia.domain.RegistrationRequest

/**
 * Fronteira de autenticação do aplicativo.
 *
 * A implementação local sustenta o protótipo. A futura implementação remota
 * deverá enviar as solicitações à API REST, nunca diretamente ao PostgreSQL.
 */
interface AuthRepository {
    fun login(request: LoginRequest): Result<AuthenticatedUser>
    fun register(request: RegistrationRequest): Result<Unit>
}
