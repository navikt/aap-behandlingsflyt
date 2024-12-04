package no.nav.aap.behandlingsflyt.faktagrunnlag.dokument.arbeid.adapter

import no.nav.aap.behandlingsflyt.faktagrunnlag.dokument.arbeid.ArbeidIPeriode
import no.nav.aap.verdityper.dokument.JournalpostId

data class UbehandletPliktkort(val journalpostId: JournalpostId, val timerArbeidPerPeriode: Set<ArbeidIPeriode>)
