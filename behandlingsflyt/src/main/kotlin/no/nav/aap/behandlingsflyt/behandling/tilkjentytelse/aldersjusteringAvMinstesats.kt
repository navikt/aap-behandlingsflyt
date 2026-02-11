package no.nav.aap.behandlingsflyt.behandling.tilkjentytelse

import no.nav.aap.behandlingsflyt.faktagrunnlag.register.personopplysninger.Fødselsdato
import no.nav.aap.komponenter.tidslinje.Tidslinje
import no.nav.aap.komponenter.tidslinje.tidslinjeOf
import no.nav.aap.komponenter.type.Periode
import no.nav.aap.komponenter.verdityper.GUnit
import no.nav.aap.komponenter.verdityper.Tid
import java.time.LocalDate

enum class AldersStrategi {
    Under25, Over25;

    /** § 11-20 første avsnitt tredje setning.
     * > For medlem under 25 år er minste årlige ytelse 2/3 av 2,041 ganger grunnbeløpet
     */
    fun apply(minsteÅrligeYtelse: GUnit): GUnit {
        return when (this) {
            Under25 -> minsteÅrligeYtelse.toTredjedeler()
            Over25 -> minsteÅrligeYtelse
        }
    }
}

internal fun aldersjusteringAvMinsteÅrligeYtelse(fødselsdato: Fødselsdato): Tidslinje<AldersStrategi> {
    return tidslinjeOf(
        Periode(LocalDate.MIN, fødselsdato.`25årsDagen`().minusDays(1)) to AldersStrategi.Under25,
        Periode(fødselsdato.`25årsDagen`(), Tid.MAKS) to AldersStrategi.Over25
    )
}