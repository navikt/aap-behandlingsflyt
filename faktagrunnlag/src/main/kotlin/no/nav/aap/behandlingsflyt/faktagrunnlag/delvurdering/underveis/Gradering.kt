package no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.underveis

import no.nav.aap.verdityper.Prosent
import no.nav.aap.verdityper.TimerArbeid

data class Gradering(
    val totaltAntallTimer: TimerArbeid,
    val andelArbeid: Prosent,
    val fastsattArbeidsevne: Prosent,
    val gradering: Prosent
)