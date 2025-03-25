package no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.underveis

import no.nav.aap.komponenter.verdityper.Prosent
import no.nav.aap.komponenter.verdityper.TimerArbeid
import java.time.LocalDate

data class ArbeidsGradering(
    val totaltAntallTimer: TimerArbeid,
    val andelArbeid: Prosent,
    val fastsattArbeidsevne: Prosent,
    val gradering: Prosent,
    val opplysningerMottatt: LocalDate?,
)