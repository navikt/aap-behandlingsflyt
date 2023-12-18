package no.nav.aap.behandlingsflyt.faktagrunnlag.arbeid

import no.nav.aap.behandlingsflyt.Periode
import no.nav.aap.behandlingsflyt.underveis.regler.TimerArbeid

data class ArbeidIPeriode(val periode: Periode, val timerArbeid: TimerArbeid)
