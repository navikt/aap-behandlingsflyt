package no.nav.aap.behandlingsflyt.domene.vilkår.alder

import no.nav.aap.behandlingsflyt.domene.behandling.Faktagrunnlag
import no.nav.aap.behandlingsflyt.grunnlag.person.Fødselsdato
import java.time.LocalDate

class Aldersgrunnlag(private val søknadsdato: LocalDate, private val fødselsdato: Fødselsdato) : Faktagrunnlag {

    fun alderPåSøknadsdato(): Int = fødselsdato.alderPåDato(søknadsdato)

}
