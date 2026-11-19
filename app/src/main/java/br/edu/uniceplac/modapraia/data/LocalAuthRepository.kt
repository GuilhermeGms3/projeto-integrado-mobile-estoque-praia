package br.edu.uniceplac.modapraia.data

import android.content.Context
import android.util.Base64
import br.edu.uniceplac.modapraia.domain.*
import org.json.JSONObject
import java.security.MessageDigest
import java.security.SecureRandom
import java.util.UUID
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

/** Cadastro local: senha derivada com PBKDF2 e salt individual, sem senha em texto puro. */
class LocalAuthRepository(context: Context) : AuthRepository {
    private val preferences = context.applicationContext.getSharedPreferences("accounts", Context.MODE_PRIVATE)
    private val session = context.applicationContext.getSharedPreferences("local_session", Context.MODE_PRIVATE)

    fun currentUser(): AuthenticatedUser? {
        val email=session.getString("email",null) ?: return null
        val user=runCatching {
            val account=JSONObject(preferences.getString(email,null) ?: return@runCatching null)
            if(account.getString("id")!=session.getString("user_id",null)) null
            else AuthenticatedUser(account.getString("id"),account.getString("name"),email,"GESTOR")
        }.getOrNull()
        if(user==null) session.edit().clear().apply()
        return user
    }

    fun logout(): Boolean = session.edit().clear().commit()
    override fun register(request: RegistrationRequest): Result<Unit> = runCatching {
        val email = request.email.trim().lowercase()
        require(request.fullName.trim().length >= 3 && android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) { "Informe nome e e-mail válidos." }
        require(request.password.length >= 8) { "A senha deve ter pelo menos 8 caracteres." }
        require(!preferences.contains(email)) { "Já existe uma conta com este e-mail." }
        val salt = ByteArray(16).also { SecureRandom().nextBytes(it) }
        val account = JSONObject().put("id", UUID.randomUUID().toString()).put("name", request.fullName.trim()).put("salt", encode(salt)).put("hash", encode(derive(request.password, salt)))
        check(preferences.edit().putString(email, account.toString()).commit()) { "Não foi possível salvar a conta." }
    }
    override fun login(request: LoginRequest): Result<AuthenticatedUser> = runCatching {
        val email = request.email.trim().lowercase()
        val stored = preferences.getString(email, null) ?: error("Credenciais não reconhecidas.")
        val account = JSONObject(stored)
        require(MessageDigest.isEqual(decode(account.getString("hash")), derive(request.password, decode(account.getString("salt"))))) { "Credenciais não reconhecidas." }
        check(session.edit().putString("email",email).putString("user_id",account.getString("id")).commit()) { "Não foi possível manter sua sessão. Tente novamente." }
        AuthenticatedUser(account.getString("id"), account.getString("name"), email, "GESTOR")
    }
    private fun derive(password: String, salt: ByteArray): ByteArray {
        val spec = PBEKeySpec(password.toCharArray(), salt, 600_000, 256)
        return try { SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).encoded } finally { spec.clearPassword() }
    }
    private fun encode(bytes: ByteArray) = Base64.encodeToString(bytes, Base64.NO_WRAP)
    private fun decode(value: String) = Base64.decode(value, Base64.NO_WRAP)
}
