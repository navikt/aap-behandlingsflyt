package no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.stansopphør

import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.temporal.ChronoUnit

data class StansOpphørGrunnlag(
    /** Denne skal fases ut, til fordel for [stansOpphørV2] og [stansOpphørVurderingerV2]. */
    val stansOgOpphør: Set<StansEllerOpphørVurdering>,

    /** Dette er alle potensielle stans og opphør, gitt opplysningene
     * vi har i denne behandlingen. Dette inkluderer stans og opphør lenger
     * frem enn vedtaksperiodene vår, så et stans og opphør her er ikke nødvendigvis
     * vedtatt.
     *
     * For å se hvilke stans og opphør som er vedtatt, så må du se i [stansOpphørVurderingerV2].
     *
     * Nullable frem til migrering er ferdig.
     */
    val stansOpphørV2: Map<LocalDate, StansEllerOpphør>?,

    /** Vurderinger av stans og opphør.
     *
     * Dette er vurderinger av stans og opphør innenfor vedtaksperioden, altså
     * stans og opphør som er vedtatt eller vil bli vedtatt i denne behandlingen.
     *
     * Det er full historikk på alle vurderingene som er gjort, så det kommer
     * egne «omgjørings»-vurderinger dersom et stans eller opphør ikke skal
     * gjelde lenger.
     *
     * Nullable frem til migrering er ferdig.
     */
    val stansOpphørVurderingerV2: Set<StansOpphørVurdering>?,
) {
    constructor(): this(emptySet(), null, null)
    constructor(stansOgOpphør: Set<StansEllerOpphørVurdering>): this(stansOgOpphør, null, null)
    fun gjeldendeStansOgOpphør(): Set<GjeldendeStansEllerOpphør> {
        return stansOgOpphør
            .groupBy { it.fom }
            .mapNotNull { (_, vedtak) -> vedtak.maxBy { it.opprettet } }
            .mapNotNull {
                when (it) {
                    is GjeldendeStansEllerOpphør -> it
                    is OpphevetStansEllerOpphør -> null
                }
            }
            .toSet()
    }

    fun stansOgOpphørMedHistorikk(): Map<LocalDate, List<StansEllerOpphørVurdering>> {
        return stansOgOpphør
            .groupBy { it.fom }
            .mapValues { (_, vurderinger) -> vurderinger.sortedByDescending { it.opprettet } }
    }


    fun utledNyttGrunnlag(
        utlededeStansOgOpphør: Map<LocalDate, StansEllerOpphør>,
        behandlingId: BehandlingId,
        clock: Clock = Clock.systemDefaultZone(),
    ): StansOpphørGrunnlag {
        val endringer = mergeNotNull(
            gjeldendeStansOgOpphør().associate { it.fom to it.vurdering },
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
            stansOgOpphør = this.stansOgOpphør + endringer.map { it.value },
            stansOpphørV2 = this.stansOpphørV2,
            stansOpphørVurderingerV2 = this.stansOpphørVurderingerV2,
        )
    }
}

sealed interface StansEllerOpphørVurdering {
    /** Stans/opphør gjelder fra og med [fom]. */
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