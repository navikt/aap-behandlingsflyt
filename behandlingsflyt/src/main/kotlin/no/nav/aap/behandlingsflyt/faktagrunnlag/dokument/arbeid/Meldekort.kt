package no.nav.aap.behandlingsflyt.faktagrunnlag.dokument.arbeid

import no.nav.aap.verdityper.dokument.JournalpostId

data class Meldekort(
    val journalpostId: JournalpostId,
    val timerArbeidPerPeriode: Set<ArbeidIPeriode>
)
