package no.nav.aap.behandlingsflyt.flate.behandling.avklaringsbehov

import com.papsign.ktor.openapigen.route.path.normal.NormalOpenAPIRoute
import com.papsign.ktor.openapigen.route.path.normal.post
import com.papsign.ktor.openapigen.route.response.respond
import com.papsign.ktor.openapigen.route.route
import com.papsign.ktor.openapigen.route.throws
import io.ktor.http.*
import no.nav.aap.behandlingsflyt.domene.behandling.BehandlingTjeneste
import no.nav.aap.flyt.kontroll.ValiderBehandlingTilstand
import no.nav.aap.hendelse.mottak.HendelsesMottak
import no.nav.aap.hendelse.mottak.LøsAvklaringsbehovBehandlingHendelse

fun NormalOpenAPIRoute.avklaringsbehovApi() {
    route("/api/behandling") {
        route("/løs-behov").throws(HttpStatusCode.BadRequest, IllegalArgumentException::class) {
            post<Unit, LøsAvklaringsbehovPåBehandling, LøsAvklaringsbehovPåBehandling> { path, request ->

                val dto = request

                val behandling = BehandlingTjeneste.hent(dto.referanse)

                ValiderBehandlingTilstand.validerTilstandBehandling(behandling, listOf(dto.behov.definisjon()))

                // TODO: Slipp denne async videre
                HendelsesMottak.håndtere(
                    key = behandling.id,
                    hendelse = LøsAvklaringsbehovBehandlingHendelse(dto.behov, dto.behandlingVersjon)
                )

                respond(dto)
            }
        }
    }
}