package no.nav.aap.behandlingsflyt.vilkår.alder

import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Faktagrunnlag
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.personopplysninger.Fødselsdato
import no.nav.aap.verdityper.Periode
import java.time.LocalDate

class Aldersgrunnlag(val periode: Periode, private val fødselsdato: Fødselsdato) : Faktagrunnlag {

    fun alderPåSøknadsdato(): Int = fødselsdato.alderPåDato(periode.fom)

    fun sisteDagMedYtelse(): LocalDate {
        return fødselsdato.toLocalDate().plusYears(67)
    }
}
