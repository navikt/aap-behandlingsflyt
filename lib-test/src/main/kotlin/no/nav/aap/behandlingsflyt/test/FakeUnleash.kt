package no.nav.aap.behandlingsflyt.test

import no.nav.aap.behandlingsflyt.unleash.BehandlingsflytFeature
import no.nav.aap.behandlingsflyt.unleash.FeatureToggle
import no.nav.aap.behandlingsflyt.unleash.UnleashGateway

open class FakeUnleashBase(
    private val flags: Map<BehandlingsflytFeature, Boolean>,
) : UnleashGateway {
    override fun isEnabled(featureToggle: FeatureToggle) = requireNotNull(flags[featureToggle]) {
        "feature toggle $featureToggle ikke definert for fake"
    }

    override fun isEnabled(featureToggle: FeatureToggle, ident: String) = isEnabled(featureToggle)
}

object FakeUnleash : FakeUnleashBase(
    mapOf(
        BehandlingsflytFeature.IngenValidering to false,
        BehandlingsflytFeature.NyBrevtype11_17 to true,
        BehandlingsflytFeature.NyBrevtype11_18 to true,
        BehandlingsflytFeature.Aktivitetsplikt11_9 to true,
        BehandlingsflytFeature.OverforingsdatoNullForAvregning to true,
        BehandlingsflytFeature.OvergangArbeid to false,
        BehandlingsflytFeature.AvklaringsbehovServiceFormkrav to true,
        BehandlingsflytFeature.KvalitetssikringsSteg to true,
        BehandlingsflytFeature.FatteVedtakAvklaringsbehovService to true,
        BehandlingsflytFeature.NyBeregningAvklarFaktaSteg to true,
        BehandlingsflytFeature.EOSBeregning to true
    )
)

object LokalUnleash : FakeUnleashBase(
    mapOf(
        BehandlingsflytFeature.IngenValidering to true,
        BehandlingsflytFeature.NyBrevtype11_17 to true,
        BehandlingsflytFeature.NyBrevtype11_18 to true,
        BehandlingsflytFeature.Aktivitetsplikt11_9 to true,
        BehandlingsflytFeature.OverforingsdatoNullForAvregning to true,
        BehandlingsflytFeature.OvergangArbeid to true,
        BehandlingsflytFeature.AvklaringsbehovServiceFormkrav to true,
        BehandlingsflytFeature.KvalitetssikringsSteg to true,
        BehandlingsflytFeature.FatteVedtakAvklaringsbehovService to true,
        BehandlingsflytFeature.NyBeregningAvklarFaktaSteg to true,
    )
)