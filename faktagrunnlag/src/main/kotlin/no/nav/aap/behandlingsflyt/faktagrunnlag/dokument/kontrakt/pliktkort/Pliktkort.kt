package no.nav.aap.behandlingsflyt.faktagrunnlag.dokument.kontrakt.pliktkort

import no.nav.aap.behandlingsflyt.faktagrunnlag.dokument.PeriodisertData
import no.nav.aap.behandlingsflyt.faktagrunnlag.dokument.arbeid.ArbeidIPeriode
import no.nav.aap.komponenter.type.Periode

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