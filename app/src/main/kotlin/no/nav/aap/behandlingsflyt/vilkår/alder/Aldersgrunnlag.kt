package no.nav.aap.behandlingsflyt.vilkår.alder

import no.nav.aap.behandlingsflyt.faktagrunnlag.personopplysninger.Fødselsdato
import no.nav.aap.behandlingsflyt.faktagrunnlag.vilkårsresultat.Faktagrunnlag
import no.nav.aap.verdityper.Periode

class Aldersgrunnlag(val periode: Periode, private val fødselsdato: Fødselsdato) : Faktagrunnlag {

    fun alderPåSøknadsdato(): Int = fødselsdato.alderPåDato(periode.fom)
}
