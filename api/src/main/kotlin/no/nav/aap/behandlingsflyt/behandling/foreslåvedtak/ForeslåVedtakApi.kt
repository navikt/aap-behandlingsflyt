package no.nav.aap.behandlingsflyt.behandling.foreslåvedtak

import com.papsign.ktor.openapigen.route.path.normal.NormalOpenAPIRoute
import com.papsign.ktor.openapigen.route.response.respond
import com.papsign.ktor.openapigen.route.route
import no.nav.aap.behandlingsflyt.behandling.vilkår.TidligereVurderinger
import no.nav.aap.behandlingsflyt.behandling.vilkår.TidligereVurderingerImpl
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.stansopphør.Opphør
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.stansopphør.Stans
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.stansopphør.StansOpphørRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.underveis.UnderveisRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Utfall
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Vilkårsresultat
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.VilkårsresultatRepository
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.Definisjon
import no.nav.aap.behandlingsflyt.kontrakt.behandling.BehandlingReferanse
import no.nav.aap.behandlingsflyt.kontrakt.behandling.TypeBehandling
import no.nav.aap.behandlingsflyt.kontrakt.steg.StegType
import no.nav.aap.behandlingsflyt.periodisering.FlytKontekstMedPeriodeService
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingRepository
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.flate.BehandlingReferanseService
import no.nav.aap.behandlingsflyt.tilgang.kanSaksbehandle
import no.nav.aap.behandlingsflyt.tilgang.relevanteIdenterForBehandlingResolver
import no.nav.aap.behandlingsflyt.unleash.BehandlingsflytFeature
import no.nav.aap.behandlingsflyt.unleash.UnleashGateway
import no.nav.aap.behandlingsflyt.utils.tilForeslåVedtakDataTidslinje
import no.nav.aap.komponenter.dbconnect.transaction
import no.nav.aap.komponenter.gateway.GatewayProvider
import no.nav.aap.komponenter.repository.RepositoryRegistry
import no.nav.aap.komponenter.tidslinje.Segment
import no.nav.aap.komponenter.tidslinje.Tidslinje
import no.nav.aap.komponenter.type.Periode
import no.nav.aap.tilgang.BehandlingPathParam
import no.nav.aap.tilgang.getGrunnlag
import javax.sql.DataSource

