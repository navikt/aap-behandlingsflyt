package no.nav.aap.behandlingsflyt.behandling.klage.effektueravvistpåformkrav

import com.papsign.ktor.openapigen.route.path.normal.NormalOpenAPIRoute
import com.papsign.ktor.openapigen.route.response.respond
import com.papsign.ktor.openapigen.route.route
import no.nav.aap.behandlingsflyt.behandling.brev.bestilling.BrevbestillingGateway
import no.nav.aap.behandlingsflyt.faktagrunnlag.klage.effektueravvistpåformkrav.EffektuerAvvistPåFormkravRepository
import no.nav.aap.behandlingsflyt.kontrakt.behandling.BehandlingReferanse
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.Behandling
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingRepository
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.flate.BehandlingReferanseService
import no.nav.aap.brev.kontrakt.Status
import no.nav.aap.komponenter.dbconnect.transaction
import no.nav.aap.komponenter.gateway.GatewayProvider
import no.nav.aap.komponenter.repository.RepositoryRegistry
import no.nav.aap.tilgang.AuthorizationParamPathConfig
import no.nav.aap.tilgang.BehandlingPathParam
import no.nav.aap.tilgang.authorizedGet
import javax.sql.DataSource

fun NormalOpenAPIRoute.effektuerAvvistPåFormkravGrunnlagApi(
    dataSource: DataSource,
    repositoryRegistry: RepositoryRegistry
) {
    route("api/klage/{referanse}/grunnlag/effektuer-avvist-på-formkrav") {
        authorizedGet<BehandlingReferanse, EffektuerAvvistPåFormkravGrunnlagDto>(
            AuthorizationParamPathConfig(
                behandlingPathParam = BehandlingPathParam(
                    "referanse"
                )
            )
        ) { req ->
            val respons = dataSource.transaction(readOnly = true) { connection ->
                val repositoryProvider = repositoryRegistry.provider(connection)
                val behandlingRepository = repositoryProvider.provide<BehandlingRepository>()
                val effektuerAvvistPåFormkravRepository =
                    repositoryProvider.provide<EffektuerAvvistPåFormkravRepository>()

                val behandling: Behandling =
                    BehandlingReferanseService(behandlingRepository).behandling(req)

                val grunnlag = effektuerAvvistPåFormkravRepository.hentHvisEksisterer(behandling.id)

                if (grunnlag == null) {
                    EffektuerAvvistPåFormkravGrunnlagDto()
                } else {
                    val brevGateway = GatewayProvider.provide<BrevbestillingGateway>()

                    val brevFerdigstilt =
                        brevGateway.hent(grunnlag.varsel.referanse)
                            .takeIf { it.status == Status.FERDIGSTILT }
                            ?.oppdatert?.toLocalDate()


                    grunnlag.tilDto(brevFerdigstilt)

                }
            }
            respond(respons)
        }
    }
}