package no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.bistand.flate

import com.papsign.ktor.openapigen.route.path.normal.NormalOpenAPIRoute
import com.papsign.ktor.openapigen.route.response.respond
import com.papsign.ktor.openapigen.route.route
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.bistand.BistandRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.sykdom.SykdomRepository
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.Definisjon
import no.nav.aap.behandlingsflyt.kontrakt.behandling.BehandlingReferanse
import no.nav.aap.behandlingsflyt.kontrakt.behandling.TypeBehandling
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

fun NormalOpenAPIRoute.bistandsgrunnlagApi(dataSource: DataSource, repositoryRegistry: RepositoryRegistry) {
    route("/api/behandling") {
        route("/{referanse}/grunnlag/bistand") {
            authorizedGet<BehandlingReferanse, BistandGrunnlagDto>(
                AuthorizationParamPathConfig(
                    behandlingPathParam = BehandlingPathParam(
                        "referanse"
                    )
                )
            ) { req ->
                val respons = dataSource.transaction(readOnly = true) { connection ->
                    val repositoryProvider = repositoryRegistry.provider(connection)
                    val behandlingRepository = repositoryProvider.provide<BehandlingRepository>()
                    val bistandRepository = repositoryProvider.provide<BistandRepository>()
                    val sykdomRepository = repositoryProvider.provide<SykdomRepository>()

                    val behandling: Behandling =
                        BehandlingReferanseService(behandlingRepository).behandling(req)

                    val historiskeVurderinger =
                        bistandRepository.hentHistoriskeBistandsvurderinger(behandling.sakId, behandling.id)
                    val grunnlag = bistandRepository.hentHvisEksisterer(behandling.id)
                    val nåTilstand = grunnlag?.vurderinger.orEmpty()
                    val vedtatteBistandsvurderinger = behandling.forrigeBehandlingId
                        ?.let { bistandRepository.hentHvisEksisterer(it) }
                        ?.vurderinger.orEmpty()
                    val vurdering = nåTilstand
                        .filterNot { it in vedtatteBistandsvurderinger }
                        .singleOrNull()
                    
                    val gjeldendeSykdomsvurderinger =
                        sykdomRepository.hentHvisEksisterer(behandling.id)?.sykdomsvurderinger!!
                    
                    val sisteSykdomsvurdering = gjeldendeSykdomsvurderinger.maxBy { it.opprettet }

                    val harTilgangTilÅSaksbehandle = TilgangGatewayImpl.sjekkTilgangTilBehandling(
                        req.referanse,
                        Definisjon.AVKLAR_BISTANDSBEHOV.kode.toString(),
                        token()
                    )

                    val erOppfylt11_5 = if (behandling.typeBehandling() == TypeBehandling.Revurdering) sisteSykdomsvurdering.erOppfyltSettBortIfraVissVarighet() else sisteSykdomsvurdering.erOppfylt()

                    BistandGrunnlagDto(
                        harTilgangTilÅSaksbehandle = harTilgangTilÅSaksbehandle,
                        BistandVurderingDto.fraBistandVurdering(vurdering),
                        vedtatteBistandsvurderinger.map { it.toDto() },
                        historiskeVurderinger.map { it.toDto() },
                        gjeldendeSykdomsvurderinger.map{it.toDto()},
                        erOppfylt11_5
                    )
                }

                respond(respons)
            }
        }
    }
}
