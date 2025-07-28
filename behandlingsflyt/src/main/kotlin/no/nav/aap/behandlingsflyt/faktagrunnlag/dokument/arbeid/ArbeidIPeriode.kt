package no.nav.aap.behandlingsflyt.faktagrunnlag.dokument.arbeid

import no.nav.aap.komponenter.type.Periode
import no.nav.aap.komponenter.verdityper.TimerArbeid

data class ArbeidIPeriode(val periode: Periode, val timerArbeid: TimerArbeid)
