package no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.arbeidsevne

import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.arbeidsevne.flate.ArbeidsevneVurderingDto
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
    val opprettetTid: LocalDateTime?
) {
    fun toDto(): ArbeidsevneVurderingDto {
        return ArbeidsevneVurderingDto(begrunnelse, opprettetTid ?: LocalDateTime.now(), arbeidsevne.prosentverdi(), fraDato)
    }

    fun tidslinje(): Tidslinje<ArbeidsevneVurdering> {
        return Tidslinje(
            listOf(Segment(Periode(fraDato, Tid.MAKS), this))
        )
    }
}
