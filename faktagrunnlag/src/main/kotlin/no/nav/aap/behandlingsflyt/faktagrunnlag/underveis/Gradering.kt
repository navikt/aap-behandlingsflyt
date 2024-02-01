package no.nav.aap.behandlingsflyt.faktagrunnlag.underveis

import no.nav.aap.verdityper.Prosent
import no.nav.aap.verdityper.TimerArbeid

data class Gradering(val totaltAntallTimer: TimerArbeid, val andelArbeid: Prosent, val gradering: Prosent)