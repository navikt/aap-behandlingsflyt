package no.nav.aap.behandlingsflyt.behandling.ansattinfo

import com.github.benmanes.caffeine.cache.Caffeine
import no.nav.aap.komponenter.gateway.GatewayProvider
import org.slf4j.LoggerFactory
import java.time.Duration

class AnsattInfoService(private val ansattInfoGateway: AnsattInfoGateway, private val enhetGateway: EnhetGateway) {
    private val logger = LoggerFactory.getLogger(javaClass)

    constructor(gatewayProvider: GatewayProvider) : this(
        gatewayProvider.provide(),
        gatewayProvider.provide()
    )

    fun hentAnsattNavnOgEnhet(navIdent: String): AnsattNavnOgEnhet? {
        try {
            val ansattInfoCacheResponse = ansattInfoCache.getIfPresent(navIdent)
            val ansattInfo = ansattInfoCacheResponse ?: ansattInfoGateway.hentAnsattInfo(navIdent)
            if (ansattInfoCacheResponse == null){
                ansattInfoCache.put(navIdent, ansattInfo)
            }

            val enhetNavnCacheResponse = enhetNavnCache.getIfPresent(ansattInfo.enhetsnummer)
            if (enhetNavnCacheResponse != null) return AnsattNavnOgEnhet(navn = ansattInfo.navn, enhet = enhetNavnCacheResponse)
            val enhet = enhetGateway.hentEnhet(ansattInfo.enhetsnummer)
            enhetNavnCache.put(ansattInfo.enhetsnummer, enhet.navn)
            return AnsattNavnOgEnhet(navn = ansattInfo.navn, enhet = enhet.navn)
        } catch (e: Exception) {
            logger.warn("Kunne ikke hente ansattnavn og enhet for ident $navIdent.", e)
            return null
        }
    }

    fun hentAnsatteVisningsnavn(navIdenter: List<String>): List<AnsattVisningsnavn?> {
        try {
            val cachetRespons = ansatteVisningsnavnCache.getIfPresent(navIdenter);
            if (cachetRespons != null) return cachetRespons

            val ansattInfo = ansattInfoGateway.hentAnsatteVisningsnavn(navIdenter)
            ansatteVisningsnavnCache.put(navIdenter, ansattInfo)
            return ansattInfo
        } catch (e: Exception) {
            logger.info("Kunne ikke hente ansattnavn.", e)
            return emptyList()
        }
    }

    fun hentAnsattEnhet(navIdent: String): String? {
        return try {
            val ansattInfoCacheResponse = ansattInfoCache.getIfPresent(navIdent)
            if (ansattInfoCacheResponse != null) return ansattInfoCacheResponse.enhetsnummer

            val ansattInfo = ansattInfoGateway.hentAnsattInfo(navIdent)
            ansattInfoCache.put(navIdent, ansattInfo)
            return ansattInfo.enhetsnummer
        } catch (e: Exception) {
            logger.info("Kunne ikke hente enhet. Fortsetter uten disse verdiene.", e)
            null
        }
    }

    companion object {

        private val ansattInfoCache = Caffeine.newBuilder()
            .expireAfterWrite(Duration.ofHours(1))
            .maximumSize(1000)
            .build<String, AnsattInfo>()

        private val enhetNavnCache = Caffeine.newBuilder()
            .expireAfterWrite(Duration.ofHours(24))
            .maximumSize(1000)
            .build<String, String>()


        private val ansatteVisningsnavnCache = Caffeine.newBuilder()
            .expireAfterWrite(Duration.ofHours(24))
            .maximumSize(1000)
            .build<List<String>, List<AnsattVisningsnavn?>>()
    }
}