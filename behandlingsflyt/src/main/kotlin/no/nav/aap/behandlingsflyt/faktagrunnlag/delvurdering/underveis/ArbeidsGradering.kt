package no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.underveis

import no.nav.aap.komponenter.verdityper.Prosent
import no.nav.aap.komponenter.verdityper.TimerArbeid
import java.time.LocalDate

/**
 * @param andelArbeid Hvor mye arbeid, hvor 100% svarer til full stilling.
 * @param totaltAntallTimer Antall timer totalt i den aktuelle perioden. (TODO: i meldeperioden eller perioden)
 * @param fastsattArbeidsevne Om 11-23 andre ledd-steget er svart på, er dette brukerens fastsatte arbeidsevne. Defaulter til 0.
 * @param gradering Reduksjon basert på timer arbeid. 100% - max(andelArbeid, fastsattArbeidsevne). Er 0 hvis søker har jobbet mer enn grenseverdi eller ikke har levert timer.
 * @param opplysningerMottatt Hvilken dato vi fikk opplysninger om arbeid.
 */
data class ArbeidsGradering(
    val totaltAntallTimer: TimerArbeid,
    val andelArbeid: Prosent,
    val fastsattArbeidsevne: Prosent,
    val gradering: Prosent,
    val opplysningerMottatt: LocalDate?,
)