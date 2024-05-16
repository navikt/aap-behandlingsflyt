package no.nav.aap.httpclient.tokenprovider

import com.auth0.jwt.JWT
import java.time.LocalDateTime
import java.time.ZoneId

class OidcToken(accessToken: String) {

    private val accessToken = JWT.decode(accessToken)

    fun token(): String {
        return accessToken.token
    }

    fun expires(): LocalDateTime {
        return LocalDateTime.ofInstant(accessToken.expiresAt.toInstant(), ZoneId.systemDefault())
    }

    fun isNotExpired(): Boolean {
        val now = LocalDateTime.now().plusSeconds(30)
        return now.isBefore(expires())
    }
}