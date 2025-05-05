package no.nav.aap.behandlingsflyt.behandling.brev

import com.papsign.ktor.openapigen.content.type.binary.BinaryResponse
import com.papsign.ktor.openapigen.route.path.normal.NormalOpenAPIRoute
import com.papsign.ktor.openapigen.route.response.respond
import com.papsign.ktor.openapigen.route.route
import io.ktor.http.*
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.AvklaringsbehovHendelseHåndterer
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.AvklaringsbehovOrkestrator
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.AvklaringsbehovRepository
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.LøsAvklaringsbehovHendelse
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løser.BREV_SYSTEMBRUKER
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løsning.BrevbestillingLøsning
import no.nav.aap.behandlingsflyt.behandling.brev.BrevGrunnlag.Brev.Mottaker
import no.nav.aap.behandlingsflyt.behandling.brev.bestilling.BrevbestillingGateway
import no.nav.aap.behandlingsflyt.behandling.brev.bestilling.BrevbestillingReferanse
import no.nav.aap.behandlingsflyt.behandling.brev.bestilling.BrevbestillingRepository
import no.nav.aap.behandlingsflyt.behandling.brev.bestilling.BrevbestillingService
import no.nav.aap.behandlingsflyt.behandling.brev.bestilling.Status
import no.nav.aap.behandlingsflyt.behandling.brev.bestilling.TypeBrev
import no.nav.aap.behandlingsflyt.hendelse.avløp.BehandlingHendelseServiceImpl
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.Definisjon
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.SKRIV_BREV_KODE
import no.nav.aap.behandlingsflyt.kontrakt.behandling.BehandlingReferanse
import no.nav.aap.behandlingsflyt.kontrakt.brevbestilling.FaktagrunnlagDto
import no.nav.aap.behandlingsflyt.kontrakt.brevbestilling.HentFaktaGrunnlagRequest
import no.nav.aap.behandlingsflyt.kontrakt.brevbestilling.LøsBrevbestillingDto
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingRepository
import no.nav.aap.behandlingsflyt.sakogbehandling.lås.TaSkriveLåsRepository
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.PersoninfoGateway
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.SakRepository
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.SakService
import no.nav.aap.behandlingsflyt.tilgang.TilgangGatewayImpl
import no.nav.aap.brev.kontrakt.Brev
import no.nav.aap.komponenter.dbconnect.transaction
import no.nav.aap.komponenter.gateway.GatewayProvider
import no.nav.aap.komponenter.httpklient.auth.bruker
import no.nav.aap.komponenter.httpklient.auth.token
import no.nav.aap.lookup.repository.RepositoryRegistry
import no.nav.aap.motor.FlytJobbRepository
import no.nav.aap.tilgang.AuthorizationBodyPathConfig
import no.nav.aap.tilgang.AuthorizationParamPathConfig
import no.nav.aap.tilgang.BehandlingPathParam
import no.nav.aap.tilgang.Operasjon
import no.nav.aap.tilgang.authorizedGet
import no.nav.aap.tilgang.authorizedPost
import no.nav.aap.tilgang.authorizedPut
import org.slf4j.LoggerFactory
import org.slf4j.MDC
import java.io.InputStream
import java.util.*
import javax.sql.DataSource

