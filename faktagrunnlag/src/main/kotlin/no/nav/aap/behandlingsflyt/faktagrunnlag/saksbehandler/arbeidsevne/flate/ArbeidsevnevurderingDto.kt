package no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.arbeidsevne.flate

import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.arbeidsevne.Arbeidsevnevurdering
import no.nav.aap.verdityper.Prosent
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime

data class ArbeidsevnevurderingDto(
    val begrunnelse: String,
    val arbeidsevne: Int,
    val fraDato: LocalDate
) {
    fun toArbeidsevnevurdering() = Arbeidsevnevurdering(begrunnelse, Prosent(arbeidsevne), fraDato, LocalDateTime.now())
}
