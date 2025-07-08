package no.nav.aap.behandlingsflyt.faktagrunnlag.register.barn

import no.nav.aap.behandlingsflyt.faktagrunnlag.register.personopplysninger.Fødselsdato
import no.nav.aap.behandlingsflyt.sakogbehandling.Ident
import no.nav.aap.komponenter.type.Periode

data class Barn(val ident: Ident, val fødselsdato: Fødselsdato, val dødsdato: Dødsdato? = null) {
    fun periodeMedRettTil(): Periode {
        val fom = fødselsdato.toLocalDate()
        // TODO: ta hensyn til dødsdato hvis den er satt (Fredrik)
        return Periode(fom, fom.plusYears(18).minusDays(1))
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Barn

        if (ident != other.ident) return false
        if (fødselsdato != other.fødselsdato) return false
        if (dødsdato != other.dødsdato) return false

        return true
    }

    override fun hashCode(): Int {
        var result = ident.hashCode()
        result = 31 * result + fødselsdato.hashCode()
        result = 31 * result + (dødsdato?.hashCode() ?: 0)
        return result
    }
}
