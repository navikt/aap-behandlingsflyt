package no.nav.aap.behandlingsflyt.behandling.underveis

import no.nav.aap.komponenter.type.Periode
import java.time.LocalDate

/** Meldeperiode https://lovdata.no/lov/1997-02-28-19/§11-10 */
data class Meldeperiode(
    private val meldeperiode: Periode,
) {
    val asPeriode: Periode get() = meldeperiode

    fun <T> map(body: (LocalDate) -> T): List<T> =
        meldeperiode.map(body)

    companion object {
        fun forRettighetsperiode(rettighetsperiodeFom: LocalDate, dato: LocalDate): Meldeperiode {
            val antallDager = Periode(rettighetsperiodeFom, dato).antallDager().toLong()
            val fom = rettighetsperiodeFom.plusDays((antallDager / 14) * 14)
            return Meldeperiode(Periode(fom = fom, tom = fom.plusDays(13)))
        }
    }
}

fun <T> Periode.map(body: (LocalDate) -> T): List<T> {
    return generateSequence(this.fom) { current ->
        if (current < this.tom)
            current.plusDays(1)
        else
            null
    }
        .map(body)
        .toList()
}

