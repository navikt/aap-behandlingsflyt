package no.nav.aap.behandlingsflyt.faktagrunnlag.arbeid

import no.nav.aap.behandlingsflyt.behandling.dokumenter.JournalpostId

data class Pliktkort(val journalpostId: JournalpostId, val timerArbeidPerPeriode: Set<ArbeidIPeriode>)
