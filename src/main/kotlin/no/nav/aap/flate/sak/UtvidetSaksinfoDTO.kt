package no.nav.aap.flate.sak

import no.nav.aap.domene.sak.Status
import no.nav.aap.domene.typer.Periode

data class UtvidetSaksinfoDTO(
    val saksnummer: String,
    val status: Status,
    val periode: Periode,
    val behandlinger: List<BehandlinginfoDTO>
)