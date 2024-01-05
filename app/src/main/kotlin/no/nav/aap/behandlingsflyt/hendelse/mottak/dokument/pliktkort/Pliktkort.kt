package no.nav.aap.behandlingsflyt.hendelse.mottak.dokument.pliktkort

import no.nav.aap.behandlingsflyt.verdityper.Periode
import no.nav.aap.behandlingsflyt.faktagrunnlag.arbeid.ArbeidIPeriode
import no.nav.aap.behandlingsflyt.hendelse.mottak.dokument.PeriodisertData

class Pliktkort(val timerArbeidPerPeriode: Set<ArbeidIPeriode>) : PeriodisertData {
    override fun periode(): Periode {
        val fom = timerArbeidPerPeriode.minOfOrNull { it.periode.fom }
        val tom = timerArbeidPerPeriode.maxOfOrNull { it.periode.tom }
        if (fom == null || tom == null) {
            throw IllegalStateException()
        }

        return Periode(fom, tom)
    }
}