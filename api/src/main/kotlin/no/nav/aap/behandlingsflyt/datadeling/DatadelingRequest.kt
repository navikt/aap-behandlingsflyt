package no.nav.aap.behandlingsflyt.datadeling

import java.time.LocalDate

data class SakerRequest (
    val personidentifikatorer: List<String>
)

data class DatadelingRequest (
        val personidentifikator: String,
        val fraOgMedDato: LocalDate,
        val tilOgMedDato: LocalDate
)