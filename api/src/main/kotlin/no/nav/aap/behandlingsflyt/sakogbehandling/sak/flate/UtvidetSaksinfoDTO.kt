package no.nav.aap.behandlingsflyt.sakogbehandling.sak.flate

import no.nav.aap.behandlingsflyt.kontrakt.sak.Status
import no.nav.aap.komponenter.type.Periode
import java.time.LocalDateTime

data class UtvidetSaksinfoDTO(
    val saksnummer: String,
    val opprettetTidspunkt: LocalDateTime,
    val status: Status,
    val periode: Periode,
    val behandlinger: List<BehandlinginfoDTO>,
    val ident: String,
    val søknadErTrukket: Boolean? = null
)