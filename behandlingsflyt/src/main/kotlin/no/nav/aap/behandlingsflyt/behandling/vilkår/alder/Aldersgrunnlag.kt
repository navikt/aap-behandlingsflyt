package no.nav.aap.behandlingsflyt.behandling.vilkår.alder

import no.nav.aap.behandlingsflyt.faktagrunnlag.Faktagrunnlag
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.personopplysninger.Fødselsdato
import no.nav.aap.komponenter.type.Periode
import java.time.LocalDate

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
