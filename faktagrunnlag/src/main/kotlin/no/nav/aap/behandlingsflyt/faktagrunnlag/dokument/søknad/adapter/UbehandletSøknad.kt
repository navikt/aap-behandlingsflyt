package no.nav.aap.behandlingsflyt.faktagrunnlag.dokument.søknad.adapter

import no.nav.aap.verdityper.Periode
import no.nav.aap.verdityper.dokument.JournalpostId

class UbehandletSøknad(
    val journalpostId: JournalpostId,
    val periode: Periode,
    val student: Boolean,
    val harYrkesskade: Boolean
) {
}