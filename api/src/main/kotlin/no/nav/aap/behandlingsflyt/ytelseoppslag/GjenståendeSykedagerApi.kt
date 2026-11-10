package no.nav.aap.behandlingsflyt.ytelseoppslag

import com.papsign.ktor.openapigen.route.path.normal.NormalOpenAPIRoute
import com.papsign.ktor.openapigen.route.response.respond
import com.papsign.ktor.openapigen.route.route
import io.ktor.http.HttpStatusCode
import io.ktor.server.response.respond
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.samordning.ytelsevurdering.gateway.SykepengerGateway
import no.nav.aap.behandlingsflyt.unleash.BehandlingsflytFeature
import no.nav.aap.behandlingsflyt.unleash.UnleashGateway
import no.nav.aap.komponenter.gateway.GatewayProvider
import no.nav.aap.komponenter.httpklient.exception.ApiException
import no.nav.aap.komponenter.httpklient.exception.UgyldigForespørselException
import no.nav.aap.tilgang.AuthorizationBodyPathConfig
import no.nav.aap.tilgang.Operasjon
import no.nav.aap.tilgang.authorizedPost

const val GJENSTAENDE_SYKEDAGER_PATH = "/api/person/gjenstaende-sykedager"

fun NormalOpenAPIRoute.gjenståendeSykedagerApi(gatewayProvider: GatewayProvider) {
    route(GJENSTAENDE_SYKEDAGER_PATH) {
        authorizedPost<Unit, GjenståendeSykedagerDTO, GjenståendeSykedagerRequest>(
            AuthorizationBodyPathConfig(operasjon = Operasjon.SE)
        ) { _, request ->
            if (!gatewayProvider.provide<UnleashGateway>().isEnabled(BehandlingsflytFeature.HentSykepengerMaksdato)) {
                val feil = ApiException(
                    status = HttpStatusCode.NotFound,
                    message = "Oppslag av gjenstående sykedager er ikke aktivert"
                )
                return@authorizedPost pipeline.call.respond(feil.status, feil.tilApiErrorResponse())
            }
            if (!request.personident.matches(Regex("[0-9]{11}"))) {
                val feil = UgyldigForespørselException("Personident må bestå av 11 siffer")
                return@authorizedPost pipeline.call.respond(feil.status, feil.tilApiErrorResponse())
            }

            val resultat = gatewayProvider.provide<SykepengerGateway>().hentGjenståendeSykedager(request.personident)
            respond(
                GjenståendeSykedagerDTO(
                    grunnlag = resultat.status.name,
                    gjenståendeDager = resultat.gjenståendeDager,
                    vurdertTilOgMed = resultat.vurdertTilOgMed,
                )
            )
        }
    }
}
