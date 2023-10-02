package no.nav.aap.behandlingsflyt.flate.sak

import no.nav.aap.behandlingsflyt.domene.Periode
import no.nav.aap.behandlingsflyt.domene.sak.Status

data class UtvidetSaksinfoDTO(
    val saksnummer: String,
    val status: Status,
    val periode: no.nav.aap.behandlingsflyt.domene.Periode,
    val behandlinger: List<BehandlinginfoDTO>,
    val ident: String
)