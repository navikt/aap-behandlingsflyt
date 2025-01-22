package no.nav.aap.behandlingsflyt.mdc

import no.nav.aap.behandlingsflyt.kontrakt.behandling.BehandlingReferanse
import no.nav.aap.behandlingsflyt.kontrakt.sak.Saksnummer

data class LogKontekst(val saksnummer: Saksnummer? = null, val referanse: BehandlingReferanse? = null)
