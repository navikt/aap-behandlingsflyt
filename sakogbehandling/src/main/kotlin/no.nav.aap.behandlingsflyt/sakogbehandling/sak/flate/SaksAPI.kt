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
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.adapters.Dokument
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.adapters.PdlIdentGateway
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.adapters.PdlPersoninfoGateway
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.adapters.SafHentDokumentGateway
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.adapters.SafListDokumentGateway
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.adapters.TilgangGateway
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.db.PersonRepository
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.db.SakRepositoryImpl
import no.nav.aap.verdityper.Periode
import no.nav.aap.verdityper.dokument.DokumentInfoId
import no.nav.aap.verdityper.dokument.JournalpostId
import no.nav.aap.verdityper.feilhåndtering.ElementNotFoundException
import no.nav.aap.verdityper.sakogbehandling.Ident
import org.slf4j.LoggerFactory
import javax.sql.DataSource

fun NormalOpenAPIRoute.saksApi(dataSource: DataSource) {
    val logger = LoggerFactory.getLogger("Saks-api")

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
                val token = token()

                val (sak, behandlinger) = dataSource.transaction { connection ->
                    val sak = SakRepositoryImpl(connection).hent(saksnummer = Saksnummer(saksnummer))

                    val leseTilgang = TilgangGateway.kanLeseSak(sak.person.identer(), token)
                    logger.info("Har lesetilgang: $leseTilgang")

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
                get<HentSakDTO, List<Dokument>> { req ->
                    val token = token()
                    // 1. gjør api-kall graphql med token over
                    // 2. returner som streng
                    // TODO gjør pent
                    val safRespons = SafListDokumentGateway.hentDokumenterForSak(Saksnummer(req.saksnummer), token)
                    respond(
                        safRespons
                    )
                }
            }

            route("/dokument/{journalpostId}/{dokumentinfoId}") {
                get<HentDokumentDTO, DokumentResponsDTO> { req ->
                    val journalpostId = req.journalpostId
                    val dokumentInfoId = req.dokumentinfoId

                    val token = token()
                    val gateway = SafHentDokumentGateway()
                    val dokumentRespons =
                        gateway.hentDokument(JournalpostId(journalpostId), DokumentInfoId(dokumentInfoId), token)
                    pipeline.context.response.headers.append(
                        name = "Content-Disposition",
                        value = "inline; filename=${dokumentRespons.filnavn}"
                    )

                    respond(DokumentResponsDTO(stream = dokumentRespons.dokument))
                }
            }

            route("/{saksnummer}/personinformasjon") {
                get<HentSakDTO, SakPersoninfoDTO> { req ->
                    val saksnummer = req.saksnummer

                    val ident = dataSource.transaction(readOnly = true) { connection ->
                        val sak = SakRepositoryImpl(connection).hent(saksnummer = Saksnummer(saksnummer))
                        sak.person.aktivIdent()
                    }

                    val personinfo = PdlPersoninfoGateway.hentPersoninfoForIdent(ident, token())

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
