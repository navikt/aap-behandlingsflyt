package no.nav.aap.behandlingsflyt.faktagrunnlag.dokument.kontrakt.søknad

import no.nav.aap.behandlingsflyt.faktagrunnlag.register.barn.OppgitteBarn

// duplisert
@Deprecated(
    "Bruk Innsending-objekt i stedet. Og et annet endepunkt.", replaceWith = ReplaceWith(
        expression = "no.nav.aap.behandlingsflyt.kontrakt.hendelse.dokumenter.SøknadV0"
    )
)
class Søknad(
    val student: SøknadStudentDto?,
    val yrkesskade: String,
    val oppgitteBarn: OppgitteBarn?
) {
    // TODO: Håndtere IKKE_OPPGITT?
    fun harYrkesskade(): Boolean {
        return yrkesskade.uppercase() == "JA"
    }
}

class SøknadStudentDto(
    val erStudent: String,
    val kommeTilbake: String? = null
)
