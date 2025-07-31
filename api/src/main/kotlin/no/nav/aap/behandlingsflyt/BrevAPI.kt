package no.nav.aap.behandlingsflyt

import com.papsign.ktor.openapigen.route.path.normal.NormalOpenAPIRoute
import com.papsign.ktor.openapigen.route.response.respond
import com.papsign.ktor.openapigen.route.route
import io.ktor.http.*
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.AvklaringsbehovHendelseHåndterer
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.AvklaringsbehovRepository
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.Avklaringsbehovene
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.LøsAvklaringsbehovHendelse
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løser.BREV_SYSTEMBRUKER
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løsning.BrevbestillingLøsning
import no.nav.aap.behandlingsflyt.behandling.brev.BrevGrunnlag
import no.nav.aap.behandlingsflyt.behandling.brev.BrevGrunnlag.Brev.Mottaker
import no.nav.aap.behandlingsflyt.behandling.brev.FaktagrunnlagService
import no.nav.aap.behandlingsflyt.behandling.brev.SignaturService
import no.nav.aap.behandlingsflyt.behandling.brev.VarselOmBrevbestillingDto
import no.nav.aap.behandlingsflyt.behandling.brev.bestilling.BrevbestillingGateway
import no.nav.aap.behandlingsflyt.behandling.brev.bestilling.BrevbestillingReferanse
import no.nav.aap.behandlingsflyt.behandling.brev.bestilling.BrevbestillingRepository
import no.nav.aap.behandlingsflyt.behandling.brev.bestilling.BrevbestillingService
import no.nav.aap.behandlingsflyt.behandling.brev.bestilling.Status
import no.nav.aap.behandlingsflyt.behandling.brev.bestilling.TypeBrev
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.Definisjon
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.SKRIV_BREV_KODE
import no.nav.aap.behandlingsflyt.kontrakt.behandling.BehandlingReferanse
import no.nav.aap.behandlingsflyt.kontrakt.brevbestilling.FaktagrunnlagDto
import no.nav.aap.behandlingsflyt.kontrakt.brevbestilling.HentFaktaGrunnlagRequest
import no.nav.aap.behandlingsflyt.kontrakt.brevbestilling.LøsBrevbestillingDto
import no.nav.aap.behandlingsflyt.mdc.LogKontekst
import no.nav.aap.behandlingsflyt.mdc.LoggingKontekst
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingRepository
import no.nav.aap.behandlingsflyt.sakogbehandling.lås.TaSkriveLåsRepository
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.PersoninfoGateway
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.SakRepository
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.SakService
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.flate.DokumentResponsDTO
import no.nav.aap.behandlingsflyt.tilgang.TilgangGateway
import no.nav.aap.behandlingsflyt.unleash.BehandlingsflytFeature
import no.nav.aap.behandlingsflyt.unleash.UnleashGateway
import no.nav.aap.brev.kontrakt.Brev
import no.nav.aap.komponenter.dbconnect.transaction
import no.nav.aap.komponenter.gateway.GatewayProvider
import no.nav.aap.komponenter.verdityper.Bruker
import no.nav.aap.komponenter.server.auth.bruker
import no.nav.aap.komponenter.server.auth.token
import no.nav.aap.komponenter.httpklient.httpclient.tokenprovider.OidcToken
import no.nav.aap.komponenter.repository.RepositoryRegistry
import no.nav.aap.tilgang.AuthorizationBodyPathConfig
import no.nav.aap.tilgang.AuthorizationParamPathConfig
import no.nav.aap.tilgang.BehandlingPathParam
import no.nav.aap.tilgang.Operasjon
import no.nav.aap.tilgang.authorizedGet
import no.nav.aap.tilgang.authorizedPost
import no.nav.aap.tilgang.authorizedPut
import org.slf4j.LoggerFactory
import org.slf4j.MDC
import java.util.*
import javax.sql.DataSource

