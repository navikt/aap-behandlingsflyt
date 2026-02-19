package no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.stansopphør

import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.temporal.ChronoUnit

data class StansOpphørGrunnlag(
    val stansOgOpphør: Set<StansEllerOpphørVurdering>
) {
    fun gjeldendeStansOgOpphør(): Set<GjeldendeStansEllerOpphør> {
        return stansOgOpphør
            .groupBy { it.fom }
            .mapNotNull { (_, vedtak) -> vedtak.maxByOrNull { it.opprettet }!! }
            .mapNotNull {
                when (it) {
                    is GjeldendeStansEllerOpphør -> it
                    is OpphevetStansEllerOpphør -> null
                }
            }
            .toSet()
    }

    fun utledNyttGrunnlag(
        utlededeStansOgOpphør: Map<LocalDate, StansEllerOpphør>,
        behandlingId: BehandlingId,
        clock: Clock = Clock.systemDefaultZone(),
    ): StansOpphørGrunnlag {
        val endringer = mergeNotNull(
            gjeldendeStansOgOpphør().associate { it.fom to  it.vurdering},
            utlededeStansOgOpphør,
        ) { dato, vedtattStans, utledetStans ->
            if (utledetStans != null && vedtattStans != utledetStans) {
                GjeldendeStansEllerOpphør(
                    fom = dato,
                    opprettet = Instant.now(clock).truncatedTo(ChronoUnit.MILLIS),
                    vurdertIBehandling = behandlingId,
                    vurdering = utledetStans,
                )
            } else if (vedtattStans != null && utledetStans == null) {
                OpphevetStansEllerOpphør(
                    fom = dato,
                    opprettet = Instant.now(clock).truncatedTo(ChronoUnit.MILLIS),
                    vurdertIBehandling = behandlingId,
                )
            } else {
                null
            }
        }
        return StansOpphørGrunnlag(
            stansOgOpphør = stansOgOpphør + endringer.map { it.value }
        )
    }
}

sealed interface StansEllerOpphørVurdering {
    /* Stans/opphør gjelder fra og med [dato]. */
    val fom: LocalDate

    val vurdertIBehandling: BehandlingId

    val opprettet: Instant
}

data class GjeldendeStansEllerOpphør(
    override val fom: LocalDate,
    override val opprettet: Instant,
    override val vurdertIBehandling: BehandlingId,
    val vurdering: StansEllerOpphør,
) : StansEllerOpphørVurdering

data class OpphevetStansEllerOpphør(
    override val fom: LocalDate,
    override val vurdertIBehandling: BehandlingId,
    override val opprettet: Instant,
) : StansEllerOpphørVurdering

private fun <K, V1 : Any, V2 : Any, V3> mergeNotNull(
    map1: Map<K, V1>,
    map2: Map<K, V2>,
    f: (K, V1?, V2?) -> V3?,
): Map<K, V3> {
    val resultat = mutableMapOf<K, V3>()
    for (key in map1.keys + map2.keys) {
        val nyVerdi = f(key, map1[key], map2[key])
        if (nyVerdi != null) {
            resultat[key] = nyVerdi
        }
    }
    return resultat
}