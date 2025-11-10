package no.nav.aap.behandlingsflyt.utils

import no.nav.aap.behandlingsflyt.behandling.foreslåvedtak.ForeslåVedtakData
import no.nav.aap.behandlingsflyt.behandling.foreslåvedtak.UnderveisPeriodeInfo
import no.nav.aap.behandlingsflyt.behandling.foreslåvedtak.UnderveisPeriodeInfo.Companion.tilForeslåVedtakData
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.underveis.UnderveisGrunnlag
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Utfall
import no.nav.aap.komponenter.tidslinje.Segment
import no.nav.aap.komponenter.tidslinje.Tidslinje
import java.time.Instant
import java.time.ZoneId

fun UnderveisGrunnlag.tilForeslåVedtakDataTidslinje(): Tidslinje<ForeslåVedtakData> {
    val underveisPerioder =
        this.perioder.map {
            UnderveisPeriodeInfo(
                periode = it.periode,
                utfall = it.utfall,
                rettighetsType = it.rettighetsType,
                underveisÅrsak = it.avslagsårsak
            )
        }
    return underveisPerioder
        .map {
            Segment(it.periode, it.tilForeslåVedtakData())
        }.let(::Tidslinje)
        .komprimer()
}

class UtfallOppfyltUtils {
    fun alleEventuellePerioderEtterOpprettetTidspunktHarUtfallIkkeOppfylt(
        opprettetTidspunkt: Instant,
        underveisGrunnlag: UnderveisGrunnlag
    ): Boolean {
        val opprettetDato = opprettetTidspunkt
            .atZone(ZoneId.of("Europe/Oslo"))
            .toLocalDate()
        val tidslinje = underveisGrunnlag.tilForeslåVedtakDataTidslinje()
        return tidslinje.segmenter()
            .filter { it.periode.tom.isAfter(opprettetDato)}
            .all {it.verdi.utfall == Utfall.IKKE_OPPFYLT } }
}