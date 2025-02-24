package no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.underveis

import no.nav.aap.komponenter.verdityper.Prosent
import no.nav.aap.komponenter.verdityper.TimerArbeid

data class ArbeidsGradering(
    val totaltAntallTimer: TimerArbeid,
    val andelArbeid: Prosent,
    val fastsattArbeidsevne: Prosent,
    val gradering: Prosent
)