package no.nav.aap.behandlingsflyt.sakogbehandling.sak.flate

import java.time.LocalDate
import java.util.UUID

data class SakPersoninfoDTO(
    val fnr: String,
    val navn: String,
    val kryptertIdent: String,
    val fødselsdato: LocalDate?,
    val dødsdato: LocalDate?,
)