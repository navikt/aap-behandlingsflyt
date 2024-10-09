package no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.arbeidsevne

import no.nav.aap.verdityper.Prosent
import java.time.LocalDate
import java.time.LocalDateTime

data class Arbeidsevnevurdering(
    val begrunnelse: String,
    val arbeidsevne: Prosent,
    val fraDato: LocalDate,
    val opprettetTid: LocalDateTime
)
