package no.nav.aap.verdityper

import java.math.BigDecimal

data class TimerArbeid(val antallTimer: BigDecimal) {
    init {
        require(antallTimer >= BigDecimal.ZERO) { "Kan ikke jobbe mindre enn 0 timer" }
    }
}
