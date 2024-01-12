package no.nav.aap.behandlingsflyt.dokument.mottak.pliktkort

import no.nav.aap.verdityper.dokument.JournalpostId
import no.nav.aap.behandlingsflyt.faktagrunnlag.arbeid.ArbeidIPeriode

data class UbehandletPliktkort(val journalpostId: JournalpostId, val timerArbeidPerPeriode: Set<ArbeidIPeriode>)
