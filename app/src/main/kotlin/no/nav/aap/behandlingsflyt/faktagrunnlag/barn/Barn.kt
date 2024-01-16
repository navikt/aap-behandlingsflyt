package no.nav.aap.behandlingsflyt.faktagrunnlag.barn

import no.nav.aap.behandlingsflyt.faktagrunnlag.personopplysninger.Fødselsdato
import no.nav.aap.verdityper.sakogbehandling.Ident
import no.nav.aap.verdityper.Periode

data class Barn(val ident: Ident, val fødselsdato: Fødselsdato) {
    fun periodeMedRettTil(): Periode {
        val fom = fødselsdato.toLocalDate()
        return Periode(fom, fom.plusYears(18).minusDays(1))
    }
}
