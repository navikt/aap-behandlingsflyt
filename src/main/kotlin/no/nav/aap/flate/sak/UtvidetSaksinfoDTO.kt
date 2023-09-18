package no.nav.aap.flate.sak

import no.nav.aap.domene.Periode
import no.nav.aap.domene.sak.Status

data class UtvidetSaksinfoDTO(
    val saksnummer: String,
    val status: Status,
    val periode: Periode,
    val behandlinger: List<BehandlinginfoDTO>,
    val ident: String
)