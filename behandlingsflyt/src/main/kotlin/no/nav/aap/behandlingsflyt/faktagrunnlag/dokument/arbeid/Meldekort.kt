package no.nav.aap.behandlingsflyt.faktagrunnlag.dokument.arbeid

import no.nav.aap.behandlingsflyt.kontrakt.hendelse.InnsendingReferanse
import no.nav.aap.komponenter.tidslinje.Segment
import no.nav.aap.komponenter.tidslinje.Tidslinje
import no.nav.aap.komponenter.type.Periode
import no.nav.aap.komponenter.verdityper.TimerArbeid
import java.time.LocalDateTime

data class Meldekort(
    val referanse: InnsendingReferanse,
    val timerArbeidPerPeriode: Set<ArbeidIPeriode>,
    val mottattTidspunkt: LocalDateTime,
    val begrunnelse: String? = null,
    val opprettetAv: String? = null,
) {
    fun somTidslinje(): Tidslinje<Pair<TimerArbeid, Int>> {
        return Tidslinje(timerArbeidPerPeriode.map {
            Segment(it.periode, it.timerArbeid to it.periode.antallDager())
        }.toList())
    }

    /**
    @return Tidsperioden meldekortet inneholder arbeidstimer for.
    Merk at det kan være dager i perioden meldekortet gjelder for som det ikke er rapportert timer på.
    @param meldekort -
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