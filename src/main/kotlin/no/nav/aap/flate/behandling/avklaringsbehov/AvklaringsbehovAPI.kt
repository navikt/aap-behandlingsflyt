package no.nav.aap.flate.behandling.avklaringsbehov

import io.github.smiley4.ktorswaggerui.dsl.post
import io.github.smiley4.ktorswaggerui.dsl.route
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import no.nav.aap.domene.behandling.BehandlingTjeneste
import no.nav.aap.flyt.kontroll.ValiderBehandlingTilstand
import no.nav.aap.hendelse.mottak.HendelsesMottak
import no.nav.aap.hendelse.mottak.LøsAvklaringsbehovBehandlingHendelse

fun Routing.avklaringsbehovApi() {
    route("/api/behandling", {
        tags = listOf("behandling")
    }) {
        post("/løs-behov", {
            request {
                body<LøsAvklaringsbehovPåBehandling>()
            }
            response {
                HttpStatusCode.Accepted to {
                    description = "Lagrer ned og prosesserer behov"
                }
                HttpStatusCode.BadRequest to {
                    description = "Behandling ikke i tilstand for å kunne"
                }
            }
        }) {

            val dto = call.receive<LøsAvklaringsbehovPåBehandling>()

            val behandling = BehandlingTjeneste.hent(dto.referanse)

            try {
                ValiderBehandlingTilstand.validerTilstandBehandling(behandling, listOf(dto.behov.definisjon()))

                // TODO: Slipp denne async videre
                HendelsesMottak.håndtere(key = behandling.id, hendelse = LøsAvklaringsbehovBehandlingHendelse(dto.behov, dto.behandlingVersjon))

                call.respond(HttpStatusCode.Accepted, dto)
            } catch (iae: IllegalArgumentException) {
                call.respond(HttpStatusCode.BadRequest, "{ feil: \"Ugyldig tilstand på behandling\"}")
            }
        }
    }
}