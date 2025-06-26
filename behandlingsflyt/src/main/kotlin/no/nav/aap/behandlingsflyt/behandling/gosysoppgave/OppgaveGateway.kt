package no.nav.aap.behandlingsflyt.behandling.gosysoppgave

import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import no.nav.aap.komponenter.gateway.Gateway

interface OppgaveGateway : Gateway {

    fun opprettOppgaveHvisIkkeEksisterer(
        oppgaveRequest: OpprettOppgaveRequest,
        bestillingReferanse: String,
        behandlingId: BehandlingId
    )
}