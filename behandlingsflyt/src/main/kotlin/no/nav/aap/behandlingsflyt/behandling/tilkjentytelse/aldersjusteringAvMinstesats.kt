package no.nav.aap.behandlingsflyt.behandling.tilkjentytelse

import no.nav.aap.behandlingsflyt.faktagrunnlag.register.personopplysninger.Fødselsdato
import no.nav.aap.komponenter.tidslinje.Tidslinje
import no.nav.aap.komponenter.tidslinje.tidslinjeOf
import no.nav.aap.komponenter.type.Periode
import no.nav.aap.komponenter.verdityper.GUnit
import no.nav.aap.komponenter.verdityper.Tid
import java.time.LocalDate

internal interface AlderStrategi {
    operator fun invoke(minsteÅrligYtelse: GUnit, årligYtelse: GUnit): ÅrligYtelse
}

internal fun aldersjusteringAvMinsteÅrligeYtelse(
    fødselsdato: Fødselsdato,
): Tidslinje<AlderStrategi> {
    return tidslinjeOf(
        Periode(LocalDate.MIN, fødselsdato.`25årsDagen`().minusDays(1)) to Under25,
        Periode(fødselsdato.`25årsDagen`(), Tid.MAKS) to Over25
    )
}

internal data class ÅrligYtelse(
    val årligYtelse: GUnit,
    val minstesats: Minstesats
)

/** § 11-20 første avsnitt tredje setning.
 * > For medlem under 25 år er minste årlige ytelse 2/3 av 2,041 ganger grunnbeløpet
 */
internal val Under25: AlderStrategi = object : AlderStrategi {
    override fun invoke(minsteÅrligYtelse: GUnit, årligYtelse: GUnit): ÅrligYtelse {
        val aldersjustert = minsteÅrligYtelse.toTredjedeler()

        if (aldersjustert > årligYtelse) return ÅrligYtelse(aldersjustert, Minstesats.MINSTESATS_UNDER_25)

        return ÅrligYtelse(aldersjustert, Minstesats.IKKE_MINSTESATS)
    }
}

internal val Over25: AlderStrategi = object : AlderStrategi {
    override fun invoke(minsteÅrligYtelse: GUnit, årligYtelse: GUnit): ÅrligYtelse {
        if (minsteÅrligYtelse > årligYtelse) return ÅrligYtelse(minsteÅrligYtelse, Minstesats.MINSTESATS_OVER_25)

        return ÅrligYtelse(årligYtelse, Minstesats.IKKE_MINSTESATS)
    }
}