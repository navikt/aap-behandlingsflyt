package no.nav.aap.tilgang

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.plugins.doublereceive.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.util.*
import no.nav.aap.auth.token
import no.nav.aap.json.DefaultJsonMapper
import org.slf4j.LoggerFactory

val log = LoggerFactory.getLogger("TilgangPlugin")

inline fun <reified T : Behandlingsreferanse> Route.installerTilgangBPostPlugin(
    operasjon: Operasjon,
) {
    install(DoubleReceive)
    install(buildTilgangPlugin { call: ApplicationCall -> call.parseBehandlingFraRequestBody<T>(operasjon) })
}

inline fun <reified T : Saksreferanse> Route.installerTilgangPostPlugin(
    operasjon: Operasjon,
) {
    install(DoubleReceive)
    install(buildTilgangPlugin { call: ApplicationCall -> call.parseSakFraRequestBody<T>(operasjon) })
}

fun Route.installerTilgangGetPlugin(
    operasjon: Operasjon,
    referanse: Ressurs
) {
    install(buildTilgangPlugin { call: ApplicationCall -> call.parseFraPath(operasjon, referanse) })
}

inline fun buildTilgangPlugin(crossinline parse: suspend (call: ApplicationCall) -> TilgangRequest): RouteScopedPlugin<Unit> {
    return createRouteScopedPlugin(name = "TilgangPlugin") {
        on(AuthenticationChecked) { call ->
            val input = parse(call)
            val harTilgang =
                TilgangGateway.harTilgang(
                    input,
                    currentToken = call.token()
                )
            if (!harTilgang) {
                call.respond(HttpStatusCode.Forbidden, "Ingen tilgang")
            }
        }
    }
}

fun ApplicationCall.parseFraPath(
    operasjon: Operasjon,
    ressurs: Ressurs,
): TilgangRequest {
    val referanse = parameters.getOrFail(ressurs.referanse)
    return when (ressurs.type) {
        RessursType.Sak -> TilgangRequest(referanse, null, null, operasjon)
        RessursType.Behandling -> TilgangRequest(null, referanse, null, operasjon)
    }
}

suspend inline fun <reified T : Behandlingsreferanse> ApplicationCall.parseBehandlingFraRequestBody(
    operasjon: Operasjon
): TilgangRequest {
    val referanseObject: T = DefaultJsonMapper.fromJson<T>(receiveText())
    val referanse = referanseObject.hentBehandlingsreferanse()
    val avklaringsbehovKode = referanseObject.hentAvklaringsbehovKode()
    return TilgangRequest(null, referanse, avklaringsbehovKode, operasjon)
}

suspend inline fun <reified T : Saksreferanse> ApplicationCall.parseSakFraRequestBody(
    operasjon: Operasjon
): TilgangRequest {
    val referanseObject: T = DefaultJsonMapper.fromJson<T>(receiveText())
    val referanse = referanseObject.hentSaksreferanse()
    return TilgangRequest(referanse, null, null, operasjon)
}

enum class RessursType {
    Sak,
    Behandling
}

interface Saksreferanse {
    fun hentSaksreferanse(): String
}

interface Behandlingsreferanse {
    fun hentBehandlingsreferanse(): String
    fun hentAvklaringsbehovKode(): String?
}

data class Ressurs(
    val referanse: String,
    val type: RessursType,
)