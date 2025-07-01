package no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.arbeidsevne

import no.nav.aap.komponenter.tidslinje.Segment
import no.nav.aap.komponenter.tidslinje.StandardSammenslåere
import no.nav.aap.komponenter.tidslinje.Tidslinje
import no.nav.aap.komponenter.type.Periode
import no.nav.aap.komponenter.verdityper.Prosent
import no.nav.aap.komponenter.verdityper.Tid
import java.time.LocalDate
import java.time.LocalDateTime

data class ArbeidsevneVurdering(
    val begrunnelse: String,
    val arbeidsevne: Prosent,
    val fraDato: LocalDate,
    val opprettetTid: LocalDateTime? = null,
    val vurdertAv: String,
) {
    fun tidslinje(): Tidslinje<ArbeidsevneVurderingData> {
        return Tidslinje(
            listOf(
                Segment(
                    Periode(fraDato, Tid.MAKS),
                    ArbeidsevneVurderingData(begrunnelse, arbeidsevne, opprettetTid, vurdertAv)
                )
            )
        )
    }

    data class ArbeidsevneVurderingData(
        val begrunnelse: String,
        val arbeidsevne: Prosent,
        val opprettetTid: LocalDateTime?,
        val vurdertAv: String,
    ) {
        fun toArbeidsevneVurdering(fraDato: LocalDate): ArbeidsevneVurdering {
            return ArbeidsevneVurdering(
                begrunnelse = begrunnelse,
                arbeidsevne = arbeidsevne,
                fraDato = fraDato,
                opprettetTid = opprettetTid,
                vurdertAv = vurdertAv
            )
        }
    }

    companion object {
        fun List<ArbeidsevneVurdering>.tidslinje(): Tidslinje<ArbeidsevneVurderingData> {
            return sortedBy { it.fraDato }.fold(Tidslinje()) { acc, arbeidsevneVurdering ->
                acc.kombiner(arbeidsevneVurdering.tidslinje(), StandardSammenslåere.prioriterHøyreSideCrossJoin())
            }
        }
    }
}
