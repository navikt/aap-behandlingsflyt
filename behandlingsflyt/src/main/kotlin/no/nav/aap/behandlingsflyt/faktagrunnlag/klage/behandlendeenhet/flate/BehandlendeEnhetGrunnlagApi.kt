package no.nav.aap.behandlingsflyt.faktagrunnlag.klage.behandlendeenhet.flate

import com.papsign.ktor.openapigen.route.path.normal.NormalOpenAPIRoute
import com.papsign.ktor.openapigen.route.response.respond
import com.papsign.ktor.openapigen.route.route
import no.nav.aap.behandlingsflyt.faktagrunnlag.klage.behandlendeenhet.BehandlendeEnhetRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.klage.formkrav.FormkravRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.klage.formkrav.flate.FormkravGrunnlagDto
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

fun NormalOpenAPIRoute.behandlendeEnhetGrunnlagApi(dataSource: DataSource, repositoryRegistry: RepositoryRegistry) {
    route("api/klage/{referanse}/grunnlag/behandlende-enhet") {
        authorizedGet<BehandlingReferanse, BehandlendeEnhetGrunnlagDto>(
            AuthorizationParamPathConfig(
                behandlingPathParam = BehandlingPathParam(
                    "referanse"
                )
            )
        ) { req ->
            val respons = dataSource.transaction(readOnly = true) { connection ->
                val repositoryProvider = repositoryRegistry.provider(connection)
                val behandlingRepository = repositoryProvider.provide<BehandlingRepository>()
                val behandlendeEnhetRepository = repositoryProvider.provide<BehandlendeEnhetRepository>()

                val behandling: Behandling =
                    BehandlingReferanseService(behandlingRepository).behandling(req)

                val grunnlag = behandlendeEnhetRepository.hentHvisEksisterer(behandling.id)

                BehandlendeEnhetGrunnlagDto(
                    grunnlag?.vurdering?.tilDto()
                )
            }
            respond(respons)
        }
    }
}