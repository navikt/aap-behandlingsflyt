package no.nav.aap.behandlingsflyt.sakogbehandling.sak.flate

import java.time.LocalDate

data class SakPersoninfoDTO(
    val fnr: String,
    val navn: String,
    val personId: Long,
    val fødselsdato: LocalDate?,
    val dødsdato: LocalDate?,
)