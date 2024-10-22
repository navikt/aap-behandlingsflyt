package no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.institusjon.flate

import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.institusjon.Soningsvurdering
import java.time.LocalDate

data class SoningsvurderingDto(
    val skalOpphore: Boolean,
    val begrunnelse: String,
    val fraDato: LocalDate
) {
    fun tilDomeneobjekt() = Soningsvurdering(
        skalOpphøre = skalOpphore,
        begrunnelse = begrunnelse,
        fraDato = fraDato
    )
}