private val log = LoggerFactory.getLogger("BrevAPI")
fun NormalOpenAPIRoute.brevApi(dataSource: DataSource, repositoryRegistry: RepositoryRegistry) {
    val authorizationParamPathConfig = AuthorizationParamPathConfig(
        operasjon = Operasjon.SAKSBEHANDLE,
        avklaringsbehovKode = SKRIV_BREV_KODE,
        behandlingPathParam = BehandlingPathParam(
            param = "brevbestillingReferanse",
            resolver = {
                val brevbestillingReferanse = BrevbestillingReferanse(UUID.fromString(it))
                dataSource.transaction { connection ->
                    val repositoryProvider = repositoryRegistry.provider(connection)
                    val brevbestillingRepository =
                        repositoryProvider.provide<BrevbestillingRepository>()
                    val behandlingRepository =
                        repositoryProvider.provide<BehandlingRepository>()

                    val behandlingId =
                        brevbestillingRepository.hent(brevbestillingReferanse).behandlingId

                    behandlingRepository.hent(behandlingId).referanse.referanse
                }
            })
    )


    val brevbestillingGateway = GatewayProvider.provide<BrevbestillingGateway>()
    route("/api") {
        route("/behandling") {
            route("/{referanse}/grunnlag/brev") {
                authorizedGet<BehandlingReferanse, BrevGrunnlag>(
                    AuthorizationParamPathConfig(
                        behandlingPathParam = BehandlingPathParam(
                            "referanse"
                        )
                    )
                ) { behandlingReferanse ->
                    val brevGrunnlag = dataSource.transaction(readOnly = true) { connection ->
                        val repositoryProvider = repositoryRegistry.provider(connection)
                        val behandlingRepository = repositoryProvider.provide<BehandlingRepository>()
                        val sakRepository = repositoryProvider.provide<SakRepository>()
                        val brevbestillingRepository = repositoryProvider.provide<BrevbestillingRepository>()
                        val avklaringsbehovRepository = repositoryProvider.provide<AvklaringsbehovRepository>()
                        val signaturService = SignaturService(avklaringsbehovRepository = avklaringsbehovRepository)
                        val brevbestillingService = BrevbestillingService(
                            signaturService = signaturService,
                            brevbestillingGateway = brevbestillingGateway,
                            brevbestillingRepository = brevbestillingRepository,
                            behandlingRepository = behandlingRepository,
                            sakRepository = sakRepository
                        )
                        val brevbestillinger = brevbestillingService.hentBrevbestillinger(behandlingReferanse)

                        val behandling = behandlingRepository.hent(behandlingReferanse)
                        val avklaringsbehovene = avklaringsbehovRepository.hentAvklaringsbehovene(behandling.id)
                        val sak = SakService(sakRepository).hent(behandling.sakId)
                        val personIdent = sak.person.aktivIdent()
                        val personinfo =
                            GatewayProvider.provide(PersoninfoGateway::class)
                                .hentPersoninfoForIdent(personIdent, token())

                        val skrivBrevAvklaringsbehov = avklaringsbehovene
                            .hentBehovForDefinisjon(
                                listOf(
                                    Definisjon.SKRIV_BREV,
                                    Definisjon.SKRIV_VEDTAKSBREV,
                                    Definisjon.SKRIV_FORHÅNDSVARSEL_BRUDD_AKTIVITETSPLIKT_BREV,
                                    Definisjon.SKRIV_FORHÅNDSVARSEL_KLAGE_FORMKRAV_BREV
                                )
                            )
                            .filter { it.erÅpent() }

                        if (skrivBrevAvklaringsbehov.size > 1) {
                            log.warn(
                                "Fant flere åpne avklaringsbehov for å skrive brev for behandling ${behandling.id}: "
                                        + skrivBrevAvklaringsbehov.joinToString { it.toString() })
                        }

                        val grunnlag = brevbestillinger.map { brevbestilling ->
                            val brevbestillingResponse =
                                brevbestillingService.hentBrevbestilling(brevbestilling.referanse)

                            val signaturer = if (brevbestilling.status == Status.FORHÅNDSVISNING_KLAR) {
                                brevbestillingGateway.hentSignaturForhåndsvisning(
                                    signaturService.finnSignaturGrunnlag(brevbestilling, bruker()),
                                    personIdent.identifikator,
                                    brevbestilling.typeBrev
                                )
                            } else {
                                emptyList()
                            }
                            val definisjon = when {
                                brevbestilling.typeBrev.erVedtak() &&
                                        skrivBrevAvklaringsbehov.any { it.definisjon == Definisjon.SKRIV_VEDTAKSBREV }
                                    -> {
                                    Definisjon.SKRIV_VEDTAKSBREV
                                }

                                brevbestilling.typeBrev == TypeBrev.FORHÅNDSVARSEL_BRUDD_AKTIVITETSPLIKT &&
                                        skrivBrevAvklaringsbehov.any { it.definisjon == Definisjon.SKRIV_FORHÅNDSVARSEL_BRUDD_AKTIVITETSPLIKT_BREV } -> {
                                    Definisjon.SKRIV_FORHÅNDSVARSEL_BRUDD_AKTIVITETSPLIKT_BREV
                                }

                                brevbestilling.typeBrev == TypeBrev.FORHÅNDSVARSEL_KLAGE_FORMKRAV && skrivBrevAvklaringsbehov.any { it.definisjon == Definisjon.SKRIV_FORHÅNDSVARSEL_KLAGE_FORMKRAV_BREV } -> {
                                    Definisjon.SKRIV_FORHÅNDSVARSEL_KLAGE_FORMKRAV_BREV
                                }

                                else -> {
                                    Definisjon.SKRIV_BREV
                                }
                            }

                            BrevGrunnlag.Brev(
                                avklaringsbehovKode = definisjon.kode,
                                brevbestillingReferanse = brevbestillingResponse.referanse,
                                brev = brevbestillingResponse.brev,
                                opprettet = brevbestillingResponse.opprettet,
                                oppdatert = brevbestillingResponse.oppdatert,
                                brevtype = brevbestillingResponse.brevtype,
                                språk = brevbestillingResponse.språk,
                                status = when (brevbestillingResponse.status) {
                                    no.nav.aap.brev.kontrakt.Status.REGISTRERT -> Status.SENDT
                                    no.nav.aap.brev.kontrakt.Status.UNDER_ARBEID -> Status.FORHÅNDSVISNING_KLAR
                                    no.nav.aap.brev.kontrakt.Status.FERDIGSTILT -> Status.FULLFØRT
                                    no.nav.aap.brev.kontrakt.Status.AVBRUTT -> Status.AVBRUTT
                                },
                                mottaker = Mottaker(
                                    navn = personinfo.fulltNavn(),
                                    ident = personinfo.ident.identifikator
                                ),
                                signaturer = signaturer,
                                harTilgangTilÅSendeBrev = utledHarTilgangTilÅSendeBrev(
                                    behandlingReferanse.referanse,
                                    token(),
                                    avklaringsbehovene,
                                    bruker(),
                                    definisjon
                                )
                            )
                        }

                        BrevGrunnlag(
                            grunnlag
                        )
                    }

                    respond(brevGrunnlag)
                }
            }
        }
        route("/brev") {
            route("/bestillingvarsel") {
                authorizedPost<Unit, String, VarselOmBrevbestillingDto>(
                    AuthorizationBodyPathConfig(
                        operasjon = Operasjon.SAKSBEHANDLE,
                        applicationRole = "bestill-varselbrev",
                        applicationsOnly = true
                    )
                ) { _, req ->
                    dataSource.transaction { connection ->
                        val repositoryProvider = repositoryRegistry.provider(connection)

                        LoggingKontekst(
                            repositoryProvider,
                            LogKontekst(referanse = req.behandlingsReferanse)
                        ).use {
                            val behandlingRepository =
                                repositoryProvider.provide<BehandlingRepository>()

                            val behandling =
                                behandlingRepository.hent(req.behandlingsReferanse)

                            val brevbestillingService = BrevbestillingService(repositoryProvider)

                            brevbestillingService.bestillV2(
                                behandlingId = behandling.id,
                                typeBrev = TypeBrev.VARSEL_OM_BESTILLING,
                                unikReferanse = req.dialogmeldingUuid.toString(),
                                ferdigstillAutomatisk = true,
                                vedlegg = req.vedlegg
                            )
                        }
                    }
                    respond("{}", HttpStatusCode.Accepted)
                }
            }
            route("/{brevbestillingReferanse}/oppdater") {
                authorizedPut<BrevbestillingReferanse, String, Brev>(authorizationParamPathConfig) { brevbestillingReferanse, brev ->
                    brevbestillingGateway.oppdater(brevbestillingReferanse, brev)
                    respond("{}", HttpStatusCode.Accepted)
                }
            }
            route("/{brevbestillingReferanse}/forhandsvis") {
                authorizedGet<BrevbestillingReferanse, DokumentResponsDTO>(authorizationParamPathConfig) { brevbestillingReferanse ->
                    val pdf = dataSource.transaction { connection ->
                        val repositoryProvider = repositoryRegistry.provider(connection)
                        val brevbestillingRepository =
                            repositoryProvider.provide<BrevbestillingRepository>()
                        val avklaringsbehovRepository = repositoryProvider.provide<AvklaringsbehovRepository>()

                        val brevbestilling = brevbestillingRepository.hent(brevbestillingReferanse)

                        val signaturService = SignaturService(avklaringsbehovRepository = avklaringsbehovRepository)
                        brevbestillingGateway.forhåndsvis(
                            bestillingReferanse = brevbestillingReferanse,
                            signaturer = signaturService.finnSignaturGrunnlag(brevbestilling, bruker()),
                        )
                    }
                    respond(DokumentResponsDTO(pdf))
                }
            }
            route("/los-bestilling") {
                authorizedPost<Unit, String, LøsBrevbestillingDto>(
                    AuthorizationBodyPathConfig(
                        operasjon = Operasjon.SAKSBEHANDLE,
                        applicationRole = "brev",
                        applicationsOnly = true
                    )
                ) { _, request ->
                    dataSource.transaction { connection ->
                        val repositoryProvider = repositoryRegistry.provider(connection)
                        val taSkriveLåsRepository =
                            repositoryProvider.provide<TaSkriveLåsRepository>()

                        val lås = taSkriveLåsRepository.lås(request.behandlingReferanse)

                        val behandlingRepository =
                            repositoryProvider.provide<BehandlingRepository>()

                        MDC.putCloseable("sakId", lås.sakSkrivelås.id.toString()).use {
                            MDC.putCloseable("behandlingId", lås.behandlingSkrivelås.id.toString())
                                .use {
                                    val behandling =
                                        behandlingRepository.hent(lås.behandlingSkrivelås.id)

                                    AvklaringsbehovHendelseHåndterer(repositoryProvider).håndtere(
                                        key = lås.behandlingSkrivelås.id,
                                        hendelse = LøsAvklaringsbehovHendelse(
                                            løsning = BrevbestillingLøsning(request),
                                            behandlingVersjon = behandling.versjon,
                                            bruker = BREV_SYSTEMBRUKER,
                                        )
                                    )

                                    taSkriveLåsRepository.verifiserSkrivelås(lås)
                                }
                        }
                    }
                    respond("{}", HttpStatusCode.Accepted)
                }
            }
            route("/faktagrunnlag") {
                authorizedPost<Unit, FaktagrunnlagDto, HentFaktaGrunnlagRequest>(
                    AuthorizationBodyPathConfig(
                        operasjon = Operasjon.SAKSBEHANDLE,
                        applicationRole = "brev",
                        applicationsOnly = true
                    )
                ) { _, request ->
                    val faktagrunnlag = dataSource.transaction { connection ->
                        val repositoryProvider = repositoryRegistry.provider(connection)
                        FaktagrunnlagService.konstruer(repositoryProvider)
                            .finnFaktagrunnlag(
                                behandlingReferanse = request.behandlingReferanse,
                                faktagrunnlag = request.faktagrunnlag
                            )
                    }
                    respond(FaktagrunnlagDto(faktagrunnlag))
                }
            }
        }
    }
}

