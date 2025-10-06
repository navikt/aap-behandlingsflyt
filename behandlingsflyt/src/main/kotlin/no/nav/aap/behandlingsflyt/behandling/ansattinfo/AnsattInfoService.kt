package no.nav.aap.behandlingsflyt.behandling.ansattinfo

import no.nav.aap.komponenter.gateway.GatewayProvider
import org.slf4j.LoggerFactory

class AnsattInfoService(private val ansattInfoGateway: AnsattInfoGateway, private val enhetGateway: EnhetGateway) {
    private val logger = LoggerFactory.getLogger(javaClass)

    constructor(gatewayProvider: GatewayProvider) : this(
        gatewayProvider.provide(),
        gatewayProvider.provide()
    )

    fun hentAnsattNavnOgEnhet(navIdent: String): AnsattNavnOgEnhet? {
        try {
            val ansattInfo = ansattInfoGateway.hentAnsattInfo(navIdent)
            val enhet = enhetGateway.hentEnhet(ansattInfo.enhetsnummer)
            return AnsattNavnOgEnhet(navn = ansattInfo.navn, enhet = enhet.navn)
        } catch (e: Exception) {
            logger.info("Kunne ikke hente ansattnavn og enhet. Fortsetter uten disse verdiene.", e)
            return null
        }
    }

    fun hentAnsatteVisningsnavn(navIdenter: List<String>): List<AnsattVisningsnavn?> {
        try {
            return ansattInfoGateway.hentAnsatteVisningsnavn(navIdenter)
        } catch (e: Exception) {
            logger.info("Kunne ikke hente ansattnavn.", e)
            return emptyList()
        }
    }

    fun hentAnsattEnhet(navIdent: String): String? {
        return try {
            val ansattInfo = ansattInfoGateway.hentAnsattInfo(navIdent)
            ansattInfo.enhetsnummer
        } catch (e: Exception) {
            logger.info("Kunne ikke hente enhet. Fortsetter uten disse verdiene.", e)
            null
        }
    }
}