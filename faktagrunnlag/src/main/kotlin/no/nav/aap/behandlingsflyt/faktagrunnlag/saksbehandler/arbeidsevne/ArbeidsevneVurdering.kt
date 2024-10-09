package no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.arbeidsevne

import no.nav.aap.komponenter.type.Periode
import no.nav.aap.tidslinje.Segment
import no.nav.aap.tidslinje.Tidslinje
import no.nav.aap.verdityper.Prosent
import no.nav.aap.verdityper.Tid
import java.time.LocalDate
import java.time.LocalDateTime

data class ArbeidsevneVurdering(
    val begrunnelse: String,
    val arbeidsevne: Prosent,
    val fraDato: LocalDate,
    val opprettetTid: LocalDateTime
) {
    fun tidslinje(): Tidslinje<ArbeidsevneVurdering> {
        return Tidslinje(
            listOf(Segment(Periode(fraDato, Tid.MAKS), this))
        )
    }

    override fun equals(other: Any?): Boolean {
        return this === other || (other is ArbeidsevneVurdering && this.valueEquals(other))
    }

    private fun valueEquals(other: ArbeidsevneVurdering): Boolean {
        return begrunnelse == other.begrunnelse && arbeidsevne == other.arbeidsevne
    }

    override fun hashCode(): Int {
        return 31 * begrunnelse.hashCode() + arbeidsevne.hashCode()
    }
}
