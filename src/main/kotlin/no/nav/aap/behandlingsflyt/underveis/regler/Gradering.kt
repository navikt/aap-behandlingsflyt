package no.nav.aap.behandlingsflyt.underveis.regler

import no.nav.aap.behandlingsflyt.beregning.Prosent

data class Gradering(val totaltAntallTimer: TimerArbeid, val prosent: Prosent) {

}
