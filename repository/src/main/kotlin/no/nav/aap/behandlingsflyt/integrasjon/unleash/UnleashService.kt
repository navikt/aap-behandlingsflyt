package no.nav.aap.behandlingsflyt.integrasjon.unleash

import io.getunleash.DefaultUnleash
import io.getunleash.UnleashContext
import io.getunleash.util.UnleashConfig
import no.nav.aap.behandlingsflyt.unleash.FeatureToggle
import no.nav.aap.behandlingsflyt.unleash.UnleashGateway
import no.nav.aap.komponenter.config.requiredConfigForKey
import no.nav.aap.komponenter.gateway.Factory

class UnleashService : UnleashGateway {
    companion object : Factory<UnleashService> {
        override fun konstruer(): UnleashService {
            return UnleashService()
        }
    }

    private val unleash = DefaultUnleash(
        UnleashConfig
            .builder()
            .appName(requiredConfigForKey("nais.app.name"))
            .unleashAPI("${requiredConfigForKey("unleash.server.api.url")}/api")
            .apiKey(requiredConfigForKey("unleash.server.api.token"))
            .build()
    )

    override fun isEnabled(featureToggle: FeatureToggle): Boolean = unleash.isEnabled(featureToggle.key())
    override fun isEnabled(featureToggle: FeatureToggle, ident: String): Boolean = unleash.isEnabled(featureToggle.key(), UnleashContext.builder().userId(ident).build())

}
