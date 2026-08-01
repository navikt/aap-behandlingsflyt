package no.nav.aap.personopplysninger

data class Personopplysning(
    val fødselsdato: Fødselsdato,
    val dødsdato: Dødsdato? = null,
    val status: PersonStatus,
    val statsborgerskap: List<Statsborgerskap>,
    val utenlandsAddresser: List<UtenlandsAdresse> = emptyList()
)