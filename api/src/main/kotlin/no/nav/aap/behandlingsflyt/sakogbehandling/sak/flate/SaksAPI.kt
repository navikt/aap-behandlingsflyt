package no.nav.aap.behandlingsflyt.sakogbehandling.sak.flate

import com.papsign.ktor.openapigen.route.TagModule
import com.papsign.ktor.openapigen.route.path.normal.NormalOpenAPIRoute
import com.papsign.ktor.openapigen.route.path.normal.get
import com.papsign.ktor.openapigen.route.path.normal.post
import com.papsign.ktor.openapigen.route.response.respond
import com.papsign.ktor.openapigen.route.route
import com.papsign.ktor.openapigen.route.tag
import no.nav.aap.behandlingsflyt.Tags
import no.nav.aap.behandlingsflyt.behandling.Resultat
import no.nav.aap.behandlingsflyt.behandling.ResultatUtleder
import no.nav.aap.behandlingsflyt.kontrakt.behandling.TypeBehandling
import no.nav.aap.behandlingsflyt.kontrakt.sak.Saksnummer
import no.nav.aap.behandlingsflyt.kontrakt.sak.Status
import no.nav.aap.behandlingsflyt.kontrakt.statistikk.ResultatKode
import no.nav.aap.behandlingsflyt.sakogbehandling.Ident
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingRepository
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.Årsak
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.IdentGateway
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.PersonOgSakService
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.PersoninfoGateway
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.SakRepository
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.adapters.DokumentInfoId
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.adapters.SafHentDokumentGateway
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.adapters.SafListDokument
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.adapters.SafListDokumentGateway
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.db.PersonRepository
import no.nav.aap.behandlingsflyt.tilgang.TilgangGateway
import no.nav.aap.komponenter.dbconnect.transaction
import no.nav.aap.komponenter.gateway.GatewayProvider
import no.nav.aap.komponenter.httpklient.auth.token
import no.nav.aap.komponenter.httpklient.exception.VerdiIkkeFunnetException
import no.nav.aap.komponenter.miljo.Miljø
import no.nav.aap.komponenter.miljo.MiljøKode
import no.nav.aap.komponenter.repository.RepositoryRegistry
import no.nav.aap.komponenter.type.Periode
import no.nav.aap.tilgang.AuthorizationBodyPathConfig
import no.nav.aap.tilgang.AuthorizationMachineToMachineConfig
import no.nav.aap.tilgang.AuthorizationParamPathConfig
import no.nav.aap.tilgang.JournalpostPathParam
import no.nav.aap.tilgang.Operasjon
import no.nav.aap.tilgang.SakPathParam
import no.nav.aap.tilgang.authorizedGet
import no.nav.aap.tilgang.authorizedPost
import no.nav.aap.verdityper.dokument.JournalpostId
import javax.sql.DataSource

