package no.nav.aap.behandlingsflyt.behandling.gosysoppgave

import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.refusjonkrav.NavKontorPeriodeDto
import no.nav.aap.behandlingsflyt.sakogbehandling.Ident
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import no.nav.aap.komponenter.gateway.GatewayProvider

class GosysService(private val oppgaveGateway: OppgaveGateway) {

    constructor(gatewayProvider: GatewayProvider) : this(
        oppgaveGateway = gatewayProvider.provide()
    )

    fun opprettOppgave(
        aktivIdent: Ident,
        bestillingReferanse: String,
        behandlingId: BehandlingId,
        navKontor: NavKontorPeriodeDto
    ) {
        oppgaveGateway.opprettOppgave(aktivIdent, bestillingReferanse, behandlingId, navKontor)
    }
}