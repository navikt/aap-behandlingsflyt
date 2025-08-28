package no.nav.aap.behandlingsflyt.sakogbehandling.sak.flate

import com.papsign.ktor.openapigen.route.TagModule
import com.papsign.ktor.openapigen.route.path.normal.NormalOpenAPIRoute
import com.papsign.ktor.openapigen.route.path.normal.get
import com.papsign.ktor.openapigen.route.path.normal.post
import com.papsign.ktor.openapigen.route.path.normal.route
import com.papsign.ktor.openapigen.route.response.respond
import com.papsign.ktor.openapigen.route.route
import com.papsign.ktor.openapigen.route.tag
import no.nav.aap.behandlingsflyt.Azp
import no.nav.aap.behandlingsflyt.Tags
import no.nav.aap.behandlingsflyt.behandling.Resultat
import no.nav.aap.behandlingsflyt.behandling.ResultatUtleder
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.AvklaringsbehovOperasjonerRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.SakOgBehandlingService
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.AVKLAR_STUDENT_KODE
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.AvklaringsbehovKode
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.Definisjon
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.OPPRETT_HENDELSE_PÅ_SAK_KODE
import no.nav.aap.behandlingsflyt.kontrakt.behandling.TypeBehandling
import no.nav.aap.behandlingsflyt.kontrakt.sak.Saksnummer
import no.nav.aap.behandlingsflyt.kontrakt.sak.Status
import no.nav.aap.behandlingsflyt.kontrakt.statistikk.ResultatKode
import no.nav.aap.behandlingsflyt.medAzureTokenGen
import no.nav.aap.behandlingsflyt.sakogbehandling.Ident
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingRepository
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.VurderingsbehovMedPeriode
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.VurderingsbehovOgÅrsak
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.ÅrsakTilOpprettelse
import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.Vurderingsbehov
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.IdentGateway
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.PersonOgSakService
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.PersoninfoGateway
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.SakRepository
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.db.PersonRepository
import no.nav.aap.behandlingsflyt.tilgang.TilgangGateway
import no.nav.aap.komponenter.dbconnect.transaction
import no.nav.aap.komponenter.gateway.GatewayProvider
import no.nav.aap.komponenter.httpklient.exception.UgyldigForespørselException
import no.nav.aap.komponenter.httpklient.exception.VerdiIkkeFunnetException
import no.nav.aap.komponenter.miljo.Miljø
import no.nav.aap.komponenter.miljo.MiljøKode
import no.nav.aap.komponenter.repository.RepositoryRegistry
import no.nav.aap.komponenter.server.auth.token
import no.nav.aap.komponenter.type.Periode
import no.nav.aap.tilgang.AuthorizationBodyPathConfig
import no.nav.aap.tilgang.AuthorizationMachineToMachineConfig
import no.nav.aap.tilgang.AuthorizationParamPathConfig
import no.nav.aap.tilgang.Operasjon
import no.nav.aap.tilgang.SakPathParam
import no.nav.aap.tilgang.authorizedGet
import no.nav.aap.tilgang.authorizedPost
import javax.sql.DataSource

