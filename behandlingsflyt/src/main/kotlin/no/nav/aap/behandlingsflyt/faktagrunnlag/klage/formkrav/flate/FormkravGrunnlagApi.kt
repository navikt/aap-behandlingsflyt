package no.nav.aap.behandlingsflyt.faktagrunnlag.klage.formkrav.flate

import com.papsign.ktor.openapigen.route.path.normal.NormalOpenAPIRoute
import com.papsign.ktor.openapigen.route.response.respond
import com.papsign.ktor.openapigen.route.route
import no.nav.aap.behandlingsflyt.faktagrunnlag.klage.formkrav.FormkravRepository
import no.nav.aap.behandlingsflyt.kontrakt.behandling.BehandlingReferanse
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.Behandling
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingRepository
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.flate.BehandlingReferanseService
import no.nav.aap.komponenter.dbconnect.transaction
import no.nav.aap.komponenter.repository.RepositoryRegistry
import no.nav.aap.tilgang.AuthorizationParamPathConfig
import no.nav.aap.tilgang.BehandlingPathParam
import no.nav.aap.tilgang.authorizedGet
import javax.sql.DataSource

fun NormalOpenAPIRoute.formkravGrunnlagApi(dataSource: DataSource, repositoryRegistry: RepositoryRegistry) {
    route("api/klage/{referanse}/grunnlag/formkrav") {
        authorizedGet<BehandlingReferanse, FormkravGrunnlagDto>(
            AuthorizationParamPathConfig(
                behandlingPathParam = BehandlingPathParam(
                    "referanse"
                )
            )
        ) {
            req -> 
                val respons = dataSource.transaction(readOnly = true) { connection ->
                    val repositoryProvider = repositoryRegistry.provider(connection)
                    val behandlingRepository = repositoryProvider.provide<BehandlingRepository>()
                    val formkravRepository = repositoryProvider.provide<FormkravRepository>()

                    val behandling: Behandling =
                        BehandlingReferanseService(behandlingRepository).behandling(req)

                    formkravRepository.hentHvisEksisterer(behandling.id)?.tilDto() ?: FormkravGrunnlagDto()
                }
            respond(respons)
        }
    }
}