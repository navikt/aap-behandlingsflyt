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
            .appName(requiredConfigForKey("NAIS_APP_NAME"))
            .unleashAPI("${requiredConfigForKey("UNLEASH_SERVER_API_URL")}/api")
            .apiKey(requiredConfigForKey("UNLEASH_SERVER_API_TOKEN"))
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

    override fun getVariantValue(featureToggle: FeatureToggle, variantName: String): String {
        val variant = unleash.getVariant(featureToggle.key())
        return if (variant.isEnabled && variant.name == variantName) {
            variant.getPayload().map { it.value }.orElse("")
        } else ""
    }

    override fun isVariantEnabled(featureToggle: FeatureToggle, variantName: String): Boolean {
        val variant = unleash.getVariant(featureToggle.key())
        return variant.isEnabled && variant.name == variantName
    }

}
