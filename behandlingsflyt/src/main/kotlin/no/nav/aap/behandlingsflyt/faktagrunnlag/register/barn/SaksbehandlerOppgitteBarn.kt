package no.nav.aap.behandlingsflyt.faktagrunnlag.register.barn

import no.nav.aap.behandlingsflyt.faktagrunnlag.register.personopplysninger.Fødselsdato
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.barn.BarnIdentifikator
import no.nav.aap.behandlingsflyt.sakogbehandling.Ident

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
            BarnIdentifikator.BarnIdent(ident)
        }

        override fun fødselsdato(): Fødselsdato = fødselsdato
    }
}
