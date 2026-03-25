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
}
/**
 * Representerer arbeid i en Periode på et Meldekort.
 */
data class ArbeidIPeriode(val periode: Periode, val timerArbeid: TimerArbeid)