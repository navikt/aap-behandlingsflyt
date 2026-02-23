package no.nav.aap.behandlingsflyt.behandling.beregning.grunnlag.sykdom.bistand

import com.papsign.ktor.openapigen.route.path.normal.NormalOpenAPIRoute
import com.papsign.ktor.openapigen.route.response.respond
import com.papsign.ktor.openapigen.route.route
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.AvklaringsbehovMetadataService
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.AvklaringsbehovRepository
import no.nav.aap.behandlingsflyt.behandling.vurdering.VurdertAvService
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.bistand.BistandRepository
import no.nav.aap.behandlingsflyt.forretningsflyt.steg.VurderBistandsbehovSteg
import no.nav.aap.behandlingsflyt.harTilgangOgKanSaksbehandle
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

fun NormalOpenAPIRoute.bistandsgrunnlagApi(
    dataSource: DataSource,
    repositoryRegistry: RepositoryRegistry,
    gatewayProvider: GatewayProvider,
) {
    route("/api/behandling") {
        route("/{referanse}/grunnlag/bistand") {
            getGrunnlag<BehandlingReferanse, BistandGrunnlagResponse>(
                relevanteIdenterResolver = relevanteIdenterForBehandlingResolver(repositoryRegistry, dataSource),
                behandlingPathParam = BehandlingPathParam("referanse"),
                avklaringsbehovKode = Definisjon.AVKLAR_BISTANDSBEHOV.kode.toString()
            ) { req ->
                val respons = dataSource.transaction(readOnly = true) { connection ->
                    val repositoryProvider = repositoryRegistry.provider(connection)
                    val vurdertAvService = VurdertAvService(repositoryProvider, gatewayProvider)
                    val behandlingRepository = repositoryProvider.provide<BehandlingRepository>()
                    val sakRepository = repositoryProvider.provide<SakRepository>()
                    val bistandRepository = repositoryProvider.provide<BistandRepository>()
                    val avklaringsbehovRepository = repositoryProvider.provide<AvklaringsbehovRepository>()
                    val vurderBistandsbehovSteg = VurderBistandsbehovSteg.konstruer(repositoryProvider, gatewayProvider)
                    val avklaringsbehovMetadataService = AvklaringsbehovMetadataService(repositoryProvider, gatewayProvider)

                    val behandling: Behandling =
                        BehandlingReferanseService(behandlingRepository).behandling(req)

                    val grunnlag = bistandRepository.hentHvisEksisterer(behandling.id)

                    val sak = sakRepository.hent(behandling.sakId)

                    val nyeVurderinger = grunnlag?.bistandsvurderingerVurdertIBehandling(behandling.id)
                        .orEmpty()
                        .map {
                            BistandVurderingResponse.fraDomene(
                                bistandsvurdering = it,
                                vurdertAvService = vurdertAvService
                            )
                        }

                    val avklaringsbehovene = avklaringsbehovRepository.hentAvklaringsbehovene(behandling.id)
                    val perioderVedtaketBehøverVurdering =
                        avklaringsbehovene.hentBehovForDefinisjon(Definisjon.AVKLAR_BISTANDSBEHOV)
                            ?.perioderVedtaketBehøverVurdering()
                            .orEmpty()

                    BistandGrunnlagResponse(
                        harTilgangTilÅSaksbehandle = harTilgangOgKanSaksbehandle(kanSaksbehandle(), avklaringsbehovene),
                        nyeVurderinger = nyeVurderinger,
                        sisteVedtatteVurderinger = BistandVurderingResponse.fraDomene(
                            grunnlag?.vedtattBistandstidslinje(behandling.id).orEmpty(),
                            vurdertAvService,
                        ),
                        kanVurderes = listOf(sak.rettighetsperiode), // TODO: Undersøk om denne gir noe verdi
                        behøverVurderinger = perioderVedtaketBehøverVurdering.toList(),
                        ikkeRelevantePerioder = avklaringsbehovMetadataService.perioderSomSkalFremhevesSomIkkeRelevant(vurderBistandsbehovSteg, behandling)
                    )
                }

                respond(respons)
            }
        }
    }
}
