package no.nav.aap.behandlingsflyt.faktagrunnlag.register.navenheter

import no.nav.aap.behandlingsflyt.behandling.ansattinfo.Enhet
import no.nav.aap.behandlingsflyt.behandling.ansattinfo.EnhetGateway
import no.nav.aap.komponenter.gateway.GatewayProvider
import org.slf4j.LoggerFactory

class NavKontorService(private val enhetGateway: EnhetGateway) {
    private val logger = LoggerFactory.getLogger(javaClass)

    constructor(gatewayProvider: GatewayProvider) : this(
        enhetGateway = gatewayProvider.provide()
    )

    fun hentNavEnheter(): List<Enhet>? {
        try {
            val enheter = enhetGateway.hentAlleEnheter()
            return enheter
        } catch (e: Exception) {
            logger.error("Kunne ikke hente enheter.", e)
            return emptyList()
        }
    }

}
