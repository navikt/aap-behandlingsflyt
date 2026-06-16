package no.nav.aap.behandlingsflyt.faktagrunnlag.register.personopplysninger

import no.nav.aap.behandlingsflyt.faktagrunnlag.register.barn.Dødsdato

data class Personopplysning(
    val fødselsdato: Fødselsdato,
    val dødsdato: Dødsdato? = null,
    val status: PersonStatus,
    val statsborgerskap: List<Statsborgerskap>,
    val utenlandsAddresser: List<UtenlandsAdresse> = emptyList()
)
