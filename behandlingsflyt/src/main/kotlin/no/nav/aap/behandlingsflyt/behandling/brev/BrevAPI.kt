package no.nav.aap.behandlingsflyt.behandling.brev

import com.papsign.ktor.openapigen.route.path.normal.NormalOpenAPIRoute
import com.papsign.ktor.openapigen.route.path.normal.get
import com.papsign.ktor.openapigen.route.path.normal.post
import com.papsign.ktor.openapigen.route.path.normal.put
import com.papsign.ktor.openapigen.route.response.respond
import com.papsign.ktor.openapigen.route.route
import io.ktor.http.*
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.AvklaringsbehovHendelseHåndterer
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.AvklaringsbehovOrkestrator
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.AvklaringsbehovRepository
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.LøsAvklaringsbehovHendelse
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løser.BREV_SYSTEMBRUKER
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løsning.BrevbestillingLøsning
import no.nav.aap.behandlingsflyt.behandling.brev.bestilling.BrevGateway
import no.nav.aap.behandlingsflyt.behandling.brev.bestilling.BrevbestillingReferanse
import no.nav.aap.behandlingsflyt.behandling.brev.bestilling.BrevbestillingRepository
import no.nav.aap.behandlingsflyt.behandling.brev.bestilling.BrevbestillingService
import no.nav.aap.behandlingsflyt.behandling.brev.bestilling.Status
import no.nav.aap.behandlingsflyt.behandling.brev.bestilling.TypeBrev
import no.nav.aap.behandlingsflyt.hendelse.avløp.BehandlingHendelseServiceImpl
import no.nav.aap.behandlingsflyt.kontrakt.behandling.BehandlingReferanse
import no.nav.aap.behandlingsflyt.kontrakt.brevbestilling.LøsBrevbestillingDto
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingRepository
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.flate.ElementNotFoundException
import no.nav.aap.behandlingsflyt.sakogbehandling.lås.TaSkriveLåsRepository
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.PersoninfoGateway
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.SakRepository
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.SakService
import no.nav.aap.brev.kontrakt.Brev
import no.nav.aap.brev.kontrakt.Brevtype
import no.nav.aap.komponenter.config.requiredConfigForKey
import no.nav.aap.komponenter.dbconnect.transaction
import no.nav.aap.komponenter.httpklient.auth.token
import no.nav.aap.lookup.gateway.GatewayProvider
import no.nav.aap.lookup.repository.RepositoryProvider
import no.nav.aap.motor.FlytJobbRepository
import no.nav.aap.tilgang.AuthorizationBodyPathConfig
import no.nav.aap.tilgang.authorizedPost
import org.slf4j.MDC
import tilgang.Operasjon
import java.util.*
import javax.sql.DataSource

