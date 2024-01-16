package no.nav.aap.behandlingsflyt.faktagrunnlag.sykdom

import no.nav.aap.verdityper.dokument.JournalpostId

data class SykepengerVurdering(
    val begrunnelse: String,
    val dokumenterBruktIVurdering: List<JournalpostId>,
    val harRettPå: Boolean?
)
