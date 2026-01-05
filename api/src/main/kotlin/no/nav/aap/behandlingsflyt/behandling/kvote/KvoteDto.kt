package no.nav.aap.behandlingsflyt.behandling.kvote

import java.time.LocalDate

data class KvoteDto (
    val ordinærKvote: OrdinærKvote,
    val studentkvote: StudentKvote,
)

data class OrdinærKvote(
    val kvote: Int,
    val bruktKvote: Int?,
    var gjenværendeKvote: Int?,
    val senesteDatoForKvote: LocalDate?,
)

data class StudentKvote(
    val kvoteStartDato: LocalDate?,
    val kvoteSluttDato: LocalDate?,
)
