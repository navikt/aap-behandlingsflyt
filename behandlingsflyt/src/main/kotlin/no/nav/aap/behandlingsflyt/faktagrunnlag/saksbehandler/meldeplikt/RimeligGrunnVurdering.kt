package no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.meldeplikt

import no.nav.aap.komponenter.tidslinje.Segment
import no.nav.aap.komponenter.tidslinje.StandardSammenslåere
import no.nav.aap.komponenter.tidslinje.Tidslinje
import no.nav.aap.komponenter.type.Periode
import no.nav.aap.komponenter.verdityper.Tid
import java.time.LocalDate
import java.time.LocalDateTime

data class RimeligGrunnVurdering(
    val harRimeligGrunn: Boolean,
    val fraDato: LocalDate,
    val begrunnelse: String,
    val vurdertAv: String,
    val opprettetTid: LocalDateTime?
) {

    fun tidslinje(): Tidslinje<RimeligGrunnVurderingData> {
        return Tidslinje(
            listOf(Segment(Periode(fraDato, Tid.MAKS), RimeligGrunnVurderingData(harRimeligGrunn, begrunnelse, vurdertAv, opprettetTid)))
        )
    }

    data class RimeligGrunnVurderingData(
        val harRimeligGrunn: Boolean,
        val begrunnelse: String,
        val vurdertAv: String,
        val opprettetTid: LocalDateTime?,
    )

    companion object {
        fun List<RimeligGrunnVurdering>.tidslinje(): Tidslinje<RimeligGrunnVurderingData> {
            return sortedBy { it.fraDato }.fold(Tidslinje()) { acc, rimeligGrunnVurdering ->
                acc.kombiner(rimeligGrunnVurdering.tidslinje(), StandardSammenslåere.prioriterHøyreSideCrossJoin())
            }
        }
    }
}