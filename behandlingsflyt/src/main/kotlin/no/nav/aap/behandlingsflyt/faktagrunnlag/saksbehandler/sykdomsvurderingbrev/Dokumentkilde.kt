package no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.sykdomsvurderingbrev

import java.time.LocalDate

data class Dokumentkilde(
    val journalpostId: String,
    val dokumentInfoId: String,
    val dokumentnavn: String,
    val journalføringsdato: LocalDate,
)
