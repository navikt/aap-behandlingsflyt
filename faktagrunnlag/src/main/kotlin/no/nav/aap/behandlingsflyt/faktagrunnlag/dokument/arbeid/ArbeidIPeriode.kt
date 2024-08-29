package no.nav.aap.behandlingsflyt.faktagrunnlag.dokument.arbeid

import no.nav.aap.komponenter.type.Periode
import no.nav.aap.verdityper.TimerArbeid
import java.math.BigDecimal
import java.math.RoundingMode

data class ArbeidIPeriode(val periode: Periode, val timerArbeid: TimerArbeid) {
    fun arbeidPerDag(): TimerArbeid {
        val antallDager = periode.antallDager()

        return TimerArbeid(timerArbeid.antallTimer.divide(BigDecimal(antallDager), 3, RoundingMode.HALF_UP))
    }
}
