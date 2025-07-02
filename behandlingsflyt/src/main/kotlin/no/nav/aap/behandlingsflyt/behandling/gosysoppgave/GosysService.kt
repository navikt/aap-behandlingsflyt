package no.nav.aap.behandlingsflyt.behandling.gosysoppgave

import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import no.nav.aap.komponenter.gateway.GatewayProvider
import no.nav.aap.behandlingsflyt.sakogbehandling.Ident

class GosysService(private val oppgaveGateway: OppgaveGateway) {

    constructor(gatewayProvider: GatewayProvider) : this(
        oppgaveGateway = gatewayProvider.provide(),
    )

    fun opprettOppgaveHvisIkkeEksisterer(
        aktivIdent: Ident,
        bestillingReferanse: String,
        behandlingId: BehandlingId,
        navKontor: String
    ) {
        oppgaveGateway.opprettOppgaveHvisIkkeEksisterer(aktivIdent, bestillingReferanse, behandlingId, navKontor)

    }
}