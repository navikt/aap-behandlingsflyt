package no.nav.aap.behandlingsflyt.integrasjon.unleash

import io.getunleash.DefaultUnleash
import io.getunleash.UnleashContext
import io.getunleash.util.UnleashConfig
import io.opentelemetry.instrumentation.annotations.WithSpan
import no.nav.aap.behandlingsflyt.behandling.brev.bestilling.TypeBrev
import no.nav.aap.behandlingsflyt.kontrakt.sak.Saksnummer
import no.nav.aap.behandlingsflyt.unleash.FeatureToggle
import no.nav.aap.behandlingsflyt.unleash.UnleashGateway
import no.nav.aap.komponenter.config.requiredConfigForKey
import no.nav.aap.komponenter.verdityper.Bruker
import kotlin.text.split

object UnleashGatewayImpl : UnleashGateway {
    private val unleash: DefaultUnleash? by lazy {
        runCatching {
            DefaultUnleash(
                UnleashConfig
                    .builder()
                    .appName(requiredConfigForKey("NAIS_APP_NAME"))
                    .unleashAPI("${requiredConfigForKey("UNLEASH_SERVER_API_URL")}/api")
                    .apiKey(requiredConfigForKey("UNLEASH_SERVER_API_TOKEN"))
                    .build()
            )
        }.getOrNull()
    }

    @WithSpan
    override fun isEnabled(featureToggle: FeatureToggle): Boolean = unleash?.isEnabled(featureToggle.key()) ?: false

    @WithSpan
    override fun isEnabled(featureToggle: FeatureToggle, ident: Bruker): Boolean =
        unleash?.isEnabled(featureToggle.key(), UnleashContext.builder().userId(ident.ident).build()) ?: false

    @WithSpan
    override fun isEnabled(featureToggle: FeatureToggle, ident: Bruker, typeBrev: TypeBrev): Boolean =
        unleash?.isEnabled(
            featureToggle.key(),
            UnleashContext.builder()
                .userId(ident.ident)
                .addProperty("typeBrev", typeBrev.name)
                .build()
        ) ?: false

    @WithSpan
    override fun getVariantValue(featureToggle: FeatureToggle, variantName: String): String {
        val variant = unleash?.getVariant(featureToggle.key()) ?: return ""
        return if (variant.isEnabled && variant.name == variantName) {
            variant.getPayload().map { it.value }.orElse("")
        } else ""
    }

    @WithSpan
    override fun isVariantEnabled(featureToggle: FeatureToggle, variantName: String): Boolean {
        val variant = unleash?.getVariant(featureToggle.key()) ?: return false
        return variant.isEnabled && variant.name == variantName
    }

    @WithSpan
    override fun erPåskruddForSak(featureToggle: FeatureToggle, variantName: String, saksnummer: Saksnummer): Boolean {
        return erPåskruddForSak(featureToggle, variantName) { saksnummer }
    }

    @WithSpan
    override fun erPåskruddForSak(
        featureToggle: FeatureToggle,
        variantName: String,
        saksnummerResolver: () -> Saksnummer
    ): Boolean {
        val verdi = getVariantValue(featureToggle, variantName)
        return when (verdi) {
            "" -> false
            else -> verdi.split(",")
                .map { it.trim() }
                .filter { it.isNotEmpty() }
                .map(Saksnummer::fra)
                .contains(saksnummerResolver())
        }
    }

}
