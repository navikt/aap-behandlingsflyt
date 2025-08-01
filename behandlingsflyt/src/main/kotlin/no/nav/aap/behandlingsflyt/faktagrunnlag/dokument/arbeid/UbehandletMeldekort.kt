package no.nav.aap.behandlingsflyt.faktagrunnlag.dokument.arbeid

import no.nav.aap.behandlingsflyt.kontrakt.hendelse.dokumenter.Meldekort
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.dokumenter.MeldekortV0
import no.nav.aap.komponenter.type.Periode
import no.nav.aap.komponenter.verdityper.TimerArbeid
import no.nav.aap.verdityper.dokument.JournalpostId
import java.time.LocalDateTime

data class UbehandletMeldekort(
    val journalpostId: JournalpostId,
    val timerArbeidPerPeriode: Set<ArbeidIPeriode>,
    val mottattTidspunkt: LocalDateTime
) {
    companion object {
        fun fraKontrakt(
            meldekort: Meldekort,
            journalpostId: JournalpostId,
            mottattTidspunkt: LocalDateTime
        ): UbehandletMeldekort {
            return when (meldekort) {
                is MeldekortV0 -> UbehandletMeldekort(
                    journalpostId = journalpostId,
                    timerArbeidPerPeriode = meldekort.timerArbeidPerPeriode.map {
                        ArbeidIPeriode(
                            periode = Periode(it.fraOgMedDato, it.tilOgMedDato),
                            timerArbeid = TimerArbeid(it.timerArbeid.toBigDecimal())
                        )
                    }.toSet(),
                    mottattTidspunkt = mottattTidspunkt
                )
            }
        }
    }
}
