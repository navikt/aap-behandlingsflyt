package no.nav.aap.behandlingsflyt.flyt.vilkår.alder

import no.nav.aap.behandlingsflyt.domene.Periode
import no.nav.aap.behandlingsflyt.flyt.vilkår.Faktagrunnlag
import no.nav.aap.behandlingsflyt.faktagrunnlag.personopplysninger.Fødselsdato

class Aldersgrunnlag(val periode: Periode, private val fødselsdato: Fødselsdato) : Faktagrunnlag {

    fun alderPåSøknadsdato(): Int = fødselsdato.alderPåDato(periode.fom)
}
