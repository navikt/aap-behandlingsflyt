package no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.institusjon.flate

import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.institusjon.Soningsvurdering
import no.nav.aap.komponenter.type.Periode
import no.nav.aap.verdityper.dokument.JournalpostId
import java.time.LocalDate

data class SoningsvurderingDto(
    val dokumenterBruktIVurdering: List<JournalpostId>,
    val soningUtenforFengsel: Boolean,
    val begrunnelse: String?,
    val arbeidUtenforAnstalt: Boolean? = null,
    val førsteArbeidsdag: LocalDate? = null,
    val periode: Periode) {

    fun tilDomeneobjekt() = Soningsvurdering(
        dokumenterBruktIVurdering = listOf(),
        soningUtenforFengsel = soningUtenforFengsel,
        begrunnelse = begrunnelse,
        arbeidUtenforAnstalt = arbeidUtenforAnstalt,
        førsteArbeidsdag = førsteArbeidsdag,
        periode = periode
    )

    companion object {
        fun toDto(soningsvurdering: Soningsvurdering?) = if (soningsvurdering != null) SoningsvurderingDto(
            dokumenterBruktIVurdering = soningsvurdering.dokumenterBruktIVurdering,
            arbeidUtenforAnstalt = soningsvurdering.arbeidUtenforAnstalt,
            begrunnelse = soningsvurdering.begrunnelse,
            soningUtenforFengsel = soningsvurdering.soningUtenforFengsel,
            førsteArbeidsdag = soningsvurdering.førsteArbeidsdag,
            periode = soningsvurdering.periode
        ) else null
    }
}
