package no.nav.aap.behandlingsflyt.unleash

import no.nav.aap.lookup.gateway.Gateway

interface UnleashGateway : Gateway {
    fun isEnabled(featureToggle: FeatureToggle): Boolean
}