fun NormalOpenAPIRoute.foreslaaVedtakApi(
    dataSource: DataSource,
    repositoryRegistry: RepositoryRegistry,
    gatewayProvider: GatewayProvider
) {
    route("/api/behandling") {
        route("/{referanse}/grunnlag/foreslaa-vedtak").getGrunnlag<BehandlingReferanse, ForeslåVedtakResponse>(
            relevanteIdenterResolver = relevanteIdenterForBehandlingResolver(repositoryRegistry, dataSource),
            behandlingPathParam = BehandlingPathParam("referanse"),
            påkrevdRolle = Definisjon.FORESLÅ_VEDTAK.løsesAv
        ) { behandlingReferanse ->
            val response =
                dataSource.transaction(readOnly = true) { conn ->
                    val repositoryProvider = repositoryRegistry.provider(conn)
                    val tidligereVurderingerImpl = TidligereVurderingerImpl(repositoryProvider, gatewayProvider)
                    val flytKontekstMedPeriodeService =
                        FlytKontekstMedPeriodeService(repositoryProvider, gatewayProvider)
                    val behandlingRepository = repositoryProvider.provide<BehandlingRepository>()
                    val behandling =
                        BehandlingReferanseService(behandlingRepository).behandling(behandlingReferanse)
                    val underveisRepository = repositoryProvider.provide<UnderveisRepository>()
                    val underveisGrunnlag = underveisRepository.hentHvisEksisterer(behandling.id)
                    val vilkårsresultatRepository = repositoryProvider.provide<VilkårsresultatRepository>()
                    val vilkårsresultat = vilkårsresultatRepository.hent(behandling.id)
                    val stansOpphørGrunnlag =
                        repositoryProvider.provide<StansOpphørRepository>().hentHvisEksisterer(behandling.id)
                    val stansOgOpphør = stansOpphørGrunnlag?.gjeldendeStansOgOpphør()
                        ?.associate({ it.fom to listOf(it) })
                        .orEmpty()
                    val referanseOppslag = behandlingRepository
                        .hentAlleFor(behandling.sakId, TypeBehandling.ytelseBehandlingstyper())
                        .associate { it.id to it.referanse }
                    val visPeriodeEndringer = gatewayProvider.provide<UnleashGateway>()
                        .isEnabled(BehandlingsflytFeature.VedtaksperiodeBeslutter)
                    val erFørstegangsbehandling = behandling.typeBehandling() == TypeBehandling.Førstegangsbehandling

                    val stansOgOpphørDto = stansOgOpphør.map { (fom, historikk) ->
                        StansOpphørDto(
                            stansOpphørFraOgMed = fom,
                            historikk = historikk.map { vurdering ->
                                val årsaker = vurdering.vurdering.årsaker.toList()
                                StansOpphørVurderingDto(
                                    type = when (vurdering.vurdering) {
                                        is Opphør -> StansOpphørVurderingTypeDto.OPPHØR
                                        is Stans -> StansOpphørVurderingTypeDto.STANS
                                    },
                                    årsaker = årsaker,
                                    hjemler = årsaker.map { it.hjemmel }.distinct(),
                                    behandlingReferanse = requireNotNull(referanseOppslag[vurdering.vurdertIBehandling]) {
                                        "Finner ikke ytelsesbehandling i sak med behandlingsid"
                                    }.referanse,
                                    erNy = visPeriodeEndringer && !erFørstegangsbehandling && vurdering.vurdertIBehandling == behandling.id
                                )
                            }
                        )
                    }

                    val kontekstMedPerioder =
                        flytKontekstMedPeriodeService.utled(behandling.flytKontekst(), StegType.FORESLÅ_VEDTAK)
                    val tidslinjeTidligereVurdering = tidligereVurderingerImpl.behandlingsutfall(
                        kontekstMedPerioder,
                        StegType.FORESLÅ_VEDTAK
                    )

                    val uunngåeligAvslagTidslinje = utledUunngåeligAvslagTidslinje(tidslinjeTidligereVurdering, vilkårsresultat)

                    if (underveisGrunnlag == null) {
                        ForeslåVedtakResponse(emptyList(), stansOgOpphørDto, kanSaksbehandle())
                    } else {
                        val foreslåVedtakData: Tidslinje<Pair<ForeslåVedtakData, List<VilkårsavslagDto>>> =
                            underveisGrunnlag
                                .tilForeslåVedtakDataTidslinje()
                                .leftJoin(uunngåeligAvslagTidslinje) { data, vilkårsavslag ->
                                    data to (vilkårsavslag?.let { listOf(it) } ?: emptyList())
                                }
                                .komprimer()

                        val forrigeTidslinje: Tidslinje<ForeslåVedtakData>? =
                            if (!visPeriodeEndringer) null else
                                behandling.forrigeBehandlingId?.let { forrigeId ->
                                    underveisRepository.hentHvisEksisterer(forrigeId)
                                        ?.tilForeslåVedtakDataTidslinje()
                                        ?: Tidslinje()
                                }

                        val forrigeHelePeriode = forrigeTidslinje?.helePeriodenOrNull()
                        val gjeldendeHelePeriode = foreslåVedtakData.helePeriodenOrNull()

                        val endretRettighetsperiode =
                            if (visPeriodeEndringer && forrigeHelePeriode != null && gjeldendeHelePeriode != null && forrigeHelePeriode != gjeldendeHelePeriode) {
                                EndretRettighetsperiodeDto(fra = forrigeHelePeriode, til = gjeldendeHelePeriode)
                            } else {
                                null
                            }

                        val forrigeSegmenter: List<Segment<ForeslåVedtakData>>? =
                            forrigeTidslinje?.segmenter()?.toList()

                        val foreslåVedtakPerioder =
                            foreslåVedtakData
                                .segmenter()
                                .map { segment ->
                                    val (data, vilkårsavslag) = segment.verdi
                                    val endring = utledEndringFraForrige(
                                        segment = segment.periode,
                                        utfall = data.utfall,
                                        forrigeSegmenter = forrigeSegmenter,
                                    )

                                    ForeslåVedtakDto(
                                        periode = segment.periode,
                                        utfall = data.utfall,
                                        rettighetsType = data.rettighetsType,
                                        avslagsårsak = AvslagsårsakDto(
                                            vilkårsavslag = vilkårsavslag,
                                            underveisavslag = data.underveisÅrsak
                                        ),
                                        tidligerePerioder = endring.tidligerePerioder,
                                        tidligereVarÅpen = endring.tidligereVarÅpen,
                                        erNy = endring.erNy
                                    )
                                }

                        ForeslåVedtakResponse(
                            foreslåVedtakPerioder,
                            stansOgOpphørDto,
                            harTilgangTilÅSaksbehandle = kanSaksbehandle(),
                            endretRettighetsperiode = endretRettighetsperiode,
                        )
                    }
                }
            respond(response)
        }
    }
}

