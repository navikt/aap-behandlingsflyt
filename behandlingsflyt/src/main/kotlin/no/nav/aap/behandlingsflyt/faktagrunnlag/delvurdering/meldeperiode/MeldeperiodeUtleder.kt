package no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.meldeperiode

import no.nav.aap.behandlingsflyt.behandling.underveis.regler.UtledMeldeperiodeRegel.Companion.MELDEPERIODE_LENGDE
import no.nav.aap.komponenter.type.Periode
import java.time.DayOfWeek
import java.time.LocalDate

object MeldeperiodeUtleder {

    fun utledMeldeperiode(
        fastsattDag: LocalDate?,
        aktuellTidsperiode: Periode
    ): List<Periode> {
        val førsteFastsatteDag = if (fastsattDag == null)
            generateSequence(aktuellTidsperiode.fom) { it.minusDays(1) }
                .first { it.dayOfWeek == DayOfWeek.MONDAY }
        else
            generateSequence(fastsattDag) { it.minusDays(MELDEPERIODE_LENGDE) }
                .first { it <= aktuellTidsperiode.fom }

        return generateSequence(førsteFastsatteDag) { it.plusDays(MELDEPERIODE_LENGDE) }
            .takeWhile { it <= aktuellTidsperiode.tom }
            .map { Periode(it, it.plusDays(MELDEPERIODE_LENGDE - 1)) }
            .toList()
    }
}