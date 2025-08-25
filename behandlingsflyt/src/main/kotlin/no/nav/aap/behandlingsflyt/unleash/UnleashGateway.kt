package no.nav.aap.behandlingsflyt.unleash

import no.nav.aap.komponenter.gateway.Gateway

interface UnleashGateway : Gateway {
    fun isEnabled(featureToggle: FeatureToggle): Boolean
    fun isDisabled(featureToggle: FeatureToggle): Boolean = !isEnabled(featureToggle)
    fun isEnabled(featureToggle: FeatureToggle, ident: String): Boolean
    fun isDisabled(featureToggle: FeatureToggle, ident: String): Boolean = !isEnabled(featureToggle, ident)

}
