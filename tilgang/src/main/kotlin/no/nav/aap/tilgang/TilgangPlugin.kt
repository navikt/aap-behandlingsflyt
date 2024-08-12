package no.nav.aap.tilgang

import com.fasterxml.jackson.databind.ObjectMapper
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.plugins.doublereceive.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.util.*
import no.nav.aap.auth.token
import org.slf4j.LoggerFactory

private val log = LoggerFactory.getLogger("TilgangPlugin")

fun Route.installerTilgangPlugin(
    operasjon: Operasjon,
    ressurs: Ressurs,
    avklaringsbehovKode: String? = null
) {
    if (ressurs.referanse.kilde === ReferanseKilde.RequestBody) {
        install(DoubleReceive)
    }
    install(TilgangPlugin) {
        this.operasjon = operasjon
        this.ressurs = ressurs
        this.avklaringsbehovKode = avklaringsbehovKode
    }
}

val TilgangPlugin =
    createRouteScopedPlugin(name = "TilgangPlugin", ::TilgangConfiguration) {
        on(AuthenticationChecked) { call ->
            val input =
                call.tilTilgangInput(pluginConfig.ressurs, pluginConfig.avklaringsbehovKode, pluginConfig.operasjon)
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

suspend fun ApplicationCall.tilTilgangInput(
    ressurs: Ressurs,
    avklaringsbehovKode: String?,
    operasjon: Operasjon
): TilgangRequest {
    val referanse = when (ressurs.referanse.kilde) {
        ReferanseKilde.PathParams -> parameters.getOrFail(ressurs.referanse.variabelNavn)
        ReferanseKilde.RequestBody -> {
            ObjectMapper()
                .readTree(receiveText())
                .get(ressurs.referanse.variabelNavn)
                .asText()
        }

        ReferanseKilde.QueryParams -> request.queryParameters.getOrFail(ressurs.referanse.variabelNavn)
    }
    log.info("referanse $referanse")
    return when (ressurs.type) {
        RessursType.Sak -> TilgangRequest(referanse, null, avklaringsbehovKode, operasjon)
        RessursType.Behandling -> TilgangRequest(null, referanse, avklaringsbehovKode, operasjon)
    }
}

enum class RessursType {
    Sak,
    Behandling
}

data class Referanse(
    val variabelNavn: String,
    val kilde: ReferanseKilde
)

enum class ReferanseKilde {
    RequestBody,
    PathParams,
    QueryParams,
}

data class Ressurs(
    val referanse: Referanse,
    val type: RessursType,
)

class TilgangConfiguration {
    var operasjon = Operasjon.SE
    var avklaringsbehovKode: String? = null
    var ressurs =
        Ressurs(Referanse("referanse", ReferanseKilde.PathParams), RessursType.Sak)
}