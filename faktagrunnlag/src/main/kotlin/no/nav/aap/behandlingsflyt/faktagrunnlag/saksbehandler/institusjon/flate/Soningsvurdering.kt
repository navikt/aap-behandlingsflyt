package no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.institusjon.flate

import no.nav.aap.verdityper.dokument.JournalpostId

data class Soningsvurdering(
    val dokumenterBruktIVurdering: List<JournalpostId>,
    val soningUtenforFengsel: Boolean,
    val begrunnelseForSoningUtenforAnstalt: String? = null,
    val arbeidUtenforAnstalt: Boolean? = null,
    val begrunnelseForArbeidUtenforAnstalt: String? = null
)
