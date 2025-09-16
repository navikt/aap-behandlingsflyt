package no.nav.aap.behandlingsflyt.sakogbehandling.sak.adapters

import no.nav.aap.behandlingsflyt.sakogbehandling.Ident
import java.time.LocalDate

class Personinfo(
    val ident: Ident,
    val fødselsdato: LocalDate? = null,
    val dødsdato: LocalDate? = null,
    val fornavn: String?,
    val mellomnavn: String?,
    val etternavn: String?
) {

    fun fulltNavn(): String {
        return listOfNotNull(
            fornavn,
            mellomnavn,
            etternavn
        ).filter { it.isNotBlank() }.ifEmpty { listOf("Ukjent") }.joinToString(" ")
    }
}