package no.nav.aap.behandlingsflyt.sakogbehandling.sak.flate

import no.nav.aap.verdityper.Periode
import java.time.LocalDateTime

data class SaksinfoDTO(
    val saksnummer: String,
    val opprettetTidspunkt: LocalDateTime,
    val periode: Periode,
    val ident: String
)
