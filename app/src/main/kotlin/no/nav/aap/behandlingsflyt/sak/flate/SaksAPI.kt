package no.nav.aap.behandlingsflyt.sak.flate

import com.papsign.ktor.openapigen.route.path.normal.NormalOpenAPIRoute
import com.papsign.ktor.openapigen.route.path.normal.get
import com.papsign.ktor.openapigen.route.path.normal.post
import com.papsign.ktor.openapigen.route.response.respond
import com.papsign.ktor.openapigen.route.route
import no.nav.aap.verdityper.feilhåndtering.ElementNotFoundException
import no.nav.aap.behandlingsflyt.behandling.behandlingRepository
import no.nav.aap.behandlingsflyt.dbconnect.transaction
import no.nav.aap.behandlingsflyt.sak.Ident
import no.nav.aap.behandlingsflyt.sak.PersonRepository
import no.nav.aap.behandlingsflyt.sak.Sak
import no.nav.aap.behandlingsflyt.sak.Saksnummer
import no.nav.aap.behandlingsflyt.sak.sakRepository
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
                    saker = sakRepository(connection).finnSakerFor(person)
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
                    saker = sakRepository(connection).finnAlle()
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
                    sak = sakRepository(connection).hent(saksnummer = Saksnummer(saksnummer))

                    behandlinger = behandlingRepository(connection).hentAlleFor(sak!!.id).map { behandling ->
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