private fun utledHarTilgangTilÅSendeBrev(
    behandlingReferanse: UUID,
    token: OidcToken,
    avklaringsbehovene: Avklaringsbehovene,
    bruker: Bruker,
    definisjon: Definisjon
): Boolean {
    val tilgangGateway = GatewayProvider.provide<TilgangGateway>()
    val unleashGateway = GatewayProvider.provide<UnleashGateway>()

    fun harTilgang(tilDefinisjon: Definisjon): Boolean =
        tilgangGateway.sjekkTilgangTilBehandling(behandlingReferanse, tilDefinisjon, token)

    return when (definisjon) {
        Definisjon.SKRIV_VEDTAKSBREV -> {
            val harTilgang = harTilgang(definisjon)
            if (!unleashGateway.isEnabled(BehandlingsflytFeature.IngenValidering, bruker.ident)) {
                val harIkkeGjortNoenVurderinger = avklaringsbehovene
                    .alle()
                    .filter { it.erTotrinn() }
                    .none { it.brukere().contains(bruker.ident) }
                harTilgang && harIkkeGjortNoenVurderinger
            } else {
                harTilgang
            }
        }

        Definisjon.SKRIV_FORHÅNDSVARSEL_BRUDD_AKTIVITETSPLIKT_BREV,
        Definisjon.SKRIV_FORHÅNDSVARSEL_KLAGE_FORMKRAV_BREV -> harTilgang(definisjon)

        else -> harTilgang(Definisjon.SKRIV_BREV)
    }
}
