package no.nav.aap.behandlingsflyt.sak.flate

import no.nav.aap.behandlingsflyt.Periode
import no.nav.aap.behandlingsflyt.sak.Status

data class UtvidetSaksinfoDTO(
    val saksnummer: String,
    val status: Status,
    val periode: Periode,
    val behandlinger: List<BehandlinginfoDTO>,
    val ident: String
)