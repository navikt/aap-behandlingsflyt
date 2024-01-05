package no.nav.aap.behandlingsflyt.faktagrunnlag.arbeid

import no.nav.aap.behandlingsflyt.verdityper.Periode
import no.nav.aap.behandlingsflyt.underveis.regler.TimerArbeid
import java.math.BigDecimal
import java.math.RoundingMode

data class ArbeidIPeriode(val periode: Periode, val timerArbeid: TimerArbeid) {
    fun arbeidPerDag(): TimerArbeid {
        val antallDager = periode.antallDager()

        return TimerArbeid(timerArbeid.antallTimer.divide(BigDecimal(antallDager), 3, RoundingMode.HALF_UP))
    }
}
