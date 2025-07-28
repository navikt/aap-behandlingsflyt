package no.nav.aap.behandlingsflyt.faktagrunnlag.register.navenheter

import no.nav.aap.behandlingsflyt.behandling.ansattinfo.Enhet
import no.nav.aap.behandlingsflyt.behandling.ansattinfo.EnhetGateway
import no.nav.aap.komponenter.gateway.GatewayProvider
import org.slf4j.LoggerFactory

class NavKontorService {
    private val logger = LoggerFactory.getLogger(javaClass)

    private val enhetGateway = GatewayProvider.provide<EnhetGateway>()

    fun hentNavEnheter(): List<Enhet>? {
        try {
            val enheter = enhetGateway.hentAlleEnheter()
            return enheter.map { it }
        } catch (e: Exception) {
            logger.error("Kunne ikke hente enheter.", e)
            return emptyList()
        }
    }

}
