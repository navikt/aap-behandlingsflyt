package no.nav.aap.behandlingsflyt.integrasjon.unleash

import io.getunleash.DefaultUnleash
import io.getunleash.UnleashContext
import io.getunleash.UnleashContextProvider
import io.getunleash.util.UnleashConfig
import no.nav.aap.behandlingsflyt.Context
import no.nav.aap.behandlingsflyt.unleash.FeatureToggle
import no.nav.aap.behandlingsflyt.unleash.UnleashGateway
import no.nav.aap.komponenter.config.requiredConfigForKey
import no.nav.aap.komponenter.gateway.Factory

class UnleashGatewayImpl private constructor(brukerIdent: () -> String? = { null }) : UnleashGateway {

    companion object : Factory<UnleashGatewayImpl> {
        override fun konstruer(): UnleashGatewayImpl {
            return UnleashGatewayImpl { Context.get()?.brukerIdent }
        }
    }

    private val appName = requiredConfigForKey("nais.app.name")

    private val unleash = DefaultUnleash(
        UnleashConfig
            .builder()
            .appName(appName)
            .unleashAPI("${requiredConfigForKey("unleash.server.api.url")}/api")
            .unleashContextProvider(contextProvider(brukerIdent()))
            .apiKey(requiredConfigForKey("unleash.server.api.token"))
            .build()
    )

    private fun contextProvider(brukerIdent: String?) =
        UnleashContextProvider {
            UnleashContext
                .builder()
                .appName(appName)
                .apply { brukerIdent?.let { this.userId(it) } }
                .build()
        }

    override fun isEnabled(featureToggle: FeatureToggle): Boolean =
        isEnabled(featureToggle, false)

    override fun isEnabled(featureToggle: FeatureToggle, defaultValue: Boolean): Boolean =
        unleash.isEnabled(featureToggle.key(), defaultValue)
}
