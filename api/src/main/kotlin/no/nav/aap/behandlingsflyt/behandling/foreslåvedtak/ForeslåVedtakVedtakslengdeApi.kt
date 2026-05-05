package no.nav.aap.behandlingsflyt.behandling.foreslåvedtak

import com.papsign.ktor.openapigen.route.path.normal.NormalOpenAPIRoute
import com.papsign.ktor.openapigen.route.response.respond
import com.papsign.ktor.openapigen.route.route
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.rettighetstype.RettighetstypeRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.stansopphør.GjeldendeStansEllerOpphør
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.stansopphør.OpphevetStansEllerOpphør
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.stansopphør.Opphør
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.stansopphør.Stans
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.stansopphør.StansOpphørRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.RettighetsType
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Utfall
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.vedtakslengde.VedtakslengdeRepository
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.Definisjon
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.FORESLÅ_VEDTAK_VEDTAKSLENGDE_KODE
import no.nav.aap.behandlingsflyt.kontrakt.behandling.BehandlingReferanse
import no.nav.aap.behandlingsflyt.kontrakt.behandling.TypeBehandling
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingRepository
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.flate.BehandlingReferanseService
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.SakRepository
import no.nav.aap.behandlingsflyt.tilgang.relevanteIdenterForBehandlingResolver
import no.nav.aap.komponenter.dbconnect.transaction
import no.nav.aap.komponenter.repository.RepositoryRegistry
import no.nav.aap.komponenter.tidslinje.Tidslinje
import no.nav.aap.komponenter.type.Periode
import no.nav.aap.tilgang.BehandlingPathParam
import no.nav.aap.tilgang.getGrunnlag
import javax.sql.DataSource

fun NormalOpenAPIRoute.foreslaaVedtakVedtakslengdeApi(
    dataSource: DataSource,
    repositoryRegistry: RepositoryRegistry
) {
    route("/api/behandling") {
        route("/{referanse}/grunnlag/foreslaa-vedtak-vedtakslengde").getGrunnlag<BehandlingReferanse, VedtakslengdeVedtakResponse>(
            relevanteIdenterResolver = relevanteIdenterForBehandlingResolver(repositoryRegistry, dataSource),
            behandlingPathParam = BehandlingPathParam("referanse"),
            påkrevdRolle = Definisjon.FORESLÅ_VEDTAK_VEDTAKSLENGDE.løsesAv
        ) { behandlingReferanse ->
            val response =
                dataSource.transaction(readOnly = true) { conn ->
                    val repositoryProvider = repositoryRegistry.provider(conn)
                    val behandlingRepository = repositoryProvider.provide<BehandlingRepository>()
                    val behandling =
                        BehandlingReferanseService(behandlingRepository).behandling(behandlingReferanse)

                    val gjeldendeSluttdato = repositoryProvider.provide<VedtakslengdeRepository>()
                        .hentHvisEksisterer(behandling.id)
                        ?.gjeldendeVurdering()
                        ?.sluttdato

                    val rettighetstypeTidslinje = repositoryProvider.provide<RettighetstypeRepository>()
                        .hentHvisEksisterer(behandling.id)
                        ?.rettighetstypeTidslinje

                    val stansOpphørGrunnlag =
                        repositoryProvider.provide<StansOpphørRepository>().hentHvisEksisterer(behandling.id)
                    val stansOgOpphør = stansOpphørGrunnlag?.stansOgOpphørMedHistorikk() ?: emptyMap()
                    val referanseOppslag = behandlingRepository
                        .hentAlleFor(behandling.sakId, TypeBehandling.ytelseBehandlingstyper())
                        .associate { it.id to it.referanse }

                    val stansOgOpphørDto = stansOgOpphør.map { (fom, historikk) ->
                        StansOpphørDto(
                            stansOpphørFraOgMed = fom,
                            historikk = historikk.map { vurdering ->
                                StansOpphørVurderingDto(
                                    type = when (vurdering) {
                                        is GjeldendeStansEllerOpphør -> when (vurdering.vurdering) {
                                            is Opphør -> StansOpphørVurderingTypeDto.OPPHØR
                                            is Stans -> StansOpphørVurderingTypeDto.STANS
                                        }
                                        is OpphevetStansEllerOpphør -> StansOpphørVurderingTypeDto.OPPHEVET
                                    },
                                    årsaker = when (vurdering) {
                                        is GjeldendeStansEllerOpphør -> vurdering.vurdering.årsaker.toList()
                                        is OpphevetStansEllerOpphør -> emptyList()
                                    },
                                    behandlingReferanse = requireNotNull(referanseOppslag[vurdering.vurdertIBehandling]) {
                                        "Finner ikke ytelsesbehandling i sak med behandlingsid"
                                    }.referanse
                                )
                            }
                        )
                    }

                    val perioder = if (gjeldendeSluttdato == null || rettighetstypeTidslinje == null) {
                        null
                    } else {
                        val rettighetsperiodeFom = repositoryProvider.provide<SakRepository>()
                            .hent(behandling.sakId)
                            .rettighetsperiode
                            .fom
                        val vedtakslengdePeriode = Periode(rettighetsperiodeFom, gjeldendeSluttdato)
                        utledPerioder(rettighetstypeTidslinje, vedtakslengdePeriode)
                    }

                    VedtakslengdeVedtakResponse(perioder, stansOgOpphørDto)
                }
            respond(response)
        }
    }
}

private fun utledPerioder(
    rettighetstypeTidslinje: Tidslinje<RettighetsType>,
    vedtakslengdePeriode: Periode
): List<VedtakslengdeVedtakDto> {
    val fullDekning = Tidslinje(vedtakslengdePeriode, Unit)

    return fullDekning
        .leftJoin(rettighetstypeTidslinje) { _, rettighetsType -> rettighetsType }
        .komprimer()
        .segmenter()
        .map { segment ->
            VedtakslengdeVedtakDto(
                periode = segment.periode,
                rettighetsType = segment.verdi,
                utfall = if (segment.verdi != null) Utfall.OPPFYLT else Utfall.IKKE_OPPFYLT
            )
        }
}
