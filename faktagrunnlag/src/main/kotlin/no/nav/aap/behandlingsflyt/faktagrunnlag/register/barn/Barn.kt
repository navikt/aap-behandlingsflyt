package no.nav.aap.behandlingsflyt.faktagrunnlag.register.barn

import no.nav.aap.behandlingsflyt.faktagrunnlag.usorterte.personopplysninger.Fødselsdato
import no.nav.aap.verdityper.Periode
import no.nav.aap.verdityper.sakogbehandling.Ident

data class Barn(val ident: Ident, val fødselsdato: Fødselsdato) {
    fun periodeMedRettTil(): Periode {
        val fom = fødselsdato.toLocalDate()
        return Periode(fom, fom.plusYears(18).minusDays(1))
    }
}
