package no.nav.aap.auth

import com.auth0.jwt.interfaces.DecodedJWT
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import no.nav.aap.httpclient.tokenprovider.OidcToken


fun ApplicationCall.bruker(): Bruker {
    // TODO: Må kreve denne før produksjonssetting error("NAVident mangler i AzureAD claims")
    return Bruker(
        principal<JWTPrincipal>()?.getClaim("NAVident", String::class) ?: "Lokalsaksbehandler"
    )
}

fun ApplicationCall.token(): OidcToken {
    // TODO: Må kreve denne før produksjonssetting error("token mangler for OBO hendelse")
    return OidcToken(
        (principal<JWTPrincipal>()?.payload as DecodedJWT).token
    )
}