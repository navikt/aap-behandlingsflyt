package no.nav.aap.behandlingsflyt.faktagrunnlag.dokument.arbeid

import no.nav.aap.behandlingsflyt.kontrakt.hendelse.dokumenter.MeldekortV0
import no.nav.aap.komponenter.tidslinje.Segment
import no.nav.aap.komponenter.tidslinje.Tidslinje
import no.nav.aap.komponenter.type.Periode
import no.nav.aap.komponenter.verdityper.TimerArbeid
import no.nav.aap.verdityper.dokument.JournalpostId
import java.time.LocalDateTime
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.dokumenter.Meldekort as KontraktMeldekort

data class Meldekort(
    val journalpostId: JournalpostId,
    val timerArbeidPerPeriode: Set<ArbeidIPeriode>,
    val mottattTidspunkt: LocalDateTime,
    val opprettetTidspunkt: LocalDateTime,
) {
    companion object {
        fun fraKontrakt(
            journalpostId: JournalpostId,
            mottattTidspunkt: LocalDateTime,
            opprettetTidspunkt: LocalDateTime,
            meldekort: KontraktMeldekort
        ): Meldekort {
            return when (meldekort) {
                is MeldekortV0 -> Meldekort(
                    journalpostId = journalpostId,
                    timerArbeidPerPeriode = meldekort.timerArbeidPerPeriode.map {
                        ArbeidIPeriode(
                            periode = Periode(it.fraOgMedDato, it.tilOgMedDato),
                            timerArbeid = TimerArbeid(it.timerArbeid.toBigDecimal())
                        )
                    }.toSet(),
                    mottattTidspunkt = mottattTidspunkt,
                    opprettetTidspunkt = opprettetTidspunkt
                )
            }
        }
    }

    fun somTidslinje(): Tidslinje<Pair<TimerArbeid, Int>> {
        return Tidslinje(timerArbeidPerPeriode.map {
            Segment(it.periode, it.timerArbeid to it.periode.antallDager())
        }.toList())
    }

    /**
     * @return Tidsperioden meldekortet inneholder arbeidstimer for.
     * Merk at det kan være dager i perioden meldekortet gjelder for som det ikke er rapportert timer på.
     **/
    fun arbeidsperiode(): Periode? {
        if (timerArbeidPerPeriode.isEmpty()) {
            return null
        }
        val arbeidPerioder = timerArbeidPerPeriode.map { it.periode }
        val arbeidsPerioderStart = arbeidPerioder.minBy { it.fom }.fom
        val arbeidsPerioderSlutt = arbeidPerioder.maxBy { it.tom }.tom

        return Periode(arbeidsPerioderStart, arbeidsPerioderSlutt)
    }
}

/**
 * Representerer arbeid i en Periode på et Meldekort.
 */
data class ArbeidIPeriode(val periode: Periode, val timerArbeid: TimerArbeid)