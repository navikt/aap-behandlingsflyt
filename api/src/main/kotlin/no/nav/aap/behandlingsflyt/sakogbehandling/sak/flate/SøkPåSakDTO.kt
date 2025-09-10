package no.nav.aap.behandlingsflyt.sakogbehandling.sak.flate

import no.nav.aap.behandlingsflyt.kontrakt.sak.Saksnummer
import java.time.LocalDate

data class SøkPåSakDTO(
    val ident: String,
    val navn: String,
    val saksnummer: Saksnummer,
    val opprettetTidspunkt: LocalDate,
    val harTilgang: Boolean
)