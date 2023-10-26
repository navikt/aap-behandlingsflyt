package no.nav.aap.behandlingsflyt.flate.sak

import com.papsign.ktor.openapigen.route.path.normal.NormalOpenAPIRoute
import com.papsign.ktor.openapigen.route.path.normal.get
import com.papsign.ktor.openapigen.route.path.normal.post
import com.papsign.ktor.openapigen.route.response.respond
import com.papsign.ktor.openapigen.route.route
import no.nav.aap.behandlingsflyt.ElementNotFoundException
import no.nav.aap.behandlingsflyt.behandling.BehandlingRepository
import no.nav.aap.behandlingsflyt.dbstuff.transaction
import no.nav.aap.behandlingsflyt.sak.Sak
import no.nav.aap.behandlingsflyt.sak.SakRepository
import no.nav.aap.behandlingsflyt.sak.Saksnummer
import no.nav.aap.behandlingsflyt.sak.person.Ident
import no.nav.aap.behandlingsflyt.sak.person.PersonRepository
import javax.sql.DataSource

fun NormalOpenAPIRoute.saksApi(dataSource: DataSource) {
    route("/api/sak") {
        route("/finn").post<Unit, List<SaksinfoDTO>, FinnSakForIdentDTO> { _, dto ->
            var saker: List<SaksinfoDTO> = emptyList()
            dataSource.transaction { connection ->
                val ident = Ident(dto.ident)
                val person = PersonRepository(connection).finn(ident)

                if (person == null) {
                    throw ElementNotFoundException()
                } else {
                    saker = SakRepository(connection).finnSakerFor(person)
                        .map { sak ->
                            SaksinfoDTO(
                                saksnummer = sak.saksnummer.toString(),
                                periode = sak.rettighetsperiode
                            )
                        }

                }
            }
            respond(saker)
        }
        route("") {
            route("/alle").get<Unit, List<SaksinfoDTO>> {
                var saker: List<SaksinfoDTO> = emptyList()
                dataSource.transaction { connection ->
                    saker = SakRepository(connection).finnAlle()
                        .map { sak ->
                            SaksinfoDTO(
                                saksnummer = sak.saksnummer.toString(),
                                periode = sak.rettighetsperiode
                            )
                        }
                }

                respond(saker)
            }
            route("/{saksnummer}").get<HentSakDTO, UtvidetSaksinfoDTO> { req ->
                val saksnummer = req.saksnummer
                var sak: Sak? = null
                var behandlinger: List<BehandlinginfoDTO> = emptyList()

                dataSource.transaction { connection ->
                    sak = SakRepository(connection).hent(saksnummer = Saksnummer(saksnummer))

                    behandlinger = BehandlingRepository.hentAlleFor(sak!!.id).map { behandling ->
                        BehandlinginfoDTO(
                            referanse = behandling.referanse,
                            type = behandling.type.identifikator(),
                            status = behandling.status(),
                            opprettet = behandling.opprettetTidspunkt
                        )
                    }
                }

                respond(
                    UtvidetSaksinfoDTO(
                        saksnummer = sak!!.saksnummer.toString(),
                        periode = sak!!.rettighetsperiode,
                        ident = sak!!.person.identer().first().identifikator,
                        behandlinger = behandlinger,
                        status = sak!!.status()
                    )
                )
            }
        }
    }
}
