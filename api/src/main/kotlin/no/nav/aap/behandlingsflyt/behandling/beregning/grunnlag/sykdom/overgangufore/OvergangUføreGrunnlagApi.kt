package no.nav.aap.behandlingsflyt.behandling.beregning.grunnlag.sykdom.overgangufore

import com.papsign.ktor.openapigen.route.path.normal.NormalOpenAPIRoute
import com.papsign.ktor.openapigen.route.response.respond
import com.papsign.ktor.openapigen.route.route
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.AvklaringsbehovRepository
import no.nav.aap.behandlingsflyt.behandling.beregning.grunnlag.sykdom.sykdom.SykdomsvurderingResponse
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.overgangufore.OvergangUføreRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.sykdom.SykdomRepository
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
import no.nav.aap.behandlingsflyt.behandling.vurdering.VurdertAvService
import no.nav.aap.behandlingsflyt.harTilgangOgKanSaksbehandle
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.SakRepository
import no.nav.aap.behandlingsflyt.tilgang.relevanteIdenterForBehandlingResolver
import no.nav.aap.behandlingsflyt.unleash.BehandlingsflytFeature
import no.nav.aap.behandlingsflyt.unleash.UnleashGateway
import no.nav.aap.komponenter.tidslinje.orEmpty
import no.nav.aap.komponenter.type.Periode
import kotlin.collections.orEmpty

fun NormalOpenAPIRoute.overgangUforeGrunnlagApi(
    dataSource: DataSource, repositoryRegistry: RepositoryRegistry,
    gatewayProvider: GatewayProvider,
) {
    route("/api/behandling") {
        route("/{referanse}/grunnlag/overgangufore") {
            getGrunnlag<BehandlingReferanse, OvergangUføreGrunnlagResponse>(
                relevanteIdenterResolver = relevanteIdenterForBehandlingResolver(repositoryRegistry, dataSource),
                behandlingPathParam = BehandlingPathParam("referanse"),
                avklaringsbehovKode = Definisjon.AVKLAR_OVERGANG_UFORE.kode.toString()
            ) { req ->
                val respons = dataSource.transaction(readOnly = true) { connection ->
                    val repositoryProvider = repositoryRegistry.provider(connection)
                    val behandlingRepository = repositoryProvider.provide<BehandlingRepository>()
                    val overgangUforeRepository = repositoryProvider.provide<OvergangUføreRepository>()
                    val sykdomRepository = repositoryProvider.provide<SykdomRepository>()
                    val vurdertAvService = VurdertAvService(repositoryProvider, gatewayProvider)
                    val sakRepository = repositoryProvider.provide<SakRepository>()

                    val behandling: Behandling =
                        BehandlingReferanseService(behandlingRepository).behandling(req)
                    val grunnlag = overgangUforeRepository.hentHvisEksisterer(behandling.id)
                    val avklaringsbehovRepository = repositoryProvider.provide<AvklaringsbehovRepository>()
                    val sak = sakRepository.hent(behandling.sakId)

                    val gjeldendeSykdomsvurderinger =
                        sykdomRepository.hentHvisEksisterer(behandling.id)?.sykdomsvurderinger.orEmpty()

                    val nyeVurderinger = grunnlag?.overgangUføreVurderingerVurdertIBehandling(behandling.id)
                        .orEmpty()
                        .map { OvergangUføreVurderingResponse.fraDomene(it, vurdertAvService) }

                    val nyesteVurdering = grunnlag?.overgangUføreVurderingerVurdertIBehandling(behandling.id)
                        ?.maxByOrNull { it.opprettet!! }
                        ?.let { OvergangUføreVurderingResponse.fraDomene(it, vurdertAvService) }

                    val avklaringsbehovene = avklaringsbehovRepository.hentAvklaringsbehovene(behandling.id)
                    val avklaringsbehov = avklaringsbehovene.hentBehovForDefinisjon(Definisjon.AVKLAR_OVERGANG_UFORE)

                    val unleashGateway = gatewayProvider.provide<UnleashGateway>()

                    OvergangUføreGrunnlagResponse(
                        harTilgangTilÅSaksbehandle = if (unleashGateway.isEnabled(BehandlingsflytFeature.EOSBeregning)) {
                            harTilgangOgKanSaksbehandle(kanSaksbehandle(), avklaringsbehovene)
                        } else {
                            kanSaksbehandle()
                        },
                        vurdering = nyesteVurdering, // TODO: Fjern
                        nyeVurderinger = nyeVurderinger,
                        // TODO: Fjern
                        gjeldendeVedtatteVurderinger = OvergangUføreVurderingResponse.fraDomene(
                            grunnlag?.vedtattOvergangUførevurderingstidslinje(behandling.id).orEmpty(),
                            vurdertAvService
                        ),
                        sisteVedtatteVurderinger = OvergangUføreVurderingResponse.fraDomene(
                            grunnlag?.vedtattOvergangUførevurderingstidslinje(behandling.id).orEmpty(),
                            vurdertAvService
                        ),
                        historiskeVurderinger = grunnlag?.historiskeOvergangUføreVurderinger(behandling.id).orEmpty()
                            .map { OvergangUføreVurderingResponse.fraDomene(it, vurdertAvService) },
                        gjeldendeSykdsomsvurderinger = gjeldendeSykdomsvurderinger.map {
                            SykdomsvurderingResponse.fraDomene(
                                it,
                                vurdertAvService
                            )
                        },
                        kanVurderes = listOf(
                            Periode(
                                sak.rettighetsperiode.fom.minusMonths(8),
                                sak.rettighetsperiode.tom
                            )
                        ),
                        behøverVurderinger = avklaringsbehov?.perioderVedtaketBehøverVurdering().orEmpty().toList(),
                        perioderSomIkkeErTilstrekkeligVurdert = avklaringsbehov?.perioderSomIkkeErTilstrekkeligVurdert()
                            .orEmpty().toList(),
                        kvalitetssikretAv = vurdertAvService.kvalitetssikretAv(
                            Definisjon.AVKLAR_OVERGANG_UFORE,
                            behandling.id
                        )
                    )
                }

                respond(respons)
            }
        }
    }
}


