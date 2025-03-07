package no.nav.aap.behandlingsflyt.hendelse.oppgavestyring

import no.nav.aap.behandlingsflyt.kontrakt.hendelse.BehandlingFlytStoppetHendelse
import no.nav.aap.lookup.gateway.Gateway

interface OppgavestyringGateway : Gateway {
    fun varsleHendelse(hendelse: BehandlingFlytStoppetHendelse)
}