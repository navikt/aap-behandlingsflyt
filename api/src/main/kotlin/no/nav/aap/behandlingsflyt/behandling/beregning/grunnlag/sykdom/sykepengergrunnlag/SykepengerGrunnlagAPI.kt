package no.nav.aap.behandlingsflyt.behandling.beregning.grunnlag.sykdom.sykepengergrunnlag

import com.papsign.ktor.openapigen.route.path.normal.NormalOpenAPIRoute
import com.papsign.ktor.openapigen.route.response.respond
import com.papsign.ktor.openapigen.route.route
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.sykdom.SykepengerErstatningRepository
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.Definisjon
import no.nav.aap.behandlingsflyt.kontrakt.behandling.BehandlingReferanse
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.Behandling
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingRepository
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.flate.BehandlingReferanseService
import no.nav.aap.behandlingsflyt.tilgang.TilgangGatewayImpl
import no.nav.aap.komponenter.dbconnect.transaction
import no.nav.aap.komponenter.httpklient.auth.token
import no.nav.aap.komponenter.repository.RepositoryRegistry
import no.nav.aap.tilgang.AuthorizationParamPathConfig
import no.nav.aap.tilgang.BehandlingPathParam
import no.nav.aap.tilgang.authorizedGet
import javax.sql.DataSource


fun NormalOpenAPIRoute.sykepengerGrunnlagApi(dataSource: DataSource, repositoryRegistry: RepositoryRegistry) {
    route("/api/behandling") {
        route("/{referanse}/grunnlag/sykdom/sykepengergrunnlag") {
            authorizedGet<BehandlingReferanse, SykepengerGrunnlagDto>(
                AuthorizationParamPathConfig(behandlingPathParam = BehandlingPathParam("referanse"))
            ) { req ->
                val sykepengerErstatningGrunnlag = dataSource.transaction(readOnly = true) { connection ->
                    val repositoryProvider = repositoryRegistry.provider(connection)
                    val behandlingRepository = repositoryProvider.provide<BehandlingRepository>()
                    val sykeRepository = repositoryProvider.provide<SykepengerErstatningRepository>()
                    val behandling: Behandling =
                        BehandlingReferanseService(behandlingRepository).behandling(req)
                    sykeRepository.hentHvisEksisterer(behandling.id)
                }

                val harTilgangTilÅSaksbehandle = TilgangGatewayImpl.sjekkTilgangTilBehandling(
                    req.referanse,
                    Definisjon.AVKLAR_SYKEPENGEERSTATNING.kode.toString(),
                    token()
                )


                respond(
                    SykepengerGrunnlagDto(
                        harTilgangTilÅSaksbehandle = harTilgangTilÅSaksbehandle,
                        sykepengerErstatningGrunnlag?.vurdering
                    )
                )
            }
        }
    }
}
