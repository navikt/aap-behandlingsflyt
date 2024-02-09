package no.nav.aap.behandlingsflyt.tilkjentytelse

import no.nav.aap.behandlingsflyt.faktagrunnlag.register.personopplysninger.Fødselsdato
import no.nav.aap.tidslinje.Segment
import no.nav.aap.tidslinje.Tidslinje
import no.nav.aap.verdityper.GUnit
import no.nav.aap.verdityper.Periode
import java.time.LocalDate

internal interface AlderStrategi {
    fun aldersjustering(minsteÅrligYtelseVerdi: GUnit): GUnit

    object Under25 : AlderStrategi {
        override fun aldersjustering(misteÅrligYtelseVerdi: GUnit): GUnit {
            return misteÅrligYtelseVerdi.toTredjedeler()
        }
    }

    object Over25 : AlderStrategi {
        override fun aldersjustering(misteÅrligYtelseVerdi: GUnit): GUnit {
            return misteÅrligYtelseVerdi
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