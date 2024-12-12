package no.nav.aap.behandlingsflyt.sakogbehandling.sak.flate

import com.papsign.ktor.openapigen.route.TagModule
import com.papsign.ktor.openapigen.route.path.normal.NormalOpenAPIRoute
import com.papsign.ktor.openapigen.route.path.normal.get
import com.papsign.ktor.openapigen.route.path.normal.post
import com.papsign.ktor.openapigen.route.response.respond
import com.papsign.ktor.openapigen.route.route
import no.nav.aap.behandlingsflyt.Tags
import no.nav.aap.behandlingsflyt.kontrakt.sak.Saksnummer
import no.nav.aap.behandlingsflyt.sakogbehandling.Ident
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingRepository
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.flate.ElementNotFoundException
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.IdentGateway
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.PersonOgSakService
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.PersoninfoGateway
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.SakRepository
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.adapters.DokumentInfoId
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.adapters.SafHentDokumentGateway
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.adapters.SafListDokument
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.adapters.SafListDokumentGateway
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.db.PersonRepository
import no.nav.aap.komponenter.config.requiredConfigForKey
import no.nav.aap.komponenter.dbconnect.transaction
import no.nav.aap.komponenter.httpklient.auth.token
import no.nav.aap.komponenter.type.Periode
import no.nav.aap.lookup.gateway.GatewayProvider
import no.nav.aap.lookup.repository.RepositoryProvider
import no.nav.aap.tilgang.AuthorizationParamPathConfig
import no.nav.aap.tilgang.SakPathParam
import no.nav.aap.tilgang.authorizedGet
import no.nav.aap.tilgang.authorizedPostWithApprovedList
import no.nav.aap.verdityper.dokument.JournalpostId
import javax.sql.DataSource

fun NormalOpenAPIRoute.saksApi(dataSource: DataSource) {
    val postmottakAzp = requiredConfigForKey("integrasjon.postmottak.azp")
    val brevAzp = requiredConfigForKey("integrasjon.brev.azp")
    route("/api/sak") {
        route("/finn").post<Unit, List<SaksinfoDTO>, FinnSakForIdentDTO>(TagModule(listOf(Tags.Sak))) { _, dto ->
            val saker: List<SaksinfoDTO> = dataSource.transaction(readOnly = true) { connection ->
                val repositoryProvider = RepositoryProvider(connection)
                val ident = Ident(dto.ident)
                val person = repositoryProvider.provide(PersonRepository::class).finn(ident)

                if (person == null) {
                    throw ElementNotFoundException()
                } else {
                    repositoryProvider.provide(SakRepository::class).finnSakerFor(person).map { sak ->
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

        route("/finnEllerOpprett").authorizedPostWithApprovedList<Unit, SaksinfoDTO, FinnEllerOpprettSakDTO>(
            postmottakAzp, modules = listOf(TagModule(listOf(Tags.Sak)))
        ) { _, dto ->
            val saken: SaksinfoDTO = dataSource.transaction { connection ->
                val repositoryProvider = RepositoryProvider(connection)
                val ident = Ident(dto.ident)
                val periode = Periode(
                    dto.søknadsdato, dto.søknadsdato.plusYears(1)
                ) // Setter til et år frem i tid som er tilsvarende "vedtakslengde" i forskriften
                val sak = PersonOgSakService(
                    pdlGateway = GatewayProvider.provide(IdentGateway::class),
                    personRepository = repositoryProvider.provide(PersonRepository::class),
                    sakRepository = repositoryProvider.provide(SakRepository::class)
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
            route("/alle").get<Unit, List<SaksinfoDTO>>(TagModule(listOf(Tags.Sak))) {
                val saker: List<SaksinfoDTO> = dataSource.transaction(readOnly = true) { connection ->
                    val repositoryProvider = RepositoryProvider(connection)
                    repositoryProvider.provide(SakRepository::class).finnAlle().map { sak ->
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
                AuthorizationParamPathConfig(
                    sakPathParam = SakPathParam("saksnummer")
                ),
                TagModule(listOf(Tags.Sak))
            ) { req ->
                val saksnummer = req.saksnummer

                val (sak, behandlinger) = dataSource.transaction(readOnly = true) { connection ->
                    val repositoryProvider = RepositoryProvider(connection)
                    val sak = repositoryProvider.provide(SakRepository::class).hent(saksnummer = Saksnummer(saksnummer))

                    val behandlinger =
                        repositoryProvider.provide(BehandlingRepository::class).hentAlleFor(sak.id).map { behandling ->
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
                authorizedGet<HentSakDTO, List<SafListDokument>>(
                    AuthorizationParamPathConfig(
                        sakPathParam = SakPathParam("saksnummer")
                    ), TagModule(listOf(Tags.Sak))
                ) { req ->
                    val token = token()
                    val safRespons = SafListDokumentGateway.hentDokumenterForSak(Saksnummer(req.saksnummer), token)
                    respond(
                        safRespons
                    )
                }
            }
            route("/dokument/{journalpostId}/{dokumentinfoId}") {
                get<HentDokumentDTO, DokumentResponsDTO>(TagModule(listOf(Tags.Sak))) { req ->
                    val journalpostId = req.journalpostId
                    val dokumentInfoId = req.dokumentinfoId

                    val token = token()
                    val gateway = SafHentDokumentGateway.withDefaultRestClient()

                    val dokumentRespons =
                        gateway.hentDokument(JournalpostId(journalpostId), DokumentInfoId(dokumentInfoId), token)

                    pipeline.call.response.headers.append(
                        name = "Content-Disposition", value = "inline; filename=${dokumentRespons.filnavn}"
                    )
                    respond(DokumentResponsDTO(stream = dokumentRespons.dokument))
                }
            }

            route("/{saksnummer}/personinformasjon") {
                authorizedGet<HentSakDTO, SakPersoninfoDTO>(
                    AuthorizationParamPathConfig(
                        sakPathParam = SakPathParam("saksnummer"),
                        approvedApplications = setOf(brevAzp)
                    )
                ) { req ->

                    val saksnummer = req.saksnummer

                    val ident = dataSource.transaction(readOnly = true) { connection ->
                        val repositoryProvider = RepositoryProvider(connection)
                        val sak =
                            repositoryProvider.provide(SakRepository::class).hent(saksnummer = Saksnummer(saksnummer))
                        sak.person.aktivIdent()
                    }

                    val personinfo =
                        GatewayProvider.provide(PersoninfoGateway::class).hentPersoninfoForIdent(ident, token())

                    respond(
                        SakPersoninfoDTO(
                            fnr = personinfo.ident.identifikator,
                            navn = personinfo.fulltNavn(),
                        )
                    )
                }
            }
        }
    }
}
