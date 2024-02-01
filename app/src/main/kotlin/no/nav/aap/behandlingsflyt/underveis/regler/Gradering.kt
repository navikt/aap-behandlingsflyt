package no.nav.aap.behandlingsflyt.underveis.regler

import no.nav.aap.verdityper.Prosent
import no.nav.aap.verdityper.TimerArbeid

data class Gradering(val totaltAntallTimer: TimerArbeid, val andelArbeid: Prosent, internal val gradering: Prosent)