package no.nav.aap.behandlingsflyt.faktagrunnlag.dokument.arbeid.adapter

import no.nav.aap.behandlingsflyt.faktagrunnlag.dokument.arbeid.ArbeidIPeriode
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.dokumenter.Pliktkort
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.dokumenter.PliktkortV0
import no.nav.aap.komponenter.type.Periode
import no.nav.aap.komponenter.verdityper.TimerArbeid
import no.nav.aap.verdityper.dokument.JournalpostId

data class UbehandletPliktkort(val journalpostId: JournalpostId, val timerArbeidPerPeriode: Set<ArbeidIPeriode>) {
    companion object {
        fun fraKontrakt(pliktkort: Pliktkort, journalpostId: JournalpostId): UbehandletPliktkort {
            return when (pliktkort) {
                is PliktkortV0 -> UbehandletPliktkort(
                    journalpostId = journalpostId,
                    timerArbeidPerPeriode = pliktkort.timerArbeidPerPeriode.map {
                        ArbeidIPeriode(
                            periode = Periode(it.fraOgMedDato, it.tilOgMedDato),
                            timerArbeid = TimerArbeid(it.timerArbeid.toBigDecimal())
                        )
                    }.toSet()
                )
            }
        }
    }
}
