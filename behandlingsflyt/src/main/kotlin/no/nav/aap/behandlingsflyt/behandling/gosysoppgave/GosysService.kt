package no.nav.aap.behandlingsflyt.behandling.gosysoppgave

import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import no.nav.aap.komponenter.gateway.GatewayProvider

class GosysService(private val oppgaveGateway: OppgaveGateway) {

    constructor(gatewayProvider: GatewayProvider) : this(
        oppgaveGateway = gatewayProvider.provide(),
    )

    fun opprettOppgaveHvisIkkeEksisterer(
        oppgaveRequest: OpprettOppgaveRequest,
        bestillingReferanse: String,
        behandlingId: BehandlingId
    ) {
        oppgaveGateway.opprettOppgaveHvisIkkeEksisterer(oppgaveRequest, bestillingReferanse, behandlingId)

    }
}