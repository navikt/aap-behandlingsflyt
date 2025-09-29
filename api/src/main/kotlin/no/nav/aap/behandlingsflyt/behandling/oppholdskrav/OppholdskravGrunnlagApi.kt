package no.nav.aap.behandlingsflyt.behandling.oppholdskrav

import com.papsign.ktor.openapigen.route.path.normal.NormalOpenAPIRoute
import com.papsign.ktor.openapigen.route.response.respond
import com.papsign.ktor.openapigen.route.response.respondWithStatus
import com.papsign.ktor.openapigen.route.route
import io.ktor.http.HttpStatusCode
import no.nav.aap.behandlingsflyt.behandling.ansattinfo.AnsattInfoService
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.Definisjon
import no.nav.aap.behandlingsflyt.kontrakt.behandling.BehandlingReferanse
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.Behandling
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingRepository
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.flate.BehandlingReferanseService
import no.nav.aap.behandlingsflyt.tilgang.kanSaksbehandle
import no.nav.aap.komponenter.dbconnect.transaction
import no.nav.aap.komponenter.gateway.GatewayProvider
import no.nav.aap.komponenter.repository.RepositoryRegistry
import no.nav.aap.tilgang.BehandlingPathParam
import no.nav.aap.tilgang.getGrunnlag
import javax.sql.DataSource

fun NormalOpenAPIRoute.oppholdskravGrunnlagApi(
    dataSource: DataSource,
    repositoryRegistry: RepositoryRegistry,
    gatewayProvider: GatewayProvider,
) {
    val ansattInfoService = AnsattInfoService(gatewayProvider)

    route("/api/behandling/{referanse}/grunnlag/oppholdskrav") {
        getGrunnlag<BehandlingReferanse, OppholdskravGrunnlagResponse>(
            behandlingPathParam = BehandlingPathParam("referanse"),
            avklaringsbehovKode = Definisjon.AVKLAR_OPPHOLDSKRAV.kode.toString(),
        ) { req ->
            val oppholdskravGrunnlag =
                dataSource.transaction(readOnly = true) { connection ->
                    val repositoryProvider = repositoryRegistry.provider(connection)
                    val behandlingRepository = repositoryProvider.provide<BehandlingRepository>()
                    val oppholdskravRepository = repositoryProvider.provide<OppholdskravGrunnlagRepository>()

                    val behandling: Behandling =
                        BehandlingReferanseService(behandlingRepository).behandling(req)

                    val grunnlag = oppholdskravRepository.hentHvisEksisterer(behandling.id) ?: return@transaction null

                    val vurdering = grunnlag.vurderinger.firstOrNull { it.vurdertIBehandling == behandling.id }
                    val gjeldendeVedtatteVurderinger = grunnlag.vurderinger.filter { it.vurdertIBehandling != behandling.id }

                    OppholdskravGrunnlagResponse(
                        harTilgangTilÅSaksbehandle = kanSaksbehandle(),
                        oppholdskravVurdering = vurdering?.tilDto(ansattInfoService),
                        gjeldendeVedtatteVurderinger = gjeldendeVedtatteVurderinger.map { it.tilDto(ansattInfoService) }
                    )
                }

            if(oppholdskravGrunnlag == null) {
                respondWithStatus(HttpStatusCode.NoContent)
            } else {
                respond(oppholdskravGrunnlag)
            }
        }
    }
}
