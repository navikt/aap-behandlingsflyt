package no.nav.aap.behandlingsflyt.behandling.beregning.grunnlag.sykdom.sykepengergrunnlag

import com.papsign.ktor.openapigen.route.path.normal.NormalOpenAPIRoute
import com.papsign.ktor.openapigen.route.response.respond
import com.papsign.ktor.openapigen.route.route
import no.nav.aap.behandlingsflyt.behandling.ansattinfo.AnsattInfoService
import no.nav.aap.behandlingsflyt.behandling.vurdering.VurdertAvResponse
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.sykdom.SykepengerErstatningRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.sykdom.SykepengerVurdering
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.Definisjon
import no.nav.aap.behandlingsflyt.kontrakt.behandling.BehandlingReferanse
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.Behandling
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


fun NormalOpenAPIRoute.sykepengerGrunnlagApi(dataSource: DataSource, repositoryRegistry: RepositoryRegistry) {
    route("/api/behandling") {
        route("/{referanse}/grunnlag/sykdom/sykepengergrunnlag") {
            authorizedGet<BehandlingReferanse, SykepengerGrunnlagResponse>(
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

                val harTilgangTilÅSaksbehandle = GatewayProvider.provide<TilgangGateway>().sjekkTilgangTilBehandling(
                    req.referanse,
                    Definisjon.AVKLAR_SYKEPENGEERSTATNING,
                    token()
                )


                respond(
                    SykepengerGrunnlagResponse(
                        harTilgangTilÅSaksbehandle = harTilgangTilÅSaksbehandle,
                        sykepengerErstatningGrunnlag?.vurdering?.tilResponse()
                    )
                )
            }
        }
    }
}

private fun SykepengerVurdering.tilResponse(): SykepengerVurderingResponse {
    val navnOgEnhet = AnsattInfoService().hentAnsattNavnOgEnhet(vurdertAv)
    return SykepengerVurderingResponse(
        begrunnelse = begrunnelse,
        dokumenterBruktIVurdering = dokumenterBruktIVurdering,
        harRettPå = harRettPå,
        grunn = grunn,
        vurdertAv = VurdertAvResponse(
            ident = vurdertAv,
            dato = vurdertTidspunkt?.toLocalDate() ?: error("Mangler dato for sykepengervurdering"),
            ansattnavn = navnOgEnhet?.navn,
            enhetsnavn = navnOgEnhet?.enhet
        )
    )
}
