package no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.arbeidsevne.flate

import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.arbeidsevne.ArbeidsevneVurdering
import no.nav.aap.verdityper.Prosent
import java.time.LocalDate
import java.time.LocalDateTime

data class FastsettArbeidsevneDto(
    val begrunnelse: String,
    val arbeidsevne: Int,
    val fraDato: LocalDate
) {
    fun toArbeidsevnevurdering() = ArbeidsevneVurdering(begrunnelse, Prosent(arbeidsevne), fraDato, LocalDateTime.now())
}
