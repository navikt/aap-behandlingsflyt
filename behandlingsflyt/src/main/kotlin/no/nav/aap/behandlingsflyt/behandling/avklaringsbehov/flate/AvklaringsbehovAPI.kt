package no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.flate

import com.papsign.ktor.openapigen.route.path.normal.NormalOpenAPIRoute
import com.papsign.ktor.openapigen.route.path.normal.post
import com.papsign.ktor.openapigen.route.response.respondWithStatus
import com.papsign.ktor.openapigen.route.route
import io.ktor.http.*
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.AvklaringsbehovHendelseHåndterer
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.AvklaringsbehovOrkestrator
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.AvklaringsbehovRepositoryImpl
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.BehandlingTilstandValidator
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.LøsAvklaringsbehovHendelse
import no.nav.aap.behandlingsflyt.hendelse.avløp.BehandlingHendelseServiceImpl
import no.nav.aap.behandlingsflyt.kontrakt.behandling.BehandlingReferanse
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingRepository
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.flate.BehandlingReferanseService
import no.nav.aap.behandlingsflyt.sakogbehandling.lås.TaSkriveLåsRepository
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.SakRepository
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.SakService
import no.nav.aap.komponenter.dbconnect.transaction
import no.nav.aap.komponenter.httpklient.auth.bruker
import no.nav.aap.motor.FlytJobbRepository
import no.nav.aap.repository.RepositoryFactory
import org.slf4j.MDC
import javax.sql.DataSource

fun NormalOpenAPIRoute.avklaringsbehovApi(dataSource: DataSource) {
    route("/api/behandling") {
        route("/løs-behov") {
            post<Unit, LøsAvklaringsbehovPåBehandling, LøsAvklaringsbehovPåBehandling> { _, request ->
                dataSource.transaction { connection ->
                    val repositoryFactory = RepositoryFactory(connection)
                    val sakRepository = repositoryFactory.create(SakRepository::class)
                    val behandlingRepository = repositoryFactory.create(BehandlingRepository::class)
                    val taSkriveLåsRepository = repositoryFactory.create(TaSkriveLåsRepository::class)

                    val lås = taSkriveLåsRepository.lås(request.referanse)
                    MDC.putCloseable("sakId", lås.sakSkrivelås.id.toString()).use {
                        MDC.putCloseable("behandlingId", lås.behandlingSkrivelås.id.toString()).use {
                            val flytJobbRepository = FlytJobbRepository(connection)
                            BehandlingTilstandValidator(
                                BehandlingReferanseService(behandlingRepository),
                                flytJobbRepository
                            ).validerTilstand(
                                BehandlingReferanse(request.referanse), request.behandlingVersjon
                            )

                            AvklaringsbehovHendelseHåndterer(
                                AvklaringsbehovOrkestrator(
                                    connection, BehandlingHendelseServiceImpl(
                                        flytJobbRepository, SakService(sakRepository)
                                    )
                                ), AvklaringsbehovRepositoryImpl(connection), behandlingRepository
                            ).håndtere(
                                key = lås.behandlingSkrivelås.id, hendelse = LøsAvklaringsbehovHendelse(
                                    request.behov,
                                    request.ingenEndringIGruppe ?: false,
                                    request.behandlingVersjon,
                                    bruker()
                                )
                            )
                            taSkriveLåsRepository.verifiserSkrivelås(lås)
                        }
                    }
                }
                respondWithStatus(HttpStatusCode.Accepted)
            }
        }
    }
}