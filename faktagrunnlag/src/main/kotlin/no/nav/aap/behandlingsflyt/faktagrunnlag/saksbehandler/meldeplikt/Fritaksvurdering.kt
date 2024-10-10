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
    val opprettetTid: LocalDateTime?
) {

    fun tidslinje(): Tidslinje<FritaksvurderingData> {
        return Tidslinje(
            listOf(Segment(Periode(fraDato, Tid.MAKS), FritaksvurderingData(harFritak, begrunnelse, opprettetTid)))
        )
    }

    data class FritaksvurderingData(
        val harFritak: Boolean,
        val begrunnelse: String,
        val opprettetTid: LocalDateTime?
    )
}