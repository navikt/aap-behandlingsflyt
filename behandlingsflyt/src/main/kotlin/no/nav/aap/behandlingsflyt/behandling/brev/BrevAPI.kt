package no.nav.aap.behandlingsflyt.behandling.brev

import com.papsign.ktor.openapigen.route.path.normal.NormalOpenAPIRoute
import com.papsign.ktor.openapigen.route.path.normal.get
import com.papsign.ktor.openapigen.route.path.normal.put
import com.papsign.ktor.openapigen.route.response.respond
import com.papsign.ktor.openapigen.route.route
import io.ktor.http.*
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.*
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løser.BREV_SYSTEMBRUKER
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løsning.BrevbestillingLøsning
import no.nav.aap.behandlingsflyt.behandling.brev.bestilling.*
import no.nav.aap.behandlingsflyt.hendelse.avløp.BehandlingHendelseServiceImpl
import no.nav.aap.behandlingsflyt.kontrakt.behandling.BehandlingReferanse
import no.nav.aap.behandlingsflyt.kontrakt.brevbestilling.LøsBrevbestillingDto
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingRepository
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.flate.ElementNotFoundException
import no.nav.aap.behandlingsflyt.sakogbehandling.lås.TaSkriveLåsRepository
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.SakRepository
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.SakService
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.adapters.PdlPersoninfoGateway
import no.nav.aap.brev.kontrakt.Brev
import no.nav.aap.komponenter.config.requiredConfigForKey
import no.nav.aap.komponenter.dbconnect.transaction
import no.nav.aap.komponenter.httpklient.auth.token
import no.nav.aap.motor.FlytJobbRepository
import no.nav.aap.repository.RepositoryFactory
import no.nav.aap.tilgang.AuthorizationBodyPathConfig
import no.nav.aap.tilgang.authorizedPost
import org.slf4j.MDC
import tilgang.Operasjon
import javax.sql.DataSource

fun NormalOpenAPIRoute.brevApi(dataSource: DataSource) {

    val brevAzp = requiredConfigForKey("integrasjon.brev.azp")
    route("/api") {
        route("/behandling") {
            route("/{referanse}/grunnlag/brev") {
                get<BehandlingReferanse, BrevGrunnlag> { behandlingReferanse ->
                    val grunnlag = dataSource.transaction { connection ->

                        val repositoryFactory = RepositoryFactory(connection)
                        val behandlingRepository =
                            repositoryFactory.create(BehandlingRepository::class)
                        val sakRepository = repositoryFactory.create(SakRepository::class)
                        val brevbestillingRepository =
                            repositoryFactory.create(BrevbestillingRepository::class)

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
                            PdlPersoninfoGateway.hentPersoninfoForIdent(personIdent, token())
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
                        val repositoryFactory = RepositoryFactory(connection)
                        val avklaringsbehovRepository = repositoryFactory.create(
                            AvklaringsbehovRepository::class)
                        val taSkriveLåsRepository =
                            repositoryFactory.create(TaSkriveLåsRepository::class)

                        val lås = taSkriveLåsRepository.lås(request.behandlingReferanse)

                        val behandlingRepository =
                            repositoryFactory.create(BehandlingRepository::class)
                        val sakRepository = repositoryFactory.create(SakRepository::class)

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
