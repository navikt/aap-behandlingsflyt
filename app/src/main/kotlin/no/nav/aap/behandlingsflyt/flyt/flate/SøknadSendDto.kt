package no.nav.aap.behandlingsflyt.flyt.flate

class SøknadSendDto(
    val saksnummer: String,
    val journalpostId: String,
    val søknad: SøknadDto
)

class SøknadDto(
    val student: Boolean
)
