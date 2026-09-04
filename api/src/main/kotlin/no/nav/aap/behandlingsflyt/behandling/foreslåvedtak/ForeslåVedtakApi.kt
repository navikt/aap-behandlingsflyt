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
import no.nav.aap.behandlingsflyt.utils.tilForeslåVedtakDataTidslinje
import no.nav.aap.komponenter.dbconnect.transaction
import no.nav.aap.komponenter.gateway.GatewayProvider
import no.nav.aap.komponenter.repository.RepositoryRegistry
import no.nav.aap.komponenter.tidslinje.Segment
import no.nav.aap.komponenter.tidslinje.Tidslinje
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

                    val stansOgOpphørDto = stansOgOpphør.map { (fom, historikk) ->
                        StansOpphørDto(
                            stansOpphørFraOgMed = fom,
                            historikk = historikk.map { vurdering ->
                                StansOpphørVurderingDto(
                                    type = when (vurdering.vurdering) {
                                        is Opphør -> StansOpphørVurderingTypeDto.OPPHØR
                                        is Stans -> StansOpphørVurderingTypeDto.STANS
                                    },
                                    årsaker = vurdering.vurdering.årsaker.toList(),
                                    behandlingReferanse = requireNotNull(referanseOppslag[vurdering.vurdertIBehandling]) {
                                        "Finner ikke ytelsesbehandling i sak med behandlingsid"
                                    }
                                        .referanse
                                )
                            }
                        )
                    }

                    val kontekstMedPerioder = flytKontekstMedPeriodeService.utled(behandling.flytKontekst(), StegType.FORESLÅ_VEDTAK)
                    val tidslinjeTidligereVurdering = tidligereVurderingerImpl.behandlingsutfall(
                        kontekstMedPerioder,
                        StegType.FORESLÅ_VEDTAK
                    )

                    val uunngåeligAvslagTidslinje: Tidslinje<VilkårsavslagDto> = tidslinjeTidligereVurdering
                        .mapNotNull { utfall -> utfall as? TidligereVurderinger.UunngåeligAvslag }
                        .map { uunngåeligAvslag ->
                            val avslagsårsak = vilkårsresultat
                                .tidslinjeFor(uunngåeligAvslag.vilkårtype)
                                .segmenter()
                                .firstOrNull()
                                ?.verdi
                                ?.avslagsårsak

                            VilkårsavslagDto(
                                vilkår = uunngåeligAvslag.vilkårtype.hjemmel,
                                avslagsårsak = avslagsårsak
                            )
                        }

                    if (underveisGrunnlag == null) {
                        ForeslåVedtakResponse(emptyList(), stansOgOpphørDto, kanSaksbehandle())
                    } else {
                        val foreslåVedtakPerioder =
                            underveisGrunnlag
                                .tilForeslåVedtakDataTidslinje()
                                .segmenter()
                                .map {
                                    val vilkårsavslag =
                                        uunngåeligAvslagTidslinje.begrensetTil(it.periode).segmenter()
                                            .map { segment -> segment.verdi }

                                    ForeslåVedtakDto(
                                        periode = it.periode,
                                        utfall = it.verdi.utfall,
                                        rettighetsType = it.verdi.rettighetsType,
                                        avslagsårsak = AvslagsårsakDto(
                                            vilkårsavslag = vilkårsavslag,
                                            underveisavslag = it.verdi.underveisÅrsak
                                        )
                                    )
                                }
                        ForeslåVedtakResponse(
                            foreslåVedtakPerioder,
                            stansOgOpphørDto,
                            harTilgangTilÅSaksbehandle = kanSaksbehandle(),
                        )
                    }
                }
            respond(response)
        }
    }
}


