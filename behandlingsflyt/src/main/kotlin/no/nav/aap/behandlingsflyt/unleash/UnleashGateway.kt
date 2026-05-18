package no.nav.aap.behandlingsflyt.unleash

import no.nav.aap.behandlingsflyt.behandling.brev.bestilling.TypeBrev
import no.nav.aap.komponenter.gateway.Gateway

interface UnleashGateway : Gateway {
    fun isEnabled(featureToggle: FeatureToggle): Boolean
    fun isDisabled(featureToggle: FeatureToggle): Boolean = !isEnabled(featureToggle)
    fun isEnabled(featureToggle: FeatureToggle, ident: String): Boolean
    fun isDisabled(featureToggle: FeatureToggle, ident: String): Boolean = !isEnabled(featureToggle, ident)
    fun isEnabled(featureToggle: FeatureToggle, ident: String, typeBrev: TypeBrev): Boolean

    /**
     * Les sak-id-filter fra Unleash-variant (CSV-payload). Standard er {0}, som sikrer at ingen
     * reelle saker prosesseres før man eksplisitt konfigurerer en liste via Unleash-varianten.
     */
    fun hentSakIdFilter(featureToggle: FeatureToggle): Set<Long> = setOf(0L)
}
