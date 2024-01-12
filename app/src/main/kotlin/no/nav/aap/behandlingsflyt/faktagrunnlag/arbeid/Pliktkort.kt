package no.nav.aap.behandlingsflyt.faktagrunnlag.arbeid

import no.nav.aap.verdityper.dokument.JournalpostId

data class Pliktkort(val journalpostId: JournalpostId, val timerArbeidPerPeriode: Set<ArbeidIPeriode>)
