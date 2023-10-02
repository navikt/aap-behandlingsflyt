package no.nav.aap.behandlingsflyt.flate.sak

import com.papsign.ktor.openapigen.route.path.normal.NormalOpenAPIRoute
import com.papsign.ktor.openapigen.route.path.normal.get
import com.papsign.ktor.openapigen.route.path.normal.post
import com.papsign.ktor.openapigen.route.response.respond
import com.papsign.ktor.openapigen.route.route
import no.nav.aap.behandlingsflyt.domene.ElementNotFoundException
import no.nav.aap.behandlingsflyt.domene.behandling.BehandlingTjeneste
import no.nav.aap.behandlingsflyt.domene.person.Ident
import no.nav.aap.behandlingsflyt.domene.person.Personlager
import no.nav.aap.behandlingsflyt.domene.sak.Sakslager
import no.nav.aap.behandlingsflyt.domene.sak.Saksnummer

fun NormalOpenAPIRoute.saksApi() {
    route("/api/sak") {
        route("/finn").post<Unit, List<SaksinfoDTO>, FinnSakForIdentDTO> { _, dto ->
            val ident = Ident(dto.ident)
            val person = Personlager.finn(ident)

            if (person == null) {
                throw no.nav.aap.behandlingsflyt.domene.ElementNotFoundException()
            } else {
                val saker = Sakslager.finnSakerFor(person)
                    .map { sak ->
                        SaksinfoDTO(
                            saksnummer = sak.saksnummer.toString(),
                            periode = sak.rettighetsperiode
                        )
                    }

                respond(saker)
            }
        }
        route("") {
            route("/alle").get<Unit, List<SaksinfoDTO>> {
                val saker = Sakslager.finnAlle()
                    .map { sak ->
                        SaksinfoDTO(
                            saksnummer = sak.saksnummer.toString(),
                            periode = sak.rettighetsperiode
                        )
                    }

                respond(saker)
            }
            route("/{saksnummer}").get<HentSakDTO, UtvidetSaksinfoDTO> { req ->
                val saksnummer = req.saksnummer

                val sak = Sakslager.hent(saksnummer = Saksnummer(saksnummer))

                val behandlinger = BehandlingTjeneste.hentAlleFor(sak.id).map { behandling ->
                    BehandlinginfoDTO(
                        referanse = behandling.referanse,
                        type = behandling.type.identifikator(),
                        status = behandling.status(),
                        opprettet = behandling.opprettetTidspunkt
                    )
                }

                respond(
                    UtvidetSaksinfoDTO(
                        saksnummer = sak.saksnummer.toString(),
                        periode = sak.rettighetsperiode,
                        ident = sak.person.identer().first().identifikator,
                        behandlinger = behandlinger,
                        status = sak.status()
                    )
                )
            }
        }
    }
}
