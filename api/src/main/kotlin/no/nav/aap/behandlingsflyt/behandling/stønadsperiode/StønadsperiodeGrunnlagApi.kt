package no.nav.aap.behandlingsflyt.behandling.stønadsperiode

import com.papsign.ktor.openapigen.route.path.normal.NormalOpenAPIRoute
import com.papsign.ktor.openapigen.route.response.respond
import com.papsign.ktor.openapigen.route.route
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.stønadsperiode.StønadsperiodeRepository
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.Definisjon
import no.nav.aap.behandlingsflyt.kontrakt.behandling.BehandlingReferanse
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingRepository
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.flate.BehandlingReferanseService
import no.nav.aap.behandlingsflyt.tilgang.kanSaksbehandle
import no.nav.aap.behandlingsflyt.tilgang.relevanteIdenterForBehandlingResolver
import no.nav.aap.komponenter.dbconnect.transaction
import no.nav.aap.komponenter.gateway.GatewayProvider
import no.nav.aap.komponenter.repository.RepositoryRegistry
import no.nav.aap.tilgang.BehandlingPathParam
import no.nav.aap.tilgang.getGrunnlag
import javax.sql.DataSource

fun NormalOpenAPIRoute.stønadsperiodeGrunnlagApi(
    dataSource: DataSource,
    repositoryRegistry: RepositoryRegistry,
    gatewayProvider: GatewayProvider
) {
    route("api/behandling/{referanse}/grunnlag/stonadsperiode") {
        getGrunnlag<BehandlingReferanse, StønadsperiodeGrunnlagDto>(
            relevanteIdenterResolver = relevanteIdenterForBehandlingResolver(repositoryRegistry, dataSource),
            behandlingPathParam = BehandlingPathParam("referanse"),
            påkrevdRolle = Definisjon.VURDER_KRAV.løsesAv // TODO: Oppdater denne når vi har avklaringsbehov
        ) { req ->
            val response = dataSource.transaction(readOnly = true) { connection ->
                val repositoryProvider = repositoryRegistry.provider(connection)
                val behandlingRepository = repositoryProvider.provide<BehandlingRepository>()
                val behandling = BehandlingReferanseService(behandlingRepository).behandling(req)

                val stønadsperiodeRepository = repositoryProvider.provide<StønadsperiodeRepository>()
                val stønadsperiodeGrunnlag = stønadsperiodeRepository.hentHvisEksisterer(behandling.id)
                val vedtattGrunnlag =
                    behandling.forrigeBehandlingId?.let { stønadsperiodeRepository.hentHvisEksisterer(it) }

                StønadsperiodeGrunnlagDto(
                    harTilgangTilÅSaksbehandle = kanSaksbehandle(),
                    nyeVurderinger = stønadsperiodeGrunnlag?.gjeldendeVurderinger()
                        .orEmpty().filter { it.vurdertIBehandling == behandling.id }.map { it.somDto() },
                    vedtatteVurderinger = vedtattGrunnlag?.gjeldendeVurderinger().orEmpty().map { it.somDto() }
                )
            }
            respond(response)
        }


    }
}