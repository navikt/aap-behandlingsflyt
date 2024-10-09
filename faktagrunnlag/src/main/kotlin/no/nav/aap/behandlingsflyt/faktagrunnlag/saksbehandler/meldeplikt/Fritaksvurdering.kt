package no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.meldeplikt

import no.nav.aap.komponenter.type.Periode
import no.nav.aap.tidslinje.Segment
import no.nav.aap.tidslinje.Tidslinje
import no.nav.aap.verdityper.Tid
import java.time.LocalDate
import java.time.LocalDateTime

data class Fritaksvurdering(
    val harFritak: Boolean,
    val fraDato: LocalDate,
    val begrunnelse: String,
    val opprettetTid: LocalDateTime
) {

    fun tidslinje(): Tidslinje<Fritaksvurdering> {
        return Tidslinje(
            listOf(Segment(Periode(fraDato, Tid.MAKS), this))
        )
    }

    override fun equals(other: Any?): Boolean {
        return this === other || (other is Fritaksvurdering && this.valueEquals(other))
    }

    private fun valueEquals(other: Fritaksvurdering): Boolean {
        return harFritak == other.harFritak && begrunnelse == other.begrunnelse
    }

    override fun hashCode(): Int {
        return 31 * harFritak.hashCode() + begrunnelse.hashCode()
    }
}