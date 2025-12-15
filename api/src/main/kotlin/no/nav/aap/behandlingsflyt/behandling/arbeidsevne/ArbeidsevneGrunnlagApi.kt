package no.nav.aap.behandlingsflyt.behandling.arbeidsevne

import com.papsign.ktor.openapigen.route.path.normal.NormalOpenAPIRoute
import com.papsign.ktor.openapigen.route.response.respond
import com.papsign.ktor.openapigen.route.route
import no.nav.aap.behandlingsflyt.behandling.ansattinfo.AnsattInfoService
import no.nav.aap.behandlingsflyt.behandling.vurdering.VurdertAvService
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.arbeidsevne.ArbeidsevneRepository
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.Definisjon
import no.nav.aap.behandlingsflyt.kontrakt.behandling.BehandlingReferanse
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.Behandling
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingRepository
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.flate.BehandlingReferanseService
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.SakRepository
import no.nav.aap.behandlingsflyt.tilgang.kanSaksbehandle
import no.nav.aap.behandlingsflyt.tilgang.relevanteIdenterForBehandlingResolver
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

            respond(arbeidsevneGrunnlag(dataSource, behandlingReferanse, kanSaksbehandle(), repositoryRegistry, gatewayProvider))
        }
    }
}

private fun arbeidsevneGrunnlag(
    dataSource: DataSource,
    behandlingReferanse: BehandlingReferanse,
    kanSaksbehandle: Boolean,
    repositoryRegistry: RepositoryRegistry,
    gatewayProvider: GatewayProvider,
): ArbeidsevneGrunnlagDto {
    return dataSource.transaction { connection ->
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

        val vedtatteVerdier =
            behandling.forrigeBehandlingId?.let { arbeidsevneRepository.hentHvisEksisterer(it) }?.vurderinger.orEmpty()
        val historikk = arbeidsevneRepository.hentAlleVurderinger(behandling.sakId, behandling.id)

        val nyeVurderinger = nåTilstand?.filter { it.vurdertIBehandling == behandling.id } ?: emptyList()

        ArbeidsevneGrunnlagDto(
            harTilgangTilÅSaksbehandle = kanSaksbehandle,
            historikk = historikk.map { it.toDto() }.sortedBy { it.vurderingsTidspunkt }.toSet(),
            vurderinger =
                nåTilstand
                    ?.filterNot { vedtatteVerdier.contains(it) }
                    ?.map { it.toDto(AnsattInfoService(gatewayProvider).hentAnsattNavnOgEnhet(it.vurdertAv)) }
                    ?.sortedBy { it.fraDato }
                    .orEmpty(),
            gjeldendeVedtatteVurderinger =
                vedtatteVerdier
                    .map { it.toDto() }
                    .sortedBy { it.fraDato },
            kanVurderes = listOf(sak.rettighetsperiode),
            behøverVurderinger = emptyList(),
            nyeVurderinger = nyeVurderinger.map { it.toResponse(vurdertAvService) },
            sisteVedtatteVurderinger = forrigeGrunnlag?.gjeldendeVurderinger().orEmpty().toResponse(vurdertAvService)
        )
    }
}