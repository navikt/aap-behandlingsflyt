package no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.meldeplikt

import no.nav.aap.komponenter.tidslinje.Segment
import no.nav.aap.komponenter.tidslinje.StandardSammenslåere
import no.nav.aap.komponenter.tidslinje.Tidslinje
import no.nav.aap.komponenter.type.Periode
import no.nav.aap.komponenter.verdityper.Tid
import java.time.LocalDate
import java.time.LocalDateTime

data class Fritaksvurdering(
    val harFritak: Boolean,
    val fraDato: LocalDate,
    val begrunnelse: String,
    val vurdertAv: String,
    val opprettetTid: LocalDateTime?
) {

    fun tidslinje(): Tidslinje<FritaksvurderingData> {
        return Tidslinje(
            listOf(Segment(Periode(fraDato, Tid.MAKS), FritaksvurderingData(harFritak, begrunnelse, vurdertAv, opprettetTid)))
        )
    }

    data class FritaksvurderingData(
        val harFritak: Boolean,
        val begrunnelse: String,
        val vurdertAv: String,
        val opprettetTid: LocalDateTime?,
    )

    companion object {
        fun List<Fritaksvurdering>.tidslinje(): Tidslinje<FritaksvurderingData> {
            return sortedBy { it.fraDato }.fold(Tidslinje()) { acc, fritaksvurdering ->
                acc.kombiner(fritaksvurdering.tidslinje(), StandardSammenslåere.prioriterHøyreSideCrossJoin())
            }
        }
    }
}