private val log = LoggerFactory.getLogger("BrevAPI")
fun NormalOpenAPIRoute.brevApi(dataSource: DataSource) {
    val authorizationParamPathConfig = AuthorizationParamPathConfig(
        operasjon = Operasjon.SAKSBEHANDLE,
        avklaringsbehovKode = SKRIV_BREV_KODE,
        behandlingPathParam = BehandlingPathParam(
            param = "brevbestillingReferanse",
            resolver = {
                val brevbestillingReferanse = BrevbestillingReferanse(UUID.fromString(it))
                dataSource.transaction { connection ->
                    val repositoryProvider = RepositoryRegistry.provider(connection)
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
                    val grunnlag = dataSource.transaction(readOnly = true) { connection ->
                        val repositoryProvider = RepositoryRegistry.provider(connection)
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
                        val sak = SakService(sakRepository).hent(behandling.sakId)
                        val personIdent = sak.person.aktivIdent()
                        val personinfo =
                            GatewayProvider.provide(PersoninfoGateway::class)
                                .hentPersoninfoForIdent(personIdent, token())

                        val skrivBrevAvklaringsbehov = avklaringsbehovRepository
                            .hentAvklaringsbehovene(behandling.id)
                            .hentBehovForDefinisjon(listOf(Definisjon.SKRIV_BREV, Definisjon.SKRIV_VEDTAKSBREV))
                            .filter { it.erÅpent() }

                        if (skrivBrevAvklaringsbehov.size > 1) {
                            log.warn("Fant flere avklaringsbehov for å skrive brev for behandling ${behandling.id}: "
                            + skrivBrevAvklaringsbehov.joinToString { it.toString() })
                        }

                        brevbestillinger.map { brevbestilling ->
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
                            val definisjon =
                                if (brevbestilling.typeBrev.erVedtak() &&
                                        skrivBrevAvklaringsbehov.any { it.definisjon == Definisjon.SKRIV_VEDTAKSBREV }) {
                                    Definisjon.SKRIV_VEDTAKSBREV
                                } else {
                                    Definisjon.SKRIV_BREV
                                }

                            BrevGrunnlag.Brev(
                                skrivBrevDefinisjon = definisjon,
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
                                signaturer = signaturer
                            )
                        }
                    }

                    val harTilgangTilÅSaksbehandle = TilgangGatewayImpl.sjekkTilgangTilBehandling(
                        behandlingReferanse.referanse,
                        Definisjon.SKRIV_BREV.kode.toString(),
                        token()
                    )

                    respond(
                        BrevGrunnlag(
                            harTilgangTilÅSaksbehandle,
                            grunnlag
                        )
                    )
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
                        val repositoryProvider = RepositoryRegistry.provider(connection)

                        val taSkriveLåsRepository =
                            repositoryProvider.provide<TaSkriveLåsRepository>()

                        val lås = taSkriveLåsRepository.lås(req.behandlingsReferanse.referanse)

                        MDC.putCloseable("sakId", lås.sakSkrivelås.id.toString()).use {
                            MDC.putCloseable("behandlingId", lås.behandlingSkrivelås.id.toString())
                                .use {
                                    val behandlingRepository =
                                        repositoryProvider.provide<BehandlingRepository>()
                                    val sakRepository =
                                        repositoryProvider.provide<SakRepository>()
                                    val brevbestillingRepository =
                                        repositoryProvider.provide<BrevbestillingRepository>()
                                    val avklaringsbehovRepository =
                                        repositoryProvider.provide<AvklaringsbehovRepository>()

                                    val behandling =
                                        behandlingRepository.hent(req.behandlingsReferanse)

                                    val service = BrevbestillingService(
                                        signaturService = SignaturService(avklaringsbehovRepository = avklaringsbehovRepository),
                                        brevbestillingGateway = brevbestillingGateway,
                                        brevbestillingRepository = brevbestillingRepository,
                                        behandlingRepository = behandlingRepository,
                                        sakRepository = sakRepository
                                    )

                                    val avklaringsbehovene =
                                        repositoryProvider.provide<AvklaringsbehovRepository>()
                                            .hentAvklaringsbehovene(behandling.id)

                                    avklaringsbehovene.validateTilstand(behandling = behandling)
                                    avklaringsbehovene.leggTil(
                                        definisjoner = listOf(Definisjon.BESTILL_BREV),
                                        funnetISteg = behandling.aktivtSteg(),
                                    )
                                    avklaringsbehovene.validerPlassering(behandling = behandling)

                                    val bestillingReferanse = service.bestill(
                                        behandlingId = behandling.id,
                                        typeBrev = TypeBrev.VARSEL_OM_BESTILLING,
                                        unikReferanse = req.dialogmeldingUuid.toString(),
                                        vedlegg = req.vedlegg
                                    )
                                    taSkriveLåsRepository.verifiserSkrivelås(lås)
                                    bestillingReferanse
                                }
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
                        val repositoryProvider = RepositoryRegistry.provider(connection)
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
                        val repositoryProvider = RepositoryRegistry.provider(connection)
                        val avklaringsbehovRepository =
                            repositoryProvider.provide<AvklaringsbehovRepository>()
                        val taSkriveLåsRepository =
                            repositoryProvider.provide<TaSkriveLåsRepository>()

                        val lås = taSkriveLåsRepository.lås(request.behandlingReferanse)

                        val behandlingRepository =
                            repositoryProvider.provide<BehandlingRepository>()
                        val sakRepository = repositoryProvider.provide<SakRepository>()
                        val flytJobbRepository = repositoryProvider.provide<FlytJobbRepository>()

                        MDC.putCloseable("sakId", lås.sakSkrivelås.id.toString()).use {
                            MDC.putCloseable("behandlingId", lås.behandlingSkrivelås.id.toString())
                                .use {
                                    val behandling =
                                        behandlingRepository.hent(lås.behandlingSkrivelås.id)

                                    AvklaringsbehovHendelseHåndterer(
                                        AvklaringsbehovOrkestrator(
                                            connection,
                                            BehandlingHendelseServiceImpl(
                                                flytJobbRepository,
                                                repositoryProvider.provide(),
                                                SakService(sakRepository)
                                            )
                                        ),
                                        avklaringsbehovRepository,
                                        behandlingRepository,
                                    ).håndtere(
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
                        FaktagrunnlagService.konstruer(connection)
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

// TODO duplisert og kan slettes når denne filen er flyttet til api-modulen
@BinaryResponse(contentTypes = ["application/pdf"])
data class DokumentResponsDTO(val stream: InputStream)
