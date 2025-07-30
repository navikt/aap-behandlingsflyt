package no.nav.aap.behandlingsflyt.behandling.gosysoppgave

import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import no.nav.aap.komponenter.gateway.GatewayProvider
import no.nav.aap.behandlingsflyt.sakogbehandling.Ident
import no.nav.aap.behandlingsflyt.unleash.BehandlingsflytFeature
import no.nav.aap.behandlingsflyt.unleash.UnleashGateway

class GosysService(private val oppgaveGateway: OppgaveGateway, private val unleashGateway: UnleashGateway) {

    constructor(gatewayProvider: GatewayProvider) : this(
        oppgaveGateway = gatewayProvider.provide(),
        unleashGateway = gatewayProvider.provide()
    )

    fun opprettOppgave(
        aktivIdent: Ident,
        bestillingReferanse: String,
        behandlingId: BehandlingId,
        navKontor: String
    ) {
        if (unleashGateway.isEnabled(BehandlingsflytFeature.SosialHjelpFlereKontorer)) {
            oppgaveGateway.opprettOppgave(aktivIdent, bestillingReferanse, behandlingId, navKontor)
        }

    }
}