package no.nav.aap.behandlingsflyt.behandling.arbeidsopptrapping

import com.papsign.ktor.openapigen.route.path.normal.NormalOpenAPIRoute
import com.papsign.ktor.openapigen.route.response.respond
import com.papsign.ktor.openapigen.route.route
import no.nav.aap.behandlingsflyt.behandling.vurdering.VurdertAvService
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.arbeidsopptrapping.ArbeidsopptrappingGrunnlag
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.arbeidsopptrapping.ArbeidsopptrappingRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.sykdom.SykdomRepository
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
import no.nav.aap.tilgang.BehandlingPathParam
import no.nav.aap.tilgang.getGrunnlag
import javax.sql.DataSource
import kotlin.collections.map
import kotlin.collections.orEmpty

fun NormalOpenAPIRoute.arbeidsopptrappingGrunnlagApi(
    dataSource: DataSource,
    repositoryRegistry: RepositoryRegistry,
    gatewayProvider: GatewayProvider,
) {
    val unleashGateway = gatewayProvider.provide<UnleashGateway>()

    route("/api/behandling/{referanse}/grunnlag/arbeidsopptrapping") {
        getGrunnlag<BehandlingReferanse, ArbeidsopptrappingGrunnlagResponse>(
            relevanteIdenterResolver = relevanteIdenterForBehandlingResolver(repositoryRegistry, dataSource),
            behandlingPathParam = BehandlingPathParam("referanse"),
            avklaringsbehovKode = Definisjon.ARBEIDSOPPTRAPPING.kode.toString()
        ) { behandlingReferanse ->
            val response = if (unleashGateway.isEnabled(BehandlingsflytFeature.Arbeidsopptrapping)) {
                dataSource.transaction { connection ->
                    val repositoryProvider = repositoryRegistry.provider(connection)
                    val behandlingRepository = repositoryProvider.provide<BehandlingRepository>()
                    val sakRepository = repositoryProvider.provide<SakRepository>()
                    val behandling: Behandling =
                        BehandlingReferanseService(behandlingRepository).behandling(behandlingReferanse)
                    val arbeidsopptrappingRepository = repositoryProvider.provide<ArbeidsopptrappingRepository>()
                    val vurdertAvService = VurdertAvService(repositoryProvider, gatewayProvider)
                    val sak = sakRepository.hent(behandling.sakId)
                    val sykdomRepository = repositoryProvider.provide<SykdomRepository>()

                    val grunnlag = arbeidsopptrappingRepository.hentHvisEksisterer(behandling.id)
                    val forrigeGrunnlag =
                        behandling.forrigeBehandlingId?.let { arbeidsopptrappingRepository.hentHvisEksisterer(it) }
                            ?: ArbeidsopptrappingGrunnlag(emptyList())

                    ArbeidsopptrappingGrunnlagResponse(
                        harTilgangTilÅSaksbehandle = kanSaksbehandle(),
                        sisteVedtatteVurderinger = ArbeidsopptrappingVurderingResponse.fraDomene(
                            forrigeGrunnlag.gjeldendeVurderinger(),
                            vurdertAvService
                        ),
                        nyeVurderinger = grunnlag?.vurderinger.orEmpty()
                            .filter { it.vurdertIBehandling == behandling.id }
                            .map { ArbeidsopptrappingVurderingResponse.fraDomene(it, vurdertAvService) },
                        kanVurderes = listOf(sak.rettighetsperiode), // Må justeres ift 11-5 / 11-6
                        behøverVurderinger = listOf(),
                    )
                }

            } else {
                ArbeidsopptrappingGrunnlagResponse(
                    kanSaksbehandle(), emptyList(), emptyList(), emptyList(), emptyList()
                )
            }

            respond(
                response
            )
        }
    }
}