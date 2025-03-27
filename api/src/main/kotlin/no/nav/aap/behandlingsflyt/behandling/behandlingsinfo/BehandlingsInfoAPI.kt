package no.nav.aap.behandlingsflyt.behandling.behandlingsinfo

import com.papsign.ktor.openapigen.route.path.normal.NormalOpenAPIRoute
import com.papsign.ktor.openapigen.route.response.respond
import com.papsign.ktor.openapigen.route.route
import no.nav.aap.behandlingsflyt.behandling.tilkjentytelse.VirkningstidspunktUtleder
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.VilkårsresultatRepository
import no.nav.aap.behandlingsflyt.kontrakt.behandling.BehandlingReferanse
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.Behandling
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingRepository
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.flate.BehandlingReferanseService
import no.nav.aap.komponenter.dbconnect.transaction
import no.nav.aap.lookup.repository.RepositoryProvider
import no.nav.aap.tilgang.AuthorizationParamPathConfig
import no.nav.aap.tilgang.BehandlingPathParam
import no.nav.aap.tilgang.authorizedGet
import javax.sql.DataSource

fun NormalOpenAPIRoute.behandlingsinfoApi(dataSource: DataSource) {
    route("/api/behandling") {
        route("/{referanse}/virkningstidspunkt") {
            authorizedGet<BehandlingReferanse, BehandlingsInfoDto>(
                AuthorizationParamPathConfig(behandlingPathParam = BehandlingPathParam("referanse")),
                auditLogConfig = null,
            ) { req ->

                val dto = dataSource.transaction(readOnly = true) { connection ->
                    val repositoryProvider = RepositoryProvider(connection)
                    val behandlingRepository = repositoryProvider.provide<BehandlingRepository>()
                    val vilkårsResultatRepository = repositoryProvider.provide<VilkårsresultatRepository>()
                    val behandling: Behandling =
                        BehandlingReferanseService(behandlingRepository).behandling(req)

                    val virkningstidspunkt = VirkningstidspunktUtleder(vilkårsresultatRepository = vilkårsResultatRepository).utledVirkningsTidspunkt(behandling.id)
                    BehandlingsInfoDto(
                        virkningstidspunkt = virkningstidspunkt,
                    )
                }
                respond(dto)
            }
        }
    }
}

