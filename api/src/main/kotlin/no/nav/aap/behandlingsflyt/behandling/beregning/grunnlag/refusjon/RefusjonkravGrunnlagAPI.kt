package no.nav.aap.behandlingsflyt.behandling.beregning.grunnlag.refusjon

import com.papsign.ktor.openapigen.route.path.normal.NormalOpenAPIRoute
import com.papsign.ktor.openapigen.route.response.respond
import com.papsign.ktor.openapigen.route.route
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.refusjonkrav.RefusjonkravRepository
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.Definisjon
import no.nav.aap.behandlingsflyt.kontrakt.behandling.BehandlingReferanse
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingRepository
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.flate.BehandlingReferanseService
import no.nav.aap.behandlingsflyt.tilgang.TilgangGateway
import no.nav.aap.komponenter.dbconnect.transaction
import no.nav.aap.komponenter.gateway.GatewayProvider
import no.nav.aap.komponenter.httpklient.auth.token
import no.nav.aap.komponenter.repository.RepositoryRegistry
import no.nav.aap.tilgang.AuthorizationParamPathConfig
import no.nav.aap.tilgang.BehandlingPathParam
import no.nav.aap.tilgang.authorizedGet
import javax.sql.DataSource

fun NormalOpenAPIRoute.refusjonGrunnlagAPI(
    dataSource: DataSource,
    repositoryRegistry: RepositoryRegistry
) {
    route("/api/behandling") {
        route("/{referanse}/grunnlag/refusjon") {
            authorizedGet<BehandlingReferanse, RefusjonkravGrunnlagDto>(
                AuthorizationParamPathConfig(
                    behandlingPathParam = BehandlingPathParam("referanse")
                )
            ) { req ->
                val response = dataSource.transaction(readOnly = true) { connection ->
                    val repositoryProvider = repositoryRegistry.provider(connection)
                    val refusjonkravRepository = repositoryProvider.provide<RefusjonkravRepository>()

                    val behandlingRepository = repositoryProvider.provide<BehandlingRepository>()
                    val behandling = BehandlingReferanseService(behandlingRepository).behandling(req)

                    val gjeldendeVurdering = refusjonkravRepository.hentHvisEksisterer(behandling.id)
                    val historiskeVurderinger = refusjonkravRepository.hentAlleVurderingerPåSak(behandling.sakId)

                    val harTilgangTilÅSaksbehandle =
                        GatewayProvider.provide<TilgangGateway>().sjekkTilgangTilBehandling(
                            req.referanse,
                            Definisjon.REFUSJON_KRAV,
                            token()
                        )

                    RefusjonkravGrunnlagDto(
                        harTilgangTilÅSaksbehandle = harTilgangTilÅSaksbehandle,
                        gjeldendeVurdering = gjeldendeVurdering,
                        historiskeVurderinger = historiskeVurderinger
                    )
                }

                respond(response)
            }
        }
    }
}

