package no.nav.aap.behandlingsflyt.behandling.brev

import com.papsign.ktor.openapigen.route.path.normal.NormalOpenAPIRoute
import com.papsign.ktor.openapigen.route.path.normal.get
import com.papsign.ktor.openapigen.route.path.normal.put
import com.papsign.ktor.openapigen.route.response.respond
import com.papsign.ktor.openapigen.route.response.respondWithStatus
import com.papsign.ktor.openapigen.route.route
import io.ktor.http.*
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.AvklaringsbehovHendelseHåndterer
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.LøsAvklaringsbehovHendelse
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løsning.BrevbestillingLøsning
import no.nav.aap.behandlingsflyt.behandling.brev.bestilling.BrevGateway
import no.nav.aap.behandlingsflyt.behandling.brev.bestilling.BrevbestillingReferanse
import no.nav.aap.behandlingsflyt.behandling.brev.bestilling.BrevbestillingService
import no.nav.aap.behandlingsflyt.behandling.brev.bestilling.Status
import no.nav.aap.behandlingsflyt.kontrakt.behandling.BehandlingReferanse
import no.nav.aap.behandlingsflyt.kontrakt.brevbestilling.LøsBrevbestillingDto
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingRepositoryImpl
import no.nav.aap.behandlingsflyt.sakogbehandling.lås.TaSkriveLåsRepository
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.SakService
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.adapters.PdlPersoninfoGateway
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.db.SakRepositoryImpl
import no.nav.aap.brev.kontrakt.Brev
import no.nav.aap.komponenter.config.requiredConfigForKey
import no.nav.aap.komponenter.dbconnect.transaction
import no.nav.aap.komponenter.httpklient.auth.Bruker
import no.nav.aap.komponenter.httpklient.auth.token
import no.nav.aap.tilgang.AuthorizationBodyPathConfig
import no.nav.aap.tilgang.authorizedPost
import no.nav.aap.verdityper.feilhåndtering.ElementNotFoundException
import org.slf4j.MDC
import tilgang.Operasjon
import javax.sql.DataSource

val BREV_SYSTEMBRUKER = Bruker("Brevløsning")

fun NormalOpenAPIRoute.brevApi(dataSource: DataSource) {

    val brevAzp = requiredConfigForKey("integrasjon.brev.azp")
    route("/api") {
        route("/behandling") {
            route("/{referanse}/grunnlag/brev") {
                get<BehandlingReferanse, BrevGrunnlag> { behandlingReferanse ->
                    val grunnlag = dataSource.transaction { connection ->
                        val brevbestilling =
                            BrevbestillingService.konstruer(connection).hentSisteBrevbestilling(behandlingReferanse)
                                ?: throw ElementNotFoundException()
                        val behandling = BehandlingRepositoryImpl(connection).hent(behandlingReferanse)
                        val sak = SakService(SakRepositoryImpl(connection)).hent(behandling.sakId)
                        val personIdent = sak.person.aktivIdent()
                        val personinfo = PdlPersoninfoGateway.hentPersoninfoForIdent(personIdent, token())
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
                            mottaker = Mottaker(navn = personinfo.fulltNavn(), ident = personinfo.ident.identifikator)
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
                        val taSkriveLåsRepository = TaSkriveLåsRepository(connection)

                        val lås = taSkriveLåsRepository.lås(request.behandlingReferanse)

                        MDC.putCloseable("sakId", lås.sakSkrivelås.id.toString()).use {
                            MDC.putCloseable("behandlingId", lås.behandlingSkrivelås.id.toString()).use {
                                val behandling = BehandlingRepositoryImpl(connection).hent(lås.behandlingSkrivelås.id)

                                AvklaringsbehovHendelseHåndterer(connection).håndtere(
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
