package no.nav.aap.barnetillegg

import no.nav.aap.misc.Ident
import no.nav.aap.personopplysninger.Fødselsdato

/**
 * Barn oppgitt manuelt av saksbehandler.
 */
data class SaksbehandlerOppgitteBarn(val id: Long? = null, val barn: List<SaksbehandlerOppgitteBarn>) {

    data class SaksbehandlerOppgitteBarn(
        val ident: Ident?,
        val navn: String,
        val fødselsdato: Fødselsdato,
        val relasjon: Relasjon
    ) : IBarn {

        override fun identifikator(): BarnIdentifikator = if (ident == null) {
            BarnIdentifikator.NavnOgFødselsdato(navn, fødselsdato)
        } else {
            BarnIdentifikator.BarnIdent(ident, navn, fødselsdato)
        }

        override fun fødselsdato(): Fødselsdato = fødselsdato
    }
}