package no.nav.aap.behandlingsflyt.faktagrunnlag.register.barn

import no.nav.aap.behandlingsflyt.faktagrunnlag.register.personopplysninger.Fødselsdato
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.barn.BarnIdentifikator
import no.nav.aap.behandlingsflyt.sakogbehandling.Ident

/**
 * Barn oppgitt i søknaden.
 */
data class OppgitteBarn(val id: Long? = null, val oppgitteBarn: List<OppgittBarn>) {
    enum class Relasjon {
        FORELDER,
        FOSTERFORELDER,
    }

    data class OppgittBarn(
        val ident: Ident?,
        val navn: String? = null,
        val fødselsdato: Fødselsdato? = null,
        val relasjon: Relasjon? = null
    ) : IBarn {
        init {
            if (ident == null) {
                requireNotNull(navn) { "Om ident er null, må både navn og fødselsdato gis." }
                requireNotNull(fødselsdato) { "Om ident er null, må både navn og fødselsdato gis." }
            }
            if (fødselsdato == null || navn == null) {
                requireNotNull(ident) { "Om fødseldato eller navn ikke er gitt, må ident være satt." }
            }
        }

        override fun identifikator(): BarnIdentifikator = if (ident == null) {
            BarnIdentifikator.NavnOgFødselsdato(navn!!, fødselsdato!!)
        } else {
            BarnIdentifikator.BarnIdent(ident)
        }

        override fun fødselsdato(): Fødselsdato {
            return requireNotNull(fødselsdato) { "Fødselsdato skal være oppgitt i søknaden. Identifikatortype: ${identifikator().javaClass.simpleName}" }
        }
    }
}
