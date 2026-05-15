package no.nav.aap.behandlingsflyt.integrasjon.unleash

import io.getunleash.DefaultUnleash
import io.getunleash.UnleashContext
import io.getunleash.util.UnleashConfig
import no.nav.aap.behandlingsflyt.behandling.brev.bestilling.TypeBrev
import no.nav.aap.behandlingsflyt.unleash.FeatureToggle
import no.nav.aap.behandlingsflyt.unleash.UnleashGateway
import no.nav.aap.komponenter.config.requiredConfigForKey

object UnleashGatewayImpl : UnleashGateway {
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
    override fun isEnabled(featureToggle: FeatureToggle, ident: String, typeBrev: TypeBrev): Boolean =
        unleash.isEnabled(
            featureToggle.key(),
            UnleashContext.builder()
                .userId(ident)
                .addProperty("typeBrev", typeBrev.name)
                .build()
        )

    override fun hentSakIdFilter(featureToggle: FeatureToggle): Set<Long> {
        val variant = unleash.getVariant(featureToggle.key())
        if (!variant.isEnabled) return setOf(0L)
        return variant.getPayload()
            .map { payload ->
                payload.value
                    ?.split(",")
                    ?.mapNotNull { it.trim().toLongOrNull() }
                    ?.toSet()
                    ?.takeIf { it.isNotEmpty() }
                    ?: setOf(0L)
            }
            .orElse(setOf(0L))
    }
}
