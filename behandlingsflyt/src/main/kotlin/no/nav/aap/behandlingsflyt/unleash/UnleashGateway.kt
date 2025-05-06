package no.nav.aap.behandlingsflyt.unleash

import no.nav.aap.komponenter.gateway.Gateway

interface UnleashGateway : Gateway {
    fun isEnabled(featureToggle: FeatureToggle): Boolean
    fun isEnabled(featureToggle: FeatureToggle, ident: String): Boolean
}
