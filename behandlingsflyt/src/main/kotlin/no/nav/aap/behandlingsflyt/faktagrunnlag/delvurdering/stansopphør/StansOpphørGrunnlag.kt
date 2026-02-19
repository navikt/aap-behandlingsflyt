package no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.stansopphør

import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import java.util.Objects

data class StansOpphørGrunnlag(
    val stansOgOpphør: Set<StansEllerOpphørVurdering>
) {
    fun gjeldendeStansOgOpphør(): Set<GjeldendeStansEllerOpphør> {
        return stansOgOpphør
            .groupBy { it.dato }
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
            gjeldendeStansOgOpphør().associate { it.dato to  it.vurdering},
            utlededeStansOgOpphør,
        ) { dato, vedtattStans, utledetStans ->
            if (vedtattStans == null && utledetStans != null) {
                GjeldendeStansEllerOpphør(
                    dato = dato,
                    opprettet = Instant.now(clock),
                    vurdertIBehandling = behandlingId,
                    vurdering = utledetStans,
                )
            } else if (vedtattStans != null && utledetStans != null && vedtattStans != utledetStans) {
                GjeldendeStansEllerOpphør(
                    dato = dato,
                    opprettet = Instant.now(clock),
                    vurdertIBehandling = behandlingId,
                    vurdering = utledetStans,
                )
            } else if (vedtattStans != null && utledetStans == null) {
                OpphevetStansEllerOpphør(
                    dato = dato,
                    opprettet = Instant.now(clock),
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
    val dato: LocalDate

    val vurdertIBehandling: BehandlingId

    val opprettet: Instant
}

data class GjeldendeStansEllerOpphør(
    override val dato: LocalDate,
    override val opprettet: Instant,
    override val vurdertIBehandling: BehandlingId,
    val vurdering: StansEllerOpphør,
) : StansEllerOpphørVurdering {
    override fun equals(other: Any?): Boolean {
        return ( other is GjeldendeStansEllerOpphør
            && other.vurdertIBehandling == vurdertIBehandling
            && other.vurdering == vurdering
            && other.dato == dato
            && other.opprettet.truncatedTo(ChronoUnit.MILLIS) == other.opprettet.truncatedTo(ChronoUnit.MILLIS))
    }

    override fun hashCode(): Int {
        return Objects.hash(dato, opprettet.truncatedTo(ChronoUnit.MILLIS), vurdertIBehandling, vurdering)
    }
}

data class OpphevetStansEllerOpphør(
    override val dato: LocalDate,
    override val vurdertIBehandling: BehandlingId,
    override val opprettet: Instant,
) : StansEllerOpphørVurdering {
    override fun equals(other: Any?): Boolean {
        return ( other is OpphevetStansEllerOpphør
                && other.vurdertIBehandling == vurdertIBehandling
                && other.dato == dato
                && other.opprettet.truncatedTo(ChronoUnit.MILLIS) == other.opprettet.truncatedTo(ChronoUnit.MILLIS))
    }

    override fun hashCode(): Int {
        return Objects.hash(dato, opprettet.truncatedTo(ChronoUnit.MILLIS), vurdertIBehandling)
    }
}

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