fun NormalOpenAPIRoute.saksApi(dataSource: DataSource, repositoryRegistry: RepositoryRegistry) {
    route("/api/sak").tag(Tags.Sak) {
        route("/ekstern/finn").authorizedPost<Unit, List<SaksinfoDTO>, FinnSakForIdentDTO>(
            AuthorizationMachineToMachineConfig(authorizedRoles = listOf("finn-sak"))
        ) { _, dto ->
            val saker: List<SaksinfoDTO> = dataSource.transaction(readOnly = true) { connection ->
                val repositoryProvider = repositoryRegistry.provider(connection)
                val ident = Ident(dto.ident)
                val person = repositoryProvider.provide<PersonRepository>().finn(ident)
                val behandlingRepository = repositoryProvider.provide<BehandlingRepository>()
                val resultatUtleder = ResultatUtleder(repositoryProvider)

                if (person == null) {
                    emptyList()
                } else {
                    repositoryProvider.provide<SakRepository>().finnSakerFor(person)
                        .map { sak ->
                            val førstegangsbehandling = if (sak.status() == Status.AVSLUTTET) {
                                behandlingRepository.hentAlleFor(sak.id).first {
                                    it.typeBehandling() == TypeBehandling.Førstegangsbehandling
                                }
                            } else null

                            val resultat = if (førstegangsbehandling != null) {
                                resultatUtleder.utledResultatFørstegangsBehandling(førstegangsbehandling)
                            } else null

                            SaksinfoDTO(
                                saksnummer = sak.saksnummer.toString(),
                                opprettetTidspunkt = sak.opprettetTidspunkt,
                                periode = sak.rettighetsperiode,
                                ident = sak.person.aktivIdent().identifikator,
                                resultat = resultat.let {
                                    when (it) {
                                        Resultat.INNVILGELSE -> ResultatKode.INNVILGET
                                        Resultat.AVSLAG -> ResultatKode.AVSLAG
                                        Resultat.TRUKKET -> ResultatKode.TRUKKET
                                        null -> null
                                    }
                                }
                            )
                        }
                }

            }
            respond(saker)
        }
        @Suppress("UnauthorizedPost")
        route("/finn").post<Unit, List<SaksinfoDTO>, FinnSakForIdentDTO> { _, dto ->
            val saker: List<SaksinfoDTO> = dataSource.transaction(readOnly = true) { connection ->
                val repositoryProvider = repositoryRegistry.provider(connection)
                val ident = Ident(dto.ident)
                val person = repositoryProvider.provide<PersonRepository>().finn(ident)
                val behandlingRepository = repositoryProvider.provide<BehandlingRepository>()
                val resultatUtleder = ResultatUtleder(repositoryProvider)

                if (person == null) {
                    emptyList()
                } else {
                    // Her skal vi strengt tatt bare ha én sak?
                    val saker = repositoryProvider.provide<SakRepository>().finnSakerFor(person)

                    saker.map { sak ->
                        val førstegangsbehandling = if (sak.status() == Status.AVSLUTTET) {
                            behandlingRepository.hentAlleFor(sak.id).first {
                                it.typeBehandling() == TypeBehandling.Førstegangsbehandling
                            }
                        } else null

                        val resultat = if (førstegangsbehandling != null) {
                            resultatUtleder.utledResultatFørstegangsBehandling(førstegangsbehandling)
                        } else null

                        SaksinfoDTO(
                            saksnummer = sak.saksnummer.toString(),
                            opprettetTidspunkt = sak.opprettetTidspunkt,
                            periode = sak.rettighetsperiode,
                            ident = sak.person.aktivIdent().identifikator,
                            resultat = resultat.let {
                                when (it) {
                                    Resultat.INNVILGELSE -> ResultatKode.INNVILGET
                                    Resultat.AVSLAG -> ResultatKode.AVSLAG
                                    Resultat.TRUKKET -> ResultatKode.TRUKKET
                                    null -> null
                                }
                            }
                        )
                    }
                }

            }
            // Midlertidig fiks for ikke å brekke postmottak
            if (token().isClientCredentials()) {
                respond(saker)
            } else {
                val sakerMedTilgang =
                    saker.filter { sak ->
                        GatewayProvider.provide<TilgangGateway>().sjekkTilgangTilSak(
                            Saksnummer(sak.saksnummer),
                            token(),
                            Operasjon.SE
                        )
                    }

                if (sakerMedTilgang.isNotEmpty()) {
                    respond(sakerMedTilgang)
                } else {
                    // TODO:
                    //  Bedre skille på om sak faktisk ikke finnes eller om sb mangler tilgang (unntak på gradering)
                    throw VerdiIkkeFunnetException("Fant ikke sak for ident")
                }
            }
        }

        route("/finnSisteBehandlinger") {
            authorizedPost<Unit, NullableSakOgBehandlingDTO, FinnBehandlingForIdentDTO>(
                modules = arrayOf(TagModule(listOf(Tags.Sak))),
                routeConfig = AuthorizationBodyPathConfig(
                    operasjon = Operasjon.SAKSBEHANDLE,
                    applicationsOnly = true,
                    applicationRole = "finn-siste-behandlinger",
                )
            )
            { _, dto ->
                val behandlinger: SakOgBehandlingDTO? = dataSource.transaction(readOnly = true) { connection ->
                    val repositoryProvider = repositoryRegistry.provider(connection)
                    val ident = Ident(dto.ident)
                    val person = repositoryProvider.provide<PersonRepository>().finn(ident)

                    if (person == null) {
                        null
                    } else {
                        val sak = repositoryProvider.provide<SakRepository>().finnSakerFor(person)
                            .filter { sak ->
                                sak.rettighetsperiode.inneholder(dto.mottattTidspunkt) && sak.status() != Status.AVSLUTTET
                            }.minByOrNull { it.opprettetTidspunkt }!!

                        val behandling =
                            repositoryProvider.provide<BehandlingRepository>()
                                .finnSisteBehandlingFor(
                                    sak.id,
                                    behandlingstypeFilter = listOf(
                                        TypeBehandling.Førstegangsbehandling,
                                        TypeBehandling.Revurdering
                                    )
                                )

                        SakOgBehandlingDTO(
                            personIdent = sak.person.aktivIdent().toString(),
                            saksnummer = sak.saksnummer.toString(),
                            status = sak.status().toString(),
                            sisteBehandlingStatus = behandling?.status().toString()
                        )
                    }
                }
                respond(NullableSakOgBehandlingDTO(behandlinger))
            }
        }

        route("/finnEllerOpprett") {
            authorizedPost<Unit, SaksinfoDTO, FinnEllerOpprettSakDTO>(
                modules = arrayOf(TagModule(listOf(Tags.Sak))),
                routeConfig = AuthorizationBodyPathConfig(
                    operasjon = Operasjon.SAKSBEHANDLE,
                    applicationsOnly = true,
                    applicationRole = "opprett-sak",
                )
            ) { _, dto ->
                val saken: SaksinfoDTO = dataSource.transaction { connection ->
                    val repositoryProvider = repositoryRegistry.provider(connection)
                    val ident = Ident(dto.ident)
                    val periode = Periode(
                        dto.søknadsdato, dto.søknadsdato.plusYears(1).minusDays(1)
                    ) // Setter til fra og med dagens dato, til og med et år frem i tid minus en dag som er tilsvarende "vedtakslengde" i forskriften
                    val sak = PersonOgSakService(
                        pdlGateway = GatewayProvider.provide(IdentGateway::class),
                        personRepository = repositoryProvider.provide<PersonRepository>(),
                        sakRepository = repositoryProvider.provide<SakRepository>()
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
        }

        route("") {
            @Suppress("UnauthorizedGet") // saksoversikt er bare tilgjengelig i DEV og lokalt
            route("/alle").get<Unit, List<SaksinfoDTO>>(TagModule(listOf(Tags.Sak))) {
                if (Miljø.er() == MiljøKode.DEV || Miljø.er() == MiljøKode.LOKALT) {
                    val saker: List<SaksinfoDTO> = dataSource.transaction(readOnly = true) { connection ->
                        val repositoryProvider = repositoryRegistry.provider(connection)
                        repositoryProvider.provide<SakRepository>().finnAlle().map { sak ->
                            SaksinfoDTO(
                                saksnummer = sak.saksnummer.toString(),
                                opprettetTidspunkt = sak.opprettetTidspunkt,
                                periode = sak.rettighetsperiode,
                                ident = sak.person.aktivIdent().identifikator
                            )
                        }
                    }
                    respond(saker)
                } else {
                    throw VerdiIkkeFunnetException("Fant ingen saker")
                }
            }
        }

        route("/{saksnummer}") {
            authorizedGet<HentSakDTO, UtvidetSaksinfoDTO>(
                AuthorizationParamPathConfig(
                    sakPathParam = SakPathParam("saksnummer")
                ),
                null,
                TagModule(listOf(Tags.Sak)),
            ) { req ->
                val saksnummer = req.saksnummer
                var søknadErTrukket: Boolean? = null
                val (sak, behandlinger) = dataSource.transaction(readOnly = true) { connection ->
                    val repositoryProvider = repositoryRegistry.provider(connection)
                    val resultatUtleder = ResultatUtleder(repositoryProvider)
                    val sak = repositoryProvider.provide<SakRepository>()
                        .hent(saksnummer = Saksnummer(saksnummer))

                    val behandlinger =
                        repositoryProvider.provide<BehandlingRepository>().hentAlleFor(sak.id)
                            .map { behandling ->
                                if (behandling.typeBehandling() == TypeBehandling.Førstegangsbehandling) {
                                    søknadErTrukket =
                                        resultatUtleder.utledResultatFørstegangsBehandling(behandling) == Resultat.TRUKKET
                                }
                                BehandlinginfoDTO(
                                    referanse = behandling.referanse.referanse,
                                    type = behandling.typeBehandling().identifikator(),
                                    status = behandling.status(),
                                    årsaker = behandling.årsaker().map(Årsak::type),
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
                        ident = sak.person.aktivIdent().identifikator,
                        behandlinger = behandlinger,
                        status = sak.status(),
                        søknadErTrukket = søknadErTrukket
                    )
                )
            }
        }

        // TODO: Fjerne når DokumentAPI er testet OK
        route("/{saksnummer}/dokumenter") {
            authorizedGet<HentSakDTO, List<SafListDokument>>(
                AuthorizationParamPathConfig(
                    sakPathParam = SakPathParam("saksnummer")
                ), null, TagModule(listOf(Tags.Sak))
            ) { req ->
                val token = token()
                val safRespons = SafListDokumentGateway.hentDokumenterForSak(Saksnummer(req.saksnummer), token)
                respond(
                    safRespons
                )
            }
        }
        // TODO: Fjerne når DokumentAPI er testet OK
        route("/dokument/{journalpostId}/{dokumentinfoId}") {
            authorizedGet<HentDokumentDTO, DokumentResponsDTO>(
                AuthorizationParamPathConfig(
                    journalpostPathParam = JournalpostPathParam(
                        "journalpostId"
                    )
                )
            ) { req ->
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
                    applicationRole = "hent-personinfo"
                )
            ) { req ->

                val saksnummer = req.saksnummer

                val ident = dataSource.transaction(readOnly = true) { connection ->
                    val repositoryProvider = repositoryRegistry.provider(connection)
                    val sak =
                        repositoryProvider.provide<SakRepository>()
                            .hent(saksnummer = Saksnummer(saksnummer))
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
