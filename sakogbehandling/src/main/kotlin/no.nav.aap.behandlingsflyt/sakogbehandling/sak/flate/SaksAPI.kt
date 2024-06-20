package no.nav.aap.behandlingsflyt.sakogbehandling.sak.flate

import com.papsign.ktor.openapigen.route.path.normal.NormalOpenAPIRoute
import com.papsign.ktor.openapigen.route.path.normal.get
import com.papsign.ktor.openapigen.route.path.normal.post
import com.papsign.ktor.openapigen.route.response.respond
import com.papsign.ktor.openapigen.route.route
import no.nav.aap.auth.token
import no.nav.aap.behandlingsflyt.dbconnect.transaction
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingRepositoryImpl
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.PersonOgSakService
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.Saksnummer
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.adapters.PdlIdentGateway
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.adapters.PdlPersoninfoGateway
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.adapters.SafGateway
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.db.PersonRepository
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.db.SakRepositoryImpl
import no.nav.aap.verdityper.Periode
import no.nav.aap.verdityper.feilhåndtering.ElementNotFoundException
import no.nav.aap.verdityper.sakogbehandling.Ident
import javax.sql.DataSource

fun NormalOpenAPIRoute.saksApi(dataSource: DataSource) {
    route("/api/sak") {
        route("/finn").post<Unit, List<SaksinfoDTO>, FinnSakForIdentDTO> { _, dto ->
            val saker: List<SaksinfoDTO> = dataSource.transaction { connection ->
                val ident = Ident(dto.ident)
                val person = PersonRepository(connection).finn(ident)

                if (person == null) {
                    throw ElementNotFoundException()
                } else {
                    SakRepositoryImpl(connection).finnSakerFor(person)
                        .map { sak ->
                            SaksinfoDTO(
                                saksnummer = sak.saksnummer.toString(),
                                periode = sak.rettighetsperiode,
                                ident = sak.person.aktivIdent().identifikator
                            )
                        }

                }
            }
            respond(saker)
        }
        route("/finnEllerOpprett").post<Unit, SaksinfoDTO, FinnEllerOpprettSakDTO> { _, dto ->
            val saken: SaksinfoDTO = dataSource.transaction { connection ->
                val ident = Ident(dto.ident)
                val periode = Periode(
                    dto.søknadsdato,
                    dto.søknadsdato.plusYears(1)
                ) // Setter til et år frem i tid som er tilsvarende "vedtakslengde" i forskriften
                val sak =
                    PersonOgSakService(
                        connection = connection,
                        pdlGateway = PdlIdentGateway
                    ).finnEllerOpprett(ident = ident, periode = periode)

                SaksinfoDTO(
                    saksnummer = sak.saksnummer.toString(),
                    periode = periode,
                    ident = sak.person.aktivIdent().identifikator
                )
            }
            respond(saken)
        }
        route("") {
            route("/alle").get<Unit, List<SaksinfoDTO>> {
                val saker: List<SaksinfoDTO> = dataSource.transaction { connection ->
                    SakRepositoryImpl(connection).finnAlle()
                        .map { sak ->
                            SaksinfoDTO(
                                saksnummer = sak.saksnummer.toString(),
                                periode = sak.rettighetsperiode,
                                ident = sak.person.aktivIdent().identifikator
                            )
                        }
                }

                respond(saker)
            }
            route("/{saksnummer}").get<HentSakDTO, UtvidetSaksinfoDTO> { req ->
                val saksnummer = req.saksnummer

                val (sak, behandlinger) = dataSource.transaction { connection ->
                    val sak = SakRepositoryImpl(connection).hent(saksnummer = Saksnummer(saksnummer))

                    val behandlinger = BehandlingRepositoryImpl(connection).hentAlleFor(sak.id).map { behandling ->
                        BehandlinginfoDTO(
                            referanse = behandling.referanse,
                            type = behandling.typeBehandling().identifikator(),
                            status = behandling.status(),
                            opprettet = behandling.opprettetTidspunkt
                        )
                    }

                    sak to behandlinger
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
            route("/{saksnummer}/dokumenter") {
                get<HentSakDTO, String> { req ->
                    val token = pipeline.context.token()
                    // 1. gjør api-kall graphql med token over
                    // 2. returner som streng
                    // TODO gjør pent

                    respond(
                        SafGateway.hentDokumenterForSak(Saksnummer(req.saksnummer), token)
                    )
                }
            }
            route("/{saksnummer}/personinformasjon") {
                get<HentSakDTO, SakPersoninfoDTO> { req ->
                    val saksnummer = req.saksnummer

                    val ident = dataSource.transaction(readOnly = true) { connection ->
                        val sak = SakRepositoryImpl(connection).hent(saksnummer = Saksnummer(saksnummer))
                        sak.person.aktivIdent()
                    }

                    val personinfo = PdlPersoninfoGateway.hentPersoninfoForIdent(ident, pipeline.context.token())

                    respond(
                        SakPersoninfoDTO(
                            fnr = personinfo.ident.identifikator,
                            navn = listOfNotNull(
                                personinfo.fornavn,
                                personinfo.mellomnavn,
                                personinfo.etternavn
                            ).filter { it.isNotBlank() }.joinToString(" "),
                        )
                    )
                }
            }
        }
    }
}
