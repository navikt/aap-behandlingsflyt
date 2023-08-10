package no.nav.aap.vilkår.alder

import no.nav.aap.domene.behandling.Faktagrunnlag
import no.nav.aap.domene.behandling.grunnlag.person.Fødselsdato
import java.time.LocalDate

class Aldersgrunnlag(private val søknadsdato: LocalDate, private val fødselsdato: Fødselsdato) : Faktagrunnlag {

    fun alderPåSøknadsdato(): Int = fødselsdato.alderPåDato(søknadsdato)

}
