package no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.underveis

import no.nav.aap.komponenter.verdityper.Prosent
import no.nav.aap.komponenter.verdityper.TimerArbeid
import java.time.LocalDate

/**
 * @param andelArbeid Hvor mye arbeid, hvor 100% svarer til full stilling.
 */
data class ArbeidsGradering(
    val totaltAntallTimer: TimerArbeid,
    val andelArbeid: Prosent,
    val fastsattArbeidsevne: Prosent,
    val gradering: Prosent,
    val opplysningerMottatt: LocalDate?,
)