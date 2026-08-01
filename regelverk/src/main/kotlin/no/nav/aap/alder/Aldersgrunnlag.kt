package no.nav.aap.alder

import java.time.LocalDate
import no.nav.aap.misc.Faktagrunnlag
import no.nav.aap.personopplysninger.Fødselsdato
import no.nav.aap.komponenter.type.Periode

class Aldersgrunnlag(
    val periode: Periode,
    val fødselsdato: Fødselsdato,
    val grenseForAntallMånederFørFylte18: Long,
    val vurderingsdato: LocalDate = LocalDate.now(),
) : Faktagrunnlag {
    fun fyller(alder: Int): LocalDate {
        return fødselsdato.toLocalDate().plusYears(alder.toLong())
    }
}