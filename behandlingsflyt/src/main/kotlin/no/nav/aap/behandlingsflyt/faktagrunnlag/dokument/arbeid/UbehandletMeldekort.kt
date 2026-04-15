package no.nav.aap.behandlingsflyt.faktagrunnlag.dokument.arbeid

import no.nav.aap.behandlingsflyt.kontrakt.hendelse.dokumenter.Meldekort
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.dokumenter.MeldekortFraSaksbehandlerV0
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.dokumenter.MeldekortV0
import no.nav.aap.komponenter.type.Periode
import no.nav.aap.komponenter.verdityper.TimerArbeid
import no.nav.aap.verdityper.dokument.JournalpostId
import java.time.LocalDateTime

data class UbehandletMeldekort(
    val journalpostId: JournalpostId,
    val timerArbeidPerPeriode: Set<ArbeidIPeriode>,
    val mottattTidspunkt: LocalDateTime,
    val harDuArbeidet: Boolean?,
    val digitalisertAvPostmottak: Boolean?
) {
    companion object {
        fun fraKontrakt(
            meldekort: Meldekort,
            journalpostId: JournalpostId,
            mottattTidspunkt: LocalDateTime,
            digitalisertAvPostmottak: Boolean?
        ): UbehandletMeldekort {
            val (harDuArbeidet, timerArbeidPerPeriode) = when (meldekort) {
                is MeldekortV0 -> meldekort.harDuArbeidet to meldekort.timerArbeidPerPeriode
                is MeldekortFraSaksbehandlerV0 -> meldekort.harDuArbeidet to meldekort.timerArbeidPerPeriode
            }
            return UbehandletMeldekort(
                journalpostId = journalpostId,
                timerArbeidPerPeriode = timerArbeidPerPeriode.map {
                    ArbeidIPeriode(
                        periode = Periode(it.fraOgMedDato, it.tilOgMedDato),
                        timerArbeid = TimerArbeid(it.timerArbeid.toBigDecimal())
                    )
                }.toSet(),
                mottattTidspunkt = mottattTidspunkt,
                harDuArbeidet = harDuArbeidet,
                digitalisertAvPostmottak = digitalisertAvPostmottak
            )
        }
    }
}
