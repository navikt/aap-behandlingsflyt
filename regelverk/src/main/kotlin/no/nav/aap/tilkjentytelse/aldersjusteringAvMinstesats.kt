package no.nav.aap.tilkjentytelse

import java.time.LocalDate
import no.nav.aap.komponenter.tidslinje.Tidslinje
import no.nav.aap.komponenter.tidslinje.tidslinjeOf
import no.nav.aap.komponenter.type.Periode
import no.nav.aap.komponenter.verdityper.GUnit
import no.nav.aap.komponenter.verdityper.Tid
import no.nav.aap.personopplysninger.Fødselsdato

interface AlderStrategi {
    operator fun invoke(minsteÅrligYtelse: GUnit, årligYtelse: GUnit): ÅrligYtelse
}

fun aldersjusteringAvMinsteÅrligeYtelse(
    fødselsdato: Fødselsdato,
): Tidslinje<AlderStrategi> {
    return tidslinjeOf(
        Periode(LocalDate.MIN, fødselsdato.`25årsDagen`().minusDays(1)) to Under25,
        Periode(fødselsdato.`25årsDagen`(), Tid.MAKS) to Over25
    )
}

data class ÅrligYtelse(
    val årligYtelse: GUnit,
    val minstesats: Minstesats
)

/** § 11-20 første avsnitt tredje setning.
 * > For medlem under 25 år er minste årlige ytelse 2/3 av 2,041 ganger grunnbeløpet
 */
val Under25: AlderStrategi = object : AlderStrategi {
    override fun invoke(minsteÅrligYtelse: GUnit, årligYtelse: GUnit): ÅrligYtelse {
        val aldersjustert = minsteÅrligYtelse.toTredjedeler()

        if (aldersjustert > årligYtelse) return ÅrligYtelse(aldersjustert, Minstesats.MINSTESATS_UNDER_25)

        return ÅrligYtelse(årligYtelse, Minstesats.IKKE_MINSTESATS)
    }
}

val Over25: AlderStrategi = object : AlderStrategi {
    override fun invoke(minsteÅrligYtelse: GUnit, årligYtelse: GUnit): ÅrligYtelse {
        if (minsteÅrligYtelse > årligYtelse) return ÅrligYtelse(minsteÅrligYtelse, Minstesats.MINSTESATS_OVER_25)

        return ÅrligYtelse(årligYtelse, Minstesats.IKKE_MINSTESATS)
    }
}