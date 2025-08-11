package no.nav.aap.behandlingsflyt.behandling.avklaringsbehov

import com.papsign.ktor.openapigen.route.path.normal.NormalOpenAPIRoute
import com.papsign.ktor.openapigen.route.response.respondWithStatus
import com.papsign.ktor.openapigen.route.route
import com.papsign.ktor.openapigen.route.tag
import io.ktor.http.*
import no.nav.aap.behandlingsflyt.Tags
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.flate.LøsAvklaringsbehovPåBehandling
import no.nav.aap.behandlingsflyt.kontrakt.behandling.BehandlingReferanse
import no.nav.aap.behandlingsflyt.mdc.LogKontekst
import no.nav.aap.behandlingsflyt.mdc.LoggingKontekst
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingRepository
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.flate.BehandlingReferanseService
import no.nav.aap.behandlingsflyt.sakogbehandling.lås.TaSkriveLåsRepository
import no.nav.aap.komponenter.dbconnect.transaction
import no.nav.aap.komponenter.gateway.GatewayProvider
import no.nav.aap.komponenter.repository.RepositoryRegistry
import no.nav.aap.komponenter.server.auth.bruker
import no.nav.aap.motor.FlytJobbRepository
import no.nav.aap.tilgang.AuthorizationBodyPathConfig
import no.nav.aap.tilgang.Operasjon
import no.nav.aap.tilgang.authorizedPost
import javax.sql.DataSource

fun NormalOpenAPIRoute.avklaringsbehovApi(dataSource: DataSource, repositoryRegistry: RepositoryRegistry) {
    route("/api/behandling").tag(Tags.Behandling) {
        route("/løs-behov") {
            authorizedPost<Unit, LøsAvklaringsbehovPåBehandling, LøsAvklaringsbehovPåBehandling>(
                AuthorizationBodyPathConfig(
                    operasjon = Operasjon.SAKSBEHANDLE,
                )
            ) { _, request ->
                dataSource.transaction { connection ->
                    val repositoryProvider = repositoryRegistry.provider(connection)
                    val behandlingRepository = repositoryProvider.provide<BehandlingRepository>()
                    val taSkriveLåsRepository = repositoryProvider.provide<TaSkriveLåsRepository>()
                    val flytJobbRepository = repositoryProvider.provide<FlytJobbRepository>()

                    LoggingKontekst(
                        repositoryProvider,
                        LogKontekst(referanse = BehandlingReferanse(request.referanse))
                    ).use {
                        val lås = taSkriveLåsRepository.lås(request.referanse)
                        BehandlingTilstandValidator(
                            BehandlingReferanseService(behandlingRepository),
                            flytJobbRepository
                        ).validerTilstand(
                            BehandlingReferanse(request.referanse), request.behandlingVersjon
                        )

                        AvklaringsbehovHendelseHåndterer(repositoryProvider, GatewayProvider).håndtere(
                            key = lås.behandlingSkrivelås.id, hendelse = LøsAvklaringsbehovHendelse(
                                request.behov,
                                request.ingenEndringIGruppe == true,
                                request.behandlingVersjon,
                                bruker()
                            )
                        )
                        taSkriveLåsRepository.verifiserSkrivelås(lås)
                    }
                }
                respondWithStatus(HttpStatusCode.Accepted)
            }
        }
    }
}