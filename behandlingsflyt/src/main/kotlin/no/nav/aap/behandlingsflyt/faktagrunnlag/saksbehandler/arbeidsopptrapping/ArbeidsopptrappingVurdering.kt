package no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.arbeidsopptrapping

import no.nav.aap.komponenter.tidslinje.Segment
import no.nav.aap.komponenter.tidslinje.StandardSammenslåere
import no.nav.aap.komponenter.tidslinje.Tidslinje
import no.nav.aap.komponenter.type.Periode
import no.nav.aap.komponenter.verdityper.Tid
import java.time.LocalDate
import java.time.LocalDateTime

data class ArbeidsopptrappingVurdering(
    val begrunnelse: String,
    val fraDato: LocalDate,
    val reellMulighetTilOpptrapping: Boolean,
    val rettPaaAAPIOpptrapping: Boolean,
    val vurdertAv: String,
    val opprettetTid: LocalDateTime?
) {
    fun tidslinje(): Tidslinje<ArbeidsopptrappingVurderingData> {
        return Tidslinje(
            listOf(
                Segment(
                    Periode(fraDato, Tid.MAKS),
                    ArbeidsopptrappingVurderingData(
                        begrunnelse,
                        reellMulighetTilOpptrapping,
                        rettPaaAAPIOpptrapping,
                        vurdertAv,
                        opprettetTid
                    )
                )
            )
        )
    }

    data class ArbeidsopptrappingVurderingData(
        val begrunnelse: String,
        val reellMulighetTilOpptrapping: Boolean,
        val rettPaaAAPIOpptrapping: Boolean,
        val vurdertAv: String,
        val opprettetTid: LocalDateTime?
    )

    companion object {
        fun List<ArbeidsopptrappingVurdering>.tidslinje(): Tidslinje<ArbeidsopptrappingVurderingData> {
            return sortedBy { it.fraDato }.fold(Tidslinje()) { acc, arbeidsopptrappingvurdering ->
                acc.kombiner(
                    arbeidsopptrappingvurdering.tidslinje(),
                    StandardSammenslåere.prioriterHøyreSideCrossJoin()
                )
            }
        }
    }
}