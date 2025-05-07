package no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.medlemskap.flate

import com.papsign.ktor.openapigen.route.path.normal.NormalOpenAPIRoute
import com.papsign.ktor.openapigen.route.response.respond
import com.papsign.ktor.openapigen.route.route
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.medlemskap.MedlemskapRepositoryImpl
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.medlemskap.MedlemskapUnntakGrunnlag
import no.nav.aap.behandlingsflyt.kontrakt.behandling.BehandlingReferanse
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingRepository
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.flate.BehandlingReferanseService
import no.nav.aap.komponenter.dbconnect.transaction
import no.nav.aap.lookup.repository.RepositoryRegistry
import no.nav.aap.tilgang.AuthorizationParamPathConfig
import no.nav.aap.tilgang.BehandlingPathParam
import no.nav.aap.tilgang.authorizedGet
import javax.sql.DataSource

fun NormalOpenAPIRoute.medlemskapsgrunnlagApi(dataSource: DataSource) {
    route("/api/behandling") {
        route("/{referanse}/grunnlag/medlemskap") {
            authorizedGet<BehandlingReferanse, MedlemskapGrunnlagDto>(
                AuthorizationParamPathConfig(
                    behandlingPathParam = BehandlingPathParam(
                        "referanse"
                    )
                )
            ) { req ->
                val medlemskap = dataSource.transaction(readOnly = true) { connection ->
                    val repositoryProvider = RepositoryRegistry.provider(connection)
                    val behandlingRepository = repositoryProvider.provide<BehandlingRepository>()
                    val behandling =
                        BehandlingReferanseService(behandlingRepository).behandling(req)
                    MedlemskapRepositoryImpl(connection).hentHvisEksisterer(behandling.id)
                        ?: MedlemskapUnntakGrunnlag(unntak = listOf())
                }
                respond(MedlemskapGrunnlagDto(medlemskap = medlemskap))
            }
        }
    }
}
