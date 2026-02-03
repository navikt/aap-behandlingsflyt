package no.nav.aap.behandlingsflyt.behandling.arbeidsevne

import com.papsign.ktor.openapigen.route.path.normal.NormalOpenAPIRoute
import com.papsign.ktor.openapigen.route.response.respond
import com.papsign.ktor.openapigen.route.route
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.AvklaringsbehovRepository
import no.nav.aap.behandlingsflyt.behandling.vurdering.VurdertAvService
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.arbeidsevne.ArbeidsevneRepository
import no.nav.aap.behandlingsflyt.harTilgangOgKanSaksbehandle
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.Definisjon
import no.nav.aap.behandlingsflyt.kontrakt.behandling.BehandlingReferanse
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.Behandling
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingRepository
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.flate.BehandlingReferanseService
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.SakRepository
import no.nav.aap.behandlingsflyt.tilgang.kanSaksbehandle
import no.nav.aap.behandlingsflyt.tilgang.relevanteIdenterForBehandlingResolver
import no.nav.aap.behandlingsflyt.unleash.BehandlingsflytFeature
import no.nav.aap.behandlingsflyt.unleash.UnleashGateway
import no.nav.aap.komponenter.dbconnect.transaction
import no.nav.aap.komponenter.gateway.GatewayProvider
import no.nav.aap.komponenter.repository.RepositoryRegistry
import no.nav.aap.komponenter.tidslinje.orEmpty
import no.nav.aap.tilgang.BehandlingPathParam
import no.nav.aap.tilgang.getGrunnlag
import javax.sql.DataSource

fun NormalOpenAPIRoute.arbeidsevneGrunnlagApi(
    dataSource: DataSource,
    repositoryRegistry: RepositoryRegistry,
    gatewayProvider: GatewayProvider,
) {
    route("/api/behandling/{referanse}/grunnlag/arbeidsevne") {
        getGrunnlag<BehandlingReferanse, ArbeidsevneGrunnlagDto>(
            relevanteIdenterResolver = relevanteIdenterForBehandlingResolver(repositoryRegistry, dataSource),
            behandlingPathParam = BehandlingPathParam("referanse"),
            avklaringsbehovKode = Definisjon.FASTSETT_ARBEIDSEVNE.kode.toString()

        ) { behandlingReferanse ->
            val arbeidsevneGrunnlag = dataSource.transaction { connection ->
                val repositoryProvider = repositoryRegistry.provider(connection)
                val behandlingRepository = repositoryProvider.provide<BehandlingRepository>()
                val sakRepository = repositoryProvider.provide<SakRepository>()
                val arbeidsevneRepository = repositoryProvider.provide<ArbeidsevneRepository>()
                val vurdertAvService = VurdertAvService(repositoryProvider, gatewayProvider)

                val behandling: Behandling =
                    BehandlingReferanseService(behandlingRepository).behandling(behandlingReferanse)
                val sak = sakRepository.hent(behandling.sakId)

                val nåTilstand = arbeidsevneRepository.hentHvisEksisterer(behandling.id)?.vurderinger
                val forrigeGrunnlag = behandling.forrigeBehandlingId?.let { arbeidsevneRepository.hentHvisEksisterer(it) }
                val nyeVurderinger = nåTilstand?.filter { it.vurdertIBehandling == behandling.id } ?: emptyList()

                val avklaringsbehovRepository = repositoryProvider.provide<AvklaringsbehovRepository>()
                val avklaringsbehovene = avklaringsbehovRepository.hentAvklaringsbehovene(behandling.id)

                val unleashGateway = gatewayProvider.provide<UnleashGateway>()

                ArbeidsevneGrunnlagDto(
                    harTilgangTilÅSaksbehandle = harTilgangOgKanSaksbehandle(kanSaksbehandle(), avklaringsbehovene),
                    kanVurderes = listOf(sak.rettighetsperiode),
                    behøverVurderinger = emptyList(),
                    nyeVurderinger = nyeVurderinger.map { it.toResponse(vurdertAvService) },
                    sisteVedtatteVurderinger = forrigeGrunnlag?.gjeldendeVurderinger().orEmpty().toResponse(vurdertAvService),
                )
            }

            respond(arbeidsevneGrunnlag)
        }
    }
}