fun NormalOpenAPIRoute.saksApi(
    dataSource: DataSource,
    repositoryRegistry: RepositoryRegistry,
    gatewayProvider: GatewayProvider,
) {
    val tilgangGateway = gatewayProvider.provide<TilgangGateway>()
    val identGateway = gatewayProvider.provide(IdentGateway::class)
    val personinfoGateway = gatewayProvider.provide(PersoninfoGateway::class)

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
        route("/{saksnummer}/opprettAktivitetspliktBehandling").authorizedPost<SaksnummerParameter, BehandlingAvTypeDTO,Unit> (
            routeConfig =  AuthorizationParamPathConfig(
                sakPathParam = SakPathParam("saksnummer"),
                operasjon = Operasjon.SAKSBEHANDLE,
                avklaringsbehovKode = AvklaringsbehovKode.`5028`

            )
        ){ req,_ ->
            dataSource.transaction {connection ->
                val repositoryProvider = repositoryRegistry.provider(connection)

                val behandlingRepository = repositoryProvider.provide<BehandlingRepository>()

                val sakRepository = repositoryProvider.provide<SakRepository>()
                val sakId = sakRepository.hent(Saksnummer(req.saksnummer)).id

                val sakOgBehandlingService = SakOgBehandlingService(repositoryProvider,gatewayProvider)

                val sisteYtelseBehandling = sakOgBehandlingService.finnSisteYtelsesbehandlingFor(sakId)

                if (sisteYtelseBehandling == null){
                    throw UgyldigForespørselException("Kan ikke opprette aktiviterspliktbehandling uten en ytelsebehandling")
                }
                val aktivitetspliktBehandlinger = behandlingRepository
                    .hentAlleFor(sakId = sakId, behandlingstypeFilter = listOf(TypeBehandling.Aktivitetsplikt))

                val åpeneAktivitetspliktBehandling = aktivitetspliktBehandlinger.filter { it.status().erÅpen() }


                if (åpeneAktivitetspliktBehandling.isNotEmpty()){
                    throw UgyldigForespørselException("Finnes allerede en åpen behandling for aktivitetsplikt")
                }

                val behandling = sakOgBehandlingService.opprettAktivitetsPliktBrudd(
                    sakId, VurderingsbehovOgÅrsak(
                        vurderingsbehov = listOf(VurderingsbehovMedPeriode(Vurderingsbehov.AKTIVITETSPLIKT_11_7)),
                        årsak = ÅrsakTilOpprettelse.AKTIVITETSPLIKT
                    ),
                    forrigeBehandlingId = aktivitetspliktBehandlinger.firstOrNull()?.id
                )
                BehandlingAvTypeDTO(
                    behandlingsReferanse = behandling.referanse.referanse,
                    opprettetDato = behandling.opprettetTidspunkt
                )


            }


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
            val token = token()
            if (token.isClientCredentials()) {
                respond(saker)
            } else {
                val sakerMedTilgang =
                    saker.filter { sak ->
                        tilgangGateway.sjekkTilgangTilSak(
                            Saksnummer(sak.saksnummer),
                            token,
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
                    val sakOgBehandlingService = SakOgBehandlingService(repositoryProvider, gatewayProvider)
                    val ident = Ident(dto.ident)
                    val person = repositoryProvider.provide<PersonRepository>().finn(ident)

                    if (person == null) {
                        null
                    } else {
                        val sak = repositoryProvider.provide<SakRepository>().finnSakerFor(person)
                            .filter { sak ->
                                sak.rettighetsperiode.inneholder(dto.mottattTidspunkt) && sak.status() != Status.AVSLUTTET
                            }.minByOrNull { it.opprettetTidspunkt }!!

                        val behandling = sakOgBehandlingService.finnSisteYtelsesbehandlingFor(sak.id)

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
                        pdlGateway = identGateway,
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
                                val vurderingsbehov = behandling.vurderingsbehov().map(VurderingsbehovMedPeriode::type)
                                BehandlinginfoDTO(
                                    referanse = behandling.referanse.referanse,
                                    type = behandling.typeBehandling().identifikator(),
                                    status = behandling.status(),
                                    vurderingsbehov = vurderingsbehov,
                                    årsakTilOpprettelse = behandling.årsakTilOpprettelse,
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

        route("/{saksnummer}/finnBehandlingerAvType") {
            authorizedPost<SaksnummerParameter, List<BehandlingAvTypeDTO>, TypeBehandling>(
                routeConfig = AuthorizationMachineToMachineConfig(
                    authorizedAzps = listOf(Azp.Postmottak.uuid)
                ).medAzureTokenGen()
            ) { saksnummer, body ->
                val behandlinger = dataSource.transaction { connection ->
                    val sakRepository = repositoryRegistry.provider(connection).provide<SakRepository>()
                    val behandlingRepository =
                        repositoryRegistry.provider(connection).provide<BehandlingRepository>()
                    val sakId = sakRepository.hent(Saksnummer(saksnummer.saksnummer)).id

                    val behandlinger = behandlingRepository.hentAlleFor(sakId)

                    behandlinger.filter { it.typeBehandling() == body }
                        .map {
                            BehandlingAvTypeDTO(
                                it.referanse.referanse,
                                it.opprettetTidspunkt
                            )
                        }


                }
                respond(behandlinger)
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
                    personinfoGateway.hentPersoninfoForIdent(ident, token())

                respond(
                    SakPersoninfoDTO(
                        fnr = personinfo.ident.identifikator,
                        navn = personinfo.fulltNavn(),
                    )
                )
            }
        }

        route("{saksnummer}/historikk").authorizedGet<HentSakDTO, List<BehandlingHistorikkDTO>>(
            AuthorizationParamPathConfig(
                sakPathParam = SakPathParam("saksnummer")
            ),
            null,
            TagModule(listOf(Tags.Sak)),
        ) { req ->
            val saksnummer = req.saksnummer

            val historikk =
                dataSource.transaction(readOnly = true) { connection ->
                    val repositoryProvider = repositoryRegistry.provider(connection)
                    val sakRepository = repositoryProvider.provide<SakRepository>()

                    val sakId = sakRepository.hent(Saksnummer(saksnummer)).id
                    val saksHistorikkService = SaksHistorikkService(repositoryProvider)

                    saksHistorikkService.utledSaksHistorikk(sakId)
                }
            respond(historikk)
        }
    }
}