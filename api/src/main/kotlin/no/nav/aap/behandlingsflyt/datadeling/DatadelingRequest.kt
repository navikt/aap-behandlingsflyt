package no.nav.aap.behandlingsflyt.datadeling

import java.time.LocalDate

data class DatadelingRequest (
        val personidentifikator: String,
        val fraOgMedDato: LocalDate,
        val tilOgMedDato: LocalDate
)