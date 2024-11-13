package no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.arbeidsevne

import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.arbeidsevne.flate.ArbeidsevneVurderingDto
import no.nav.aap.komponenter.type.Periode
import no.nav.aap.tidslinje.Segment
import no.nav.aap.tidslinje.StandardSammenslåere
import no.nav.aap.tidslinje.Tidslinje
import no.nav.aap.komponenter.verdityper.Prosent
import no.nav.aap.komponenter.verdityper.Tid
import java.time.LocalDate
import java.time.LocalDateTime

data class ArbeidsevneVurdering(
    val begrunnelse: String,
    val arbeidsevne: Prosent,
    val fraDato: LocalDate,
    val opprettetTid: LocalDateTime?
) {
    fun toDto(): ArbeidsevneVurderingDto {
        return ArbeidsevneVurderingDto(
            begrunnelse,
            opprettetTid ?: LocalDateTime.now(),
            arbeidsevne.prosentverdi(),
            fraDato
        )
    }

    fun tidslinje(): Tidslinje<ArbeidsevneVurderingData> {
        return Tidslinje(
            listOf(
                Segment(
                    Periode(fraDato, Tid.MAKS),
                    ArbeidsevneVurderingData(begrunnelse, arbeidsevne, opprettetTid)
                )
            )
        )
    }

    data class ArbeidsevneVurderingData(
        val begrunnelse: String,
        val arbeidsevne: Prosent,
        val opprettetTid: LocalDateTime?
    ) {
        fun toArbeidsevneVurdering(fraDato: LocalDate): ArbeidsevneVurdering {
            return ArbeidsevneVurdering(begrunnelse, arbeidsevne, fraDato, opprettetTid)
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
