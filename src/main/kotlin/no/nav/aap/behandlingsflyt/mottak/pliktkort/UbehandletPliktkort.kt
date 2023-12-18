package no.nav.aap.behandlingsflyt.mottak.pliktkort

import no.nav.aap.behandlingsflyt.behandling.dokumenter.JournalpostId

data class UbehandletPliktkort(val journalpostId: JournalpostId, val timerArbeidPerPeriode: Set<ArbeidIPeriode>)
