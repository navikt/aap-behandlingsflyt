package no.nav.aap.behandlingsflyt.avklaringsbehov.løser.sykdom

import no.nav.aap.behandlingsflyt.behandling.dokumenter.JournalpostId

data class SykepengerVurdering(
    val begrunnelse: String,
    val dokumenterBruktIVurdering: List<JournalpostId>,
    val harRettPå: Boolean?
)