fun NormalOpenAPIRoute.brevApi(dataSource: DataSource) {

    val brevAzp = requiredConfigForKey("integrasjon.brev.azp")
    route("/api") {
        route("/behandling") {
            route("/{referanse}/grunnlag/brev") {
                get<BehandlingReferanse, BrevGrunnlag> { behandlingReferanse ->
                    val grunnlag = dataSource.transaction { connection ->
                        val repositoryProvider = RepositoryProvider(connection)
                        val behandlingRepository =
                            repositoryProvider.provide(BehandlingRepository::class)
                        val sakRepository = repositoryProvider.provide(SakRepository::class)
                        val brevbestillingRepository =
                            repositoryProvider.provide(BrevbestillingRepository::class)

                        val brevbestilling =
                            BrevbestillingService(
                                brevbestillingGateway = BrevGateway(),
                                brevbestillingRepository = brevbestillingRepository,
                                behandlingRepository = behandlingRepository,
                                sakRepository = sakRepository
                            ).hentSisteBrevbestilling(behandlingReferanse)
                                ?: throw ElementNotFoundException()
                        val behandling = behandlingRepository.hent(behandlingReferanse)

                        val sak = SakService(sakRepository).hent(behandling.sakId)
                        val personIdent = sak.person.aktivIdent()
                        val personinfo =
                            GatewayProvider.provide(PersoninfoGateway::class)
                                .hentPersoninfoForIdent(personIdent, token())
                        BrevGrunnlag(
                            brevbestillingReferanse = brevbestilling.referanse,
                            brev = brevbestilling.brev,
                            opprettet = brevbestilling.opprettet,
                            oppdatert = brevbestilling.oppdatert,
                            brevtype = brevbestilling.brevtype,
                            språk = brevbestilling.språk,
                            status = when (brevbestilling.status) {
                                no.nav.aap.brev.kontrakt.Status.REGISTRERT -> Status.SENDT
                                no.nav.aap.brev.kontrakt.Status.UNDER_ARBEID -> Status.FORHÅNDSVISNING_KLAR
                                no.nav.aap.brev.kontrakt.Status.FERDIGSTILT -> Status.FULLFØRT
                            },
                            mottaker = Mottaker(
                                navn = personinfo.fulltNavn(),
                                ident = personinfo.ident.identifikator
                            )
                        )
                    }

                    respond(grunnlag)
                }
            }
        }
        route("/brev") {
            route("/bestillingvarsel") {
                    post<Unit, UUID, VarselOmBrevbestillingDto> { _, req ->
                        val bestillingVarselReferanse = dataSource.transaction { connection ->
                            val repositoryProvider = RepositoryProvider(connection)
                            val behandlingRepository = repositoryProvider.provide(BehandlingRepository::class)
                            val sakRepository = repositoryProvider.provide(SakRepository::class)
                            val brevbestillingRepository = repositoryProvider.provide(BrevbestillingRepository::class)

                            val behandlingId = behandlingRepository.hent(req.behandlingsReferanse).id

                            val service = BrevbestillingService(BrevGateway(), brevbestillingRepository, behandlingRepository, sakRepository)
                            val bestillingReferanse = service.bestill(
                                behandlingId,
                                TypeBrev.VARSEL_OM_BESTILLING,
                                req.vedlegg
                            )
                            bestillingReferanse
                        }

                        respond(bestillingVarselReferanse, HttpStatusCode.Accepted)
                    }
                }
            }
            route("/{brevbestillingReferanse}/oppdater") {
                put<BrevbestillingReferanse, String, Brev> { brevbestillingReferanse, brev ->
                    BrevGateway().oppdater(brevbestillingReferanse, brev)
                    respond("{}", HttpStatusCode.Accepted)
                }
            }
            route("/los-bestilling") {
                authorizedPost<Unit, String, LøsBrevbestillingDto>(
                    AuthorizationBodyPathConfig(
                        operasjon = Operasjon.SAKSBEHANDLE,
                        approvedApplications = setOf(brevAzp),
                        applicationsOnly = true
                    )
                ) { _, request ->
                    dataSource.transaction { connection ->
                        val repositoryProvider = RepositoryProvider(connection)
                        val avklaringsbehovRepository = repositoryProvider.provide(
                            AvklaringsbehovRepository::class
                        )
                        val taSkriveLåsRepository =
                            repositoryProvider.provide(TaSkriveLåsRepository::class)

                        val lås = taSkriveLåsRepository.lås(request.behandlingReferanse)

                        val behandlingRepository =
                            repositoryProvider.provide(BehandlingRepository::class)
                        val sakRepository = repositoryProvider.provide(SakRepository::class)

                        MDC.putCloseable("sakId", lås.sakSkrivelås.id.toString()).use {
                            MDC.putCloseable("behandlingId", lås.behandlingSkrivelås.id.toString())
                                .use {
                                    val behandling =
                                        behandlingRepository.hent(lås.behandlingSkrivelås.id)

                                    AvklaringsbehovHendelseHåndterer(
                                        AvklaringsbehovOrkestrator(
                                            connection,
                                            BehandlingHendelseServiceImpl(
                                                FlytJobbRepository(connection),
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
        }
    }
}
