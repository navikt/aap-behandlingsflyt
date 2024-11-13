package no.nav.aap.behandlingsflyt.behandling.tilkjentytelse

import no.nav.aap.behandlingsflyt.faktagrunnlag.register.personopplysninger.Fødselsdato
import no.nav.aap.komponenter.type.Periode
import no.nav.aap.tidslinje.Segment
import no.nav.aap.tidslinje.Tidslinje
import no.nav.aap.komponenter.verdityper.GUnit
import java.time.LocalDate

internal interface AlderStrategi {
    fun aldersjustering(minsteÅrligYtelseVerdi: GUnit): GUnit

    object Under25 : AlderStrategi {
        override fun aldersjustering(minsteÅrligYtelseVerdi: GUnit): GUnit {
            return minsteÅrligYtelseVerdi.toTredjedeler()
        }
    }

    object Over25 : AlderStrategi {
        override fun aldersjustering(minsteÅrligYtelseVerdi: GUnit): GUnit {
            return minsteÅrligYtelseVerdi
        }
    }
}

internal class MinsteÅrligYtelseAlderTidslinje(val fødselsdato: Fødselsdato) {
    fun tilTidslinje(): Tidslinje<AlderStrategi> {
        return Tidslinje(
            listOf(
                Segment(
                    periode = Periode(LocalDate.MIN, fødselsdato.`25årsDagen`().minusDays(1)),
                    verdi = AlderStrategi.Under25
                ),
                Segment(
                    periode = Periode(fødselsdato.`25årsDagen`(), LocalDate.MAX),
                    verdi = AlderStrategi.Over25
                )
            )
        )
    }

}