internal data class EndringFraForrige(
    val erNy: Boolean,
    val tidligerePerioder: List<Periode>,
    val tidligereVarÅpen: Boolean,
)

internal fun utledEndringFraForrige(
    segment: Periode,
    utfall: Utfall,
    forrigeSegmenter: List<Segment<ForeslåVedtakData>>?,
): EndringFraForrige {
    if (forrigeSegmenter.isNullOrEmpty()) {
        return EndringFraForrige(erNy = false, tidligerePerioder = emptyList(), tidligereVarÅpen = false)
    }

    return if (utfall == Utfall.OPPFYLT) {
        val forrigePeriode = forrigeSegmenter
            .firstOrNull { it.verdi.utfall == Utfall.OPPFYLT && it.periode.overlapper(segment) }
            ?.periode
        EndringFraForrige(
            erNy = forrigePeriode == null,
            tidligerePerioder = listOfNotNull(forrigePeriode?.takeIf { it != segment }),
            tidligereVarÅpen = false,
        )
    } else {
        val forrigeIndex = forrigeSegmenter.indexOfFirst {
            it.verdi.utfall == Utfall.IKKE_OPPFYLT && it.periode.overlapper(segment)
        }
        val forrige = forrigeSegmenter.getOrNull(forrigeIndex)
        val endret = forrige != null && forrige.periode != segment
        EndringFraForrige(
            erNy = forrige == null,
            tidligerePerioder = listOfNotNull(forrige?.periode?.takeIf { endret }),
            tidligereVarÅpen = endret &&
                    forrigeIndex > forrigeSegmenter.indexOfLast { it.verdi.utfall == Utfall.OPPFYLT },
        )
    }
}

internal fun utledUunngåeligAvslagTidslinje(
    tidligereVurdering: Tidslinje<TidligereVurderinger.Behandlingsutfall>,
    vilkårsresultat: Vilkårsresultat,
): Tidslinje<VilkårsavslagDto> {
    val segmenter = tidligereVurdering.segmenter().mapNotNull { segment ->
        val uunngåeligAvslag = segment.verdi as? TidligereVurderinger.UunngåeligAvslag
            ?: return@mapNotNull null

        val avslagsårsak = vilkårsresultat
            .tidslinjeFor(uunngåeligAvslag.vilkårtype)
            .begrensetTil(segment.periode)
            .segmenter()
            .firstOrNull { it.verdi.utfall == Utfall.IKKE_OPPFYLT && it.verdi.avslagsårsak != null }
            ?.verdi
            ?.avslagsårsak

        Segment(
            segment.periode,
            VilkårsavslagDto(vilkår = uunngåeligAvslag.vilkårtype.hjemmel, avslagsårsak = avslagsårsak)
        )
    }
    return Tidslinje(segmenter)
}



