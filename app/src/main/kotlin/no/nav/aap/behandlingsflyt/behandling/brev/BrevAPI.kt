package no.nav.aap.behandlingsflyt.behandling.brev

import com.papsign.ktor.openapigen.route.path.normal.NormalOpenAPIRoute
import com.papsign.ktor.openapigen.route.response.respond
import com.papsign.ktor.openapigen.route.response.respondWithStatus
import com.papsign.ktor.openapigen.route.route
import io.ktor.http.HttpStatusCode
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.AvklaringsbehovHendelseHåndterer
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.LøsAvklaringsbehovBehandlingHendelse
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løsning.BrevbestillingLøsning
import no.nav.aap.behandlingsflyt.behandling.brev.bestilling.BrevbestillingRepository
import no.nav.aap.behandlingsflyt.kontrakt.brevbestilling.LøsBrevbestillingDto
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingRepositoryImpl
import no.nav.aap.behandlingsflyt.sakogbehandling.lås.TaSkriveLåsRepository
import no.nav.aap.komponenter.config.requiredConfigForKey
import no.nav.aap.komponenter.dbconnect.transaction
import no.nav.aap.komponenter.httpklient.auth.Bruker
import no.nav.aap.tilgang.authorizedPostWithApprovedList
import org.slf4j.MDC
import javax.sql.DataSource

val BREV_SYSTEMBRUKER = Bruker("Brevløsning")

fun NormalOpenAPIRoute.brevApi(dataSource: DataSource) {

    val brevAzp = requiredConfigForKey("integrasjon.brev.azp")

    route("/api/brev") {
        route("/løs-bestilling") {
            authorizedPostWithApprovedList<Unit, String, LøsBrevbestillingDto>(brevAzp) { _, request ->
                dataSource.transaction { connection ->
                    val taSkriveLåsRepository = TaSkriveLåsRepository(connection)
                    val lås = taSkriveLåsRepository.lås(request.referanse)
                    MDC.putCloseable("sakId", lås.sakSkrivelås.id.toString()).use {
                        MDC.putCloseable("behandlingId", lås.behandlingSkrivelås.id.toString()).use {
                            val brevbestilling = BrevbestillingRepository(connection).hent(request.referanse)
                            val behandling = BehandlingRepositoryImpl(connection).hent(brevbestilling.behandlingId)

                            AvklaringsbehovHendelseHåndterer(connection).håndtere(
                                key = brevbestilling.behandlingId,
                                hendelse = LøsAvklaringsbehovBehandlingHendelse(
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
