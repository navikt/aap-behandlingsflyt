package no.nav.aap.behandlingsflyt.behandling.ansattinfo

import no.nav.aap.komponenter.gateway.GatewayProvider
import org.slf4j.LoggerFactory

class AnsattInfoService {
    val logger = LoggerFactory.getLogger(javaClass)

    private val ansattInfoGateway = GatewayProvider.provide<AnsattInfoGateway>()
    private val enhetGateway = GatewayProvider.provide<EnhetGateway>()

    fun hentAnsattNavnOgEnhet(navIdent: String): AnsattNavnOgEnhet? {
        try {
            val ansattInfo = ansattInfoGateway.hentAnsattInfo(navIdent)
            val enhet = enhetGateway.hentEnhet(ansattInfo.enhetsnummer)
            return AnsattNavnOgEnhet(navn = ansattInfo.navn, enhet = enhet.navn)
        } catch (e: Exception) {
            logger.warn("Kunne ikke hente ansattnavn og enhet. Fortsetter uten disse verdiene.", e)
            return null
        }
    }
    
    fun hentAnsattEnhet(navIdent: String): String? {
        return try {
            val ansattInfo = ansattInfoGateway.hentAnsattInfo(navIdent)
            ansattInfo.enhetsnummer
        } catch (e: Exception) {
            logger.warn("Kunne ikke hente enhet. Fortsetter uten disse verdiene.", e)
            null
        }
    }
}