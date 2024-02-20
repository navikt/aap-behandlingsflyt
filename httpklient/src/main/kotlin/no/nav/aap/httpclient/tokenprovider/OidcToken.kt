package no.nav.aap.httpclient.tokenprovider

import java.time.LocalDateTime

class OidcToken(val accessToken: String, private val expires: LocalDateTime) {
    fun isNotExpired(): Boolean {
        return LocalDateTime.now() > expires
    }
}