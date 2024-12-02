package no.nav.aap.behandlingsflyt.sakogbehandling.sak.adapters

import no.nav.aap.behandlingsflyt.sakogbehandling.Ident

class Personinfo(val ident: Ident, val fornavn: String?, val mellomnavn: String?, val etternavn: String?) {

    fun fulltNavn(): String {
        return listOfNotNull(
            fornavn,
            mellomnavn,
            etternavn
        ).filter { it.isNotBlank() }.ifEmpty { listOf("Ukjent") }.joinToString(" ")
    }
}