package no.nav.aap.behandlingsflyt.mdc

import no.nav.aap.behandlingsflyt.kontrakt.behandling.BehandlingReferanse
import no.nav.aap.behandlingsflyt.kontrakt.sak.Saksnummer
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId

data class LogKontekst(
    val saksnummer: Saksnummer? = null,
    val referanse: BehandlingReferanse? = null,
    val behandlingId: BehandlingId? = null
)
