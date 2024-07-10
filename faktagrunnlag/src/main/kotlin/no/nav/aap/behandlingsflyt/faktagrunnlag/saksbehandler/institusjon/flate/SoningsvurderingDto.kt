package no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.institusjon.flate

import no.nav.aap.verdityper.dokument.JournalpostId

data class SoningsvurderingDto(
    val dokumenterBruktIVurdering: List<JournalpostId>,
    val soningUtenforFengsel: Boolean,
    val begrunnelseForSoningUtenforAnstalt: String? = null,
    val arbeidUtenforAnstalt: Boolean? = null,
    val begrunnelseForArbeidUtenforAnstalt: String? = null
) {

    fun tilDomeneobjekt() = Soningsvurdering(
        dokumenterBruktIVurdering = listOf(),
        begrunnelseForArbeidUtenforAnstalt = begrunnelseForArbeidUtenforAnstalt,
        arbeidUtenforAnstalt = arbeidUtenforAnstalt,
        soningUtenforFengsel = soningUtenforFengsel,
        begrunnelseForSoningUtenforAnstalt = begrunnelseForSoningUtenforAnstalt,
    )

    companion object {
        fun toDto(soningsvurdering: Soningsvurdering?) = if (soningsvurdering != null) SoningsvurderingDto(
            dokumenterBruktIVurdering = listOf(),
            arbeidUtenforAnstalt = soningsvurdering.arbeidUtenforAnstalt,
            begrunnelseForArbeidUtenforAnstalt = soningsvurdering.begrunnelseForArbeidUtenforAnstalt,
            soningUtenforFengsel = soningsvurdering.soningUtenforFengsel,
            begrunnelseForSoningUtenforAnstalt = soningsvurdering.begrunnelseForSoningUtenforAnstalt,
        ) else null
    }
}
