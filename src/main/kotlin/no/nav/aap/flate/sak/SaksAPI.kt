package no.nav.aap.flate.sak

import io.github.smiley4.ktorswaggerui.dsl.get
import io.github.smiley4.ktorswaggerui.dsl.post
import io.github.smiley4.ktorswaggerui.dsl.route
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.util.*
import no.nav.aap.domene.behandling.BehandlingTjeneste
import no.nav.aap.domene.person.Personlager
import no.nav.aap.domene.sak.Sakslager
import no.nav.aap.domene.typer.Ident
import no.nav.aap.domene.typer.Saksnummer

fun Routing.saksApi() {
    route("/api/sak", {
        tags = listOf("sak")
    }) {
        post("/finn", {
            request { body<FinnSakForIdentDTO>() }
            response {
                HttpStatusCode.OK to {
                    description = "Successful Request"
                    body<List<SaksinfoDTO>> { }
                }
            }
        }) {
            val dto = call.receive<FinnSakForIdentDTO>()

            val ident = Ident(dto.ident)
            val person = Personlager.finn(ident)

            if (person == null) {
                call.respond(HttpStatusCode.NoContent)
            } else {
                val saker = Sakslager.finnSakerFor(person)
                    .map { sak -> SaksinfoDTO(saksnummer = sak.saksnummer.toString(), periode = sak.rettighetsperiode) }

                call.respond(HttpStatusCode.OK, saker)
            }
        }
        get("/alle", {
            description = "Endepunkt for å hente ut alle saker. NB! Fjernes senere"
            deprecated = true
            tags = listOf("test")
            response {
                HttpStatusCode.OK to {
                    description = "Successful Request"
                    body<List<SaksinfoDTO>> { }
                }
            }
        }) {
            val saker = Sakslager.finnAlle()
                .map { sak -> SaksinfoDTO(saksnummer = sak.saksnummer.toString(), periode = sak.rettighetsperiode) }

            call.respond(HttpStatusCode.OK, saker)
        }
        get("/hent/{saksnummer}", {
            request { pathParameter<String>("saksnummer") }
            response {
                HttpStatusCode.OK to {
                    description = "Successful Request"
                    body<UtvidetSaksinfoDTO> { }
                }
            }
        }) {
            val saksnummer = call.parameters.getOrFail("saksnummer")

            val sak = Sakslager.hent(saksnummer = Saksnummer(saksnummer))

            val behandlinger = BehandlingTjeneste.hentAlleFor(sak.id).map { behandling ->
                BehandlinginfoDTO(
                    referanse = behandling.referanse,
                    type = behandling.type.identifikator(),
                    status = behandling.status(),
                    opprettet = behandling.opprettetTidspunkt
                )
            }

            call.respond(
                HttpStatusCode.OK,
                UtvidetSaksinfoDTO(
                    saksnummer = sak.saksnummer.toString(),
                    periode = sak.rettighetsperiode,
                    behandlinger = behandlinger,
                    status = sak.status()
                )
            )
        }
    }
}
