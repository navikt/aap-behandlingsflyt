package no.nav.aap.behandlingsflyt.sakogbehandling.sak.flate

import com.papsign.ktor.openapigen.route.path.normal.NormalOpenAPIRoute
import com.papsign.ktor.openapigen.route.path.normal.get
import com.papsign.ktor.openapigen.route.path.normal.post
import com.papsign.ktor.openapigen.route.response.respond
import com.papsign.ktor.openapigen.route.route
import no.nav.aap.auth.token
import no.nav.aap.komponenter.dbconnect.transaction
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingRepositoryImpl
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.PersonOgSakService
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.Saksnummer
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.adapters.Dokument
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.adapters.PdlIdentGateway
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.adapters.PdlPersoninfoGateway
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.adapters.SafHentDokumentGateway
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.adapters.SafListDokumentGateway
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.db.PersonRepository
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.db.SakRepositoryImpl
import no.nav.aap.komponenter.type.Periode
import no.nav.aap.requiredConfigForKey
import no.nav.aap.tilgang.Operasjon
import no.nav.aap.tilgang.Ressurs
import no.nav.aap.tilgang.RessursType
import no.nav.aap.tilgang.TilgangGateway
import no.nav.aap.tilgang.TilgangRequest
import no.nav.aap.tilgang.authorizedGet
import no.nav.aap.tilgang.authorizedPostWithApprovedList
import no.nav.aap.verdityper.dokument.DokumentInfoId
import no.nav.aap.verdityper.dokument.JournalpostId
import no.nav.aap.verdityper.feilhåndtering.ElementNotFoundException
import no.nav.aap.verdityper.sakogbehandling.Ident
import javax.sql.DataSource

fun NormalOpenAPIRoute.saksApi(dataSource: DataSource) {
    val mottakAzp= requiredConfigForKey("integrasjon.mottak.azp")
    route("/api/sak") {
        route("/finn").post<Unit, List<SaksinfoDTO>, FinnSakForIdentDTO> { _, dto ->
            val saker: List<SaksinfoDTO> = dataSource.transaction(readOnly = true) { connection ->
                val ident = Ident(dto.ident)
                val person = PersonRepository(connection).finn(ident)

                if (person == null) {
                    throw ElementNotFoundException()
                } else {
                    SakRepositoryImpl(connection).finnSakerFor(person).map { sak ->
                        SaksinfoDTO(
                            saksnummer = sak.saksnummer.toString(),
                            opprettetTidspunkt = sak.opprettetTidspunkt,
                            periode = sak.rettighetsperiode,
                            ident = sak.person.aktivIdent().identifikator
                        )
                    }

                }
            }
            respond(saker)
        }

        route("/finnEllerOpprett").authorizedPostWithApprovedList<Unit, SaksinfoDTO, FinnEllerOpprettSakDTO>(mottakAzp) { _, dto ->
            val saken: SaksinfoDTO = dataSource.transaction { connection ->
                val ident = Ident(dto.ident)
                val periode = Periode(
                    dto.søknadsdato, dto.søknadsdato.plusYears(1)
                ) // Setter til et år frem i tid som er tilsvarende "vedtakslengde" i forskriften
                val sak = PersonOgSakService(
                    connection = connection, pdlGateway = PdlIdentGateway
                ).finnEllerOpprett(ident = ident, periode = periode)

                SaksinfoDTO(
                    saksnummer = sak.saksnummer.toString(),
                    opprettetTidspunkt = sak.opprettetTidspunkt,
                    periode = periode,
                    ident = sak.person.aktivIdent().identifikator
                )
            }
            respond(saken)
        }
        route("") {
            route("/alle").get<Unit, List<SaksinfoDTO>> {
                val saker: List<SaksinfoDTO> = dataSource.transaction(readOnly = true) { connection ->
                    SakRepositoryImpl(connection).finnAlle().map { sak ->
                        SaksinfoDTO(
                            saksnummer = sak.saksnummer.toString(),
                            opprettetTidspunkt = sak.opprettetTidspunkt,
                            periode = sak.rettighetsperiode,
                            ident = sak.person.aktivIdent().identifikator
                        )
                    }
                }

                respond(saker)
            }
            route("/{saksnummer}").authorizedGet<HentSakDTO, UtvidetSaksinfoDTO>(
                Operasjon.SE, Ressurs("saksnummer", RessursType.Sak)
            ) { req ->
                val saksnummer = req.saksnummer

                val (sak, behandlinger) = dataSource.transaction(readOnly = true) { connection ->
                    val sak = SakRepositoryImpl(connection).hent(saksnummer = Saksnummer(saksnummer))

                    val behandlinger = BehandlingRepositoryImpl(connection).hentAlleFor(sak.id).map { behandling ->
                        BehandlinginfoDTO(
                            referanse = behandling.referanse.referanse,
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
                        opprettetTidspunkt = sak.opprettetTidspunkt,
                        periode = sak.rettighetsperiode,
                        ident = sak.person.identer().first().identifikator,
                        behandlinger = behandlinger,
                        status = sak.status()
                    )
                )
            }
            route("/{saksnummer}/dokumenter") {
                authorizedGet<HentSakDTO, List<Dokument>>(Operasjon.SE, Ressurs("saksnummer", RessursType.Sak)) { req ->
                    val token = token()
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
                    val gateway = SafHentDokumentGateway.withDefaultRestClient()
                    val dokumentRespons =
                        gateway.hentDokument(JournalpostId(journalpostId), DokumentInfoId(dokumentInfoId), token)
                    pipeline.context.response.headers.append(
                        name = "Content-Disposition", value = "inline; filename=${dokumentRespons.filnavn}"
                    )

                    respond(DokumentResponsDTO(stream = dokumentRespons.dokument))
                }
            }
            route("/{saksnummer}/lesetilgang") {
                get<HentSakDTO, LesetilgangDTO> { req ->
                    val saksnummer = req.saksnummer
                    val sak = dataSource.transaction(readOnly = true) { connection ->
                        SakRepositoryImpl(connection).hent(saksnummer = Saksnummer(saksnummer))
                    }
                    val harLesetilgang = TilgangGateway.harTilgang(
                        TilgangRequest(sak.saksnummer.toString(), null, null, Operasjon.SE), currentToken = token()
                    )
                    respond(LesetilgangDTO(harLesetilgang))
                }
            }

            route("/{saksnummer}/personinformasjon") {
                authorizedGet<HentSakDTO, SakPersoninfoDTO>(
                    Operasjon.SE,
                    Ressurs("saksnummer", RessursType.Sak)
                ) { req ->

                    val saksnummer = req.saksnummer

                    val ident = dataSource.transaction(readOnly = true) { connection ->
                        val sak = SakRepositoryImpl(connection).hent(saksnummer = Saksnummer(saksnummer))
                        sak.person.aktivIdent()
                    }

                    val personinfo = PdlPersoninfoGateway.hentPersoninfoForIdent(ident, token())

                    respond(
                        SakPersoninfoDTO(
                            fnr = personinfo.ident.identifikator,
                            navn = personinfo.fultNavn(),
                        )
                    )
                }
            }
        }
    